/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.common.qr

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.liveData
import ch.admin.bag.covidcertificate.common.R
import ch.admin.bag.covidcertificate.common.data.ConfigSecureStorage
import ch.admin.bag.covidcertificate.common.util.ErrorHelper
import ch.admin.bag.covidcertificate.common.util.ErrorState
import ch.admin.bag.covidcertificate.sdk.core.models.state.StateError
import ch.ubique.qrscanner.scanner.ErrorCodes
import ch.ubique.qrscanner.scanner.ScanningMode
import ch.ubique.qrscanner.state.DecodingState
import ch.ubique.qrscanner.util.CameraUtil
import ch.ubique.qrscanner.view.QrScannerView
import ch.ubique.qrscanner.zxing.decoder.GlobalHistogramImageDecoder
import ch.ubique.qrscanner.zxing.decoder.HybridImageDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

abstract class QrScanFragment : Fragment() {

	companion object {
		private const val STATE_IS_TORCH_ON = "STATE_IS_TORCH_ON"
		private const val PERMISSION_REQUEST_CAMERA = 13
		private const val MIN_ERROR_VISIBILITY = 1000L

		private const val QR_CODE_ERROR_WRONG_FORMAT = "Q|YWF"
		private const val QR_CODE_ERROR_READ_FAILED = "Q|IRF"
	}

	// These need to be set by implementing classes during onCreateView. That's why they are not private.
	protected lateinit var toolbar: Toolbar
	protected lateinit var flashButton: ImageButton
	protected lateinit var errorView: View
	protected lateinit var errorCodeView: TextView
	protected lateinit var zoomButton: ImageButton

	protected lateinit var invalidCodeText: TextView
	protected lateinit var viewFinderTopLeftIndicator: View
	protected lateinit var viewFinderTopRightIndicator: View
	protected lateinit var viewFinderBottomLeftIndicator: View
	protected lateinit var viewFinderBottomRightIndicator: View
	protected lateinit var qrCodeScanner: QrScannerView
	protected lateinit var cutOut: View
	abstract val viewFinderColor: Int

	abstract val viewFinderErrorColor: Int
	abstract val torchOnDrawable: Int
	abstract val torchOffDrawable: Int
	abstract val zoomOnDrawable: Int
	abstract val zoomOffDrawable: Int
	private var lastUIErrorUpdate = 0L

	private var cameraPermissionState = CameraPermissionState.REQUESTING
	private val secureStorage by lazy { ConfigSecureStorage.getInstance(requireContext()) }
	private var cameraPermissionExplanationDialog: CameraPermissionExplanationDialog? = null
	private var isTorchOn: Boolean = false

	private val autoFocusClockLiveData = liveData(Dispatchers.IO) {
		while (currentCoroutineContext().isActive) {
			emit(Unit)
			delay(3 * 1000L)
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		isTorchOn = savedInstanceState?.getBoolean(STATE_IS_TORCH_ON, isTorchOn) ?: isTorchOn
		toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }

		// Wait for the views to be properly laid out
		qrCodeScanner.post {
			initializeCamera()
		}
	}

	override fun onResume() {
		super.onResume()

		// Check permission in onResume to automatically handle the user returning from the system settings.
		// Be careful to avoid popup loops, since our fragment is resumed whenever the user returns from the dialog!
		checkCameraPermission()

		setFlashAndButtonStyle()
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putBoolean(STATE_IS_TORCH_ON, isTorchOn)
	}

	abstract fun decodeQrCodeData(qrCodeData: String, onDecodeSuccess: () -> Unit, onDecodeError: (StateError) -> Unit)

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
		if (requestCode == PERMISSION_REQUEST_CAMERA) {
			val isGranted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED

			cameraPermissionState = if (isGranted) CameraPermissionState.GRANTED else CameraPermissionState.DENIED
			refreshView()
		}
	}

	protected fun activateCamera() {
		if (!isAdded || !qrCodeScanner.isAttachedToWindow) return

		autoFocusClockLiveData.observe(viewLifecycleOwner) {
			autoFocus()
		}

		qrCodeScanner.activateCamera()
	}

	protected fun deactivateCamera() {
		if (!isAdded || !qrCodeScanner.isAttachedToWindow) return

		autoFocusClockLiveData.removeObservers(viewLifecycleOwner)

		qrCodeScanner.deactivateCamera()
	}

	@SuppressLint("ClickableViewAccessibility")
	private fun initializeCamera() {
		if (!isAdded || !qrCodeScanner.isAttachedToWindow) return

		setScannerCallback()
		qrCodeScanner.setImageDecoders(GlobalHistogramImageDecoder(), HybridImageDecoder())
		qrCodeScanner.setScanningMode(ScanningMode.PARALLEL)
		qrCodeScanner.setFocusOnTap(true)
		qrCodeScanner.setCameraStateCallback { isActive ->
			if (isActive) {
				qrCodeScanner.isVisible = true
				setupZoomButton()
				setupFlashButton()
			} else {
				qrCodeScanner.isVisible = false
			}
		}
	}

	protected fun setScannerCallback() {
		qrCodeScanner.setScannerCallback { state ->
			when (state) {
				is DecodingState.NotFound -> view?.post { updateQrCodeScannerState(QrScannerState.NO_CODE_FOUND) }
				is DecodingState.Decoded -> {
					val qrCodeData = state.content
					decodeQrCodeData(
						qrCodeData,
						onDecodeSuccess = {
							// Once successfully decoded, clear the analyzer from stopping more frames being
							// analyzed and possibly decoded successfully
							qrCodeScanner.setScannerCallback(null)

							view?.post { updateQrCodeScannerState(QrScannerState.VALID) }
						},
						onDecodeError = { error ->
							view?.post { handleInvalidQRCodeExceptions(error) }
						}
					)
				}
				is DecodingState.Error -> {
					val stateError = if (state.errorCode == ErrorCodes.INPUT_WRONG_FORMAT) {
						StateError(QR_CODE_ERROR_WRONG_FORMAT)
					} else {
						StateError(QR_CODE_ERROR_READ_FAILED)
					}
					handleInvalidQRCodeExceptions(stateError)
				}
			}
		}
	}

	private fun setZoom() {
		if (secureStorage.getZoomOn()) {
			qrCodeScanner.setLinearZoom(1f)
		} else {
			qrCodeScanner.setLinearZoom(0f)
		}
	}

	private fun autoFocus() {
		val centerX = cutOut.left + cutOut.width / 2.0f
		val centerY = cutOut.top + cutOut.height / 2.0f
		qrCodeScanner.startAutofocus(centerX, centerY)
	}

	private fun checkCameraPermission() {
		val isGranted = CameraUtil.hasCameraPermission(requireContext())

		if (isGranted) {
			cameraPermissionState = CameraPermissionState.GRANTED
		}
		// Do not request the permission again if the last time we tried the user denied it.
		// I.e. don't show the popup but the error view
		else if (cameraPermissionState != CameraPermissionState.DENIED) {
			cameraPermissionState = CameraPermissionState.REQUESTING
		}
		refreshView()
	}

	private fun refreshView() {
		when (cameraPermissionState) {
			CameraPermissionState.GRANTED -> {
				errorView.isVisible = false
			}
			CameraPermissionState.REQUESTING -> {
				errorView.isVisible = false
				showCameraPermissionExplanationDialog()
			}
			CameraPermissionState.CANCELLED, CameraPermissionState.DENIED -> {
				errorView.isVisible = true
				ErrorHelper.updateErrorView(errorView, ErrorState.CAMERA_ACCESS_DENIED, null, context)
			}
		}
		updateQrCodeScannerState(QrScannerState.NO_CODE_FOUND)
	}

	private fun showCameraPermissionExplanationDialog() {
		if (cameraPermissionExplanationDialog?.isShowing == true) {
			return
		}

		cameraPermissionExplanationDialog = CameraPermissionExplanationDialog(requireContext()).apply {
			setOnCancelListener {
				cameraPermissionState = CameraPermissionState.CANCELLED
				refreshView()
			}
			setGrantCameraAccessClickListener {
				requestPermissions(arrayOf(Manifest.permission.CAMERA), PERMISSION_REQUEST_CAMERA)
			}
			setOnDismissListener {
				cameraPermissionExplanationDialog = null
			}
			show()
		}
	}

	private fun setupFlashButton() {
		val cameraInfo = qrCodeScanner.getCameraInfo() ?: return

		if (cameraInfo.hasFlashUnit()) {
			flashButton.isVisible = true
			setFlashAndButtonStyle()
		} else {
			flashButton.isVisible = false
		}

		flashButton.setOnClickListener {
			isTorchOn = !flashButton.isSelected
			setFlashAndButtonStyle()
		}
		zoomButton.setOnClickListener {
			secureStorage.setZoomOn(!secureStorage.getZoomOn())
			setupZoomButton()
		}
	}

	private fun setFlashAndButtonStyle() {
		qrCodeScanner.setFlash(isTorchOn)
		val drawableId = if (isTorchOn) torchOnDrawable else torchOffDrawable
		flashButton.isSelected = isTorchOn
		flashButton.setImageResource(drawableId)
	}

	private fun setupZoomButton() {
		if (requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS)) {
			zoomButton.isVisible = false
		} else {
			val isZoomOn = secureStorage.getZoomOn()
			zoomButton.isVisible = true
			val drawableId = if (isZoomOn) zoomOnDrawable else zoomOffDrawable
			zoomButton.isSelected = isZoomOn
			zoomButton.setImageResource(drawableId)
			setZoom()
		}
	}

	private fun handleInvalidQRCodeExceptions(error: StateError?) {
		updateQrCodeScannerState(QrScannerState.INVALID_FORMAT, error?.code)
	}

	private fun updateQrCodeScannerState(qrScannerState: QrScannerState, errorCode: String? = null) {
		if (!isAdded) return

		val currentTime = System.currentTimeMillis()
		if (lastUIErrorUpdate > currentTime - MIN_ERROR_VISIBILITY && qrScannerState == QrScannerState.NO_CODE_FOUND) {
			return
		}

		lastUIErrorUpdate = currentTime
		var color: Int = viewFinderColor
		when (qrScannerState) {
			QrScannerState.VALID, QrScannerState.NO_CODE_FOUND -> {
				invalidCodeText.isVisible = false
				errorCodeView.isVisible = false
			}
			QrScannerState.INVALID_FORMAT -> {
				invalidCodeText.isVisible = true
				errorCodeView.isVisible = errorCode != null
				errorCodeView.text = errorCode
				color = viewFinderErrorColor
			}
		}

		setIndicatorColor(viewFinderTopLeftIndicator, color)
		setIndicatorColor(viewFinderTopRightIndicator, color)
		setIndicatorColor(viewFinderBottomLeftIndicator, color)
		setIndicatorColor(viewFinderBottomRightIndicator, color)
	}

	private fun setIndicatorColor(indicator: View, @ColorRes color: Int) {
		val drawable = indicator.background as LayerDrawable
		val stroke = drawable.findDrawableByLayerId(R.id.indicator) as GradientDrawable
		stroke.setStroke(
			resources.getDimensionPixelSize(R.dimen.qr_scanner_indicator_stroke_width),
			resources.getColor(color, null)
		)
	}

	enum class CameraPermissionState {
		GRANTED,
		REQUESTING,
		CANCELLED,
		DENIED,
	}
}