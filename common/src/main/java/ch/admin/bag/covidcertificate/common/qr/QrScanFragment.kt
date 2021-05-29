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
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import ch.admin.bag.covidcertificate.common.R
import ch.admin.bag.covidcertificate.common.util.ErrorHelper
import ch.admin.bag.covidcertificate.common.util.ErrorState
import ch.admin.bag.covidcertificate.eval.DecodeState
import ch.admin.bag.covidcertificate.eval.Error
import ch.admin.bag.covidcertificate.eval.Eval
import ch.admin.bag.covidcertificate.eval.models.Bagdgc
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView


abstract class QrScanFragment : Fragment() {

	companion object {
		val TAG = QrScanFragment::class.java.canonicalName

		private const val PERMISSION_REQUEST_CAMERA = 13
		private const val MIN_ERROR_VISIBILITY = 1000L
	}

	// These need to be set by implementing classes during onCreateView. That's why they are not private.
	lateinit var toolbar: Toolbar
	lateinit var barcodeScanner: DecoratedBarcodeView
	lateinit var flashButton: ImageButton
	lateinit var errorView: View

	lateinit var invalidCodeText: TextView
	lateinit var viewFinderTopLeftIndicator: View
	lateinit var viewFinderTopRightIndicator: View
	lateinit var viewFinderBottomLeftIndicator: View
	lateinit var viewFinderBottomRightIndicator: View

	abstract val viewFinderColor: Int
	abstract val viewFinderErrorColor: Int
	abstract val torchOnDrawable: Int
	abstract val torchOffDrawable: Int

	private var lastUIErrorUpdate = 0L

	private val callback: BarcodeCallback = buildBarcodeCallback()

	private var cameraPermissionState = CameraPermissionState.REQUESTING
	private val IS_TORCH_ON = "IS_TORCH_ON"
	private var isTorchOn: Boolean = false

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		isTorchOn = savedInstanceState?.getBoolean(IS_TORCH_ON, isTorchOn) ?: isTorchOn
		toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }
		barcodeScanner.setStatusText("")
	}

	override fun onResume() {
		super.onResume()

		// Check permission in onResume to automatically handle the user returning from the system settings.
		// Be careful to avoid popup loops, since our fragment is resumed whenever the user returns from the dialog!
		checkCameraPermission()

		barcodeScanner.resume()

		setupFlashButtonStyle()
	}

	override fun onPause() {
		super.onPause()
		barcodeScanner.pause()
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putBoolean(IS_TORCH_ON, isTorchOn)
	}

	abstract fun onDecodeSuccess(dgc: Bagdgc)

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
		if (requestCode == PERMISSION_REQUEST_CAMERA) {
			val isGranted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED

			cameraPermissionState = if (isGranted) CameraPermissionState.GRANTED else CameraPermissionState.DENIED
			refreshView()
		}
	}

	private fun checkCameraPermission() {
		val isGranted =
			ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

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
				startCameraAndQrAnalyzer()
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
		indicateInvalidQrCode(QrScannerState.NO_CODE_FOUND)
	}

	private fun showCameraPermissionExplanationDialog() {
		CameraPermissionExplanationDialog(requireContext()).apply {
			setOnCancelListener {
				cameraPermissionState = CameraPermissionState.CANCELLED
				refreshView()
			}
			setGrantCameraAccessClickListener {
				requestPermissions(arrayOf(Manifest.permission.CAMERA), PERMISSION_REQUEST_CAMERA)
			}
			show()
		}
	}

	private fun startCameraAndQrAnalyzer() {
		barcodeScanner.apply {
			val formats: Collection<BarcodeFormat> = listOf(BarcodeFormat.AZTEC, BarcodeFormat.QR_CODE)
			barcodeView.decoderFactory = DefaultDecoderFactory(formats)
			decodeContinuous(callback)
			setStatusText("")
			viewFinder.visibility = View.GONE
		}
		val cameraSettings = barcodeScanner.cameraSettings
		cameraSettings.focusMode
		setupFlashButton()
	}

	private fun setupFlashButton() {
		barcodeScanner.setTorchListener(object : DecoratedBarcodeView.TorchListener {
			override fun onTorchOn() {
				this@QrScanFragment.isTorchOn = true
				setupFlashButtonStyle()
			}

			override fun onTorchOff() {
				this@QrScanFragment.isTorchOn = false
				setupFlashButtonStyle()
			}

		})

		flashButton.setOnClickListener {
			val isOn = flashButton.isSelected
			if (isOn) {
				barcodeScanner.setTorchOff()
			} else {
				barcodeScanner.setTorchOn()
			}
		}
	}

	private fun setupFlashButtonStyle() {
		if (isTorchOn) {
			flashButton.apply {
				isSelected = true
				setImageDrawable(ContextCompat.getDrawable(context, torchOnDrawable))
			}
		} else {
			flashButton.apply {
				isSelected = false
				setImageDrawable(ContextCompat.getDrawable(context, torchOffDrawable))
			}
		}
	}

	private fun handleInvalidQRCodeExceptions(qrCodeData: String?, error: Error?) {
		indicateInvalidQrCode(QrScannerState.INVALID_FORMAT)
		barcodeScanner.resume()
	}

	private fun indicateInvalidQrCode(qrScannerState: QrScannerState) {
		val currentTime = System.currentTimeMillis()
		if (lastUIErrorUpdate > currentTime - MIN_ERROR_VISIBILITY && qrScannerState == QrScannerState.NO_CODE_FOUND) {
			return
		}
		lastUIErrorUpdate = currentTime
		var color: Int = viewFinderColor
		when (qrScannerState) {
			QrScannerState.VALID, QrScannerState.NO_CODE_FOUND -> {
				invalidCodeText.visibility = View.INVISIBLE
			}
			QrScannerState.INVALID_FORMAT -> {
				invalidCodeText.visibility = View.VISIBLE
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

	private fun buildBarcodeCallback(): BarcodeCallback = object : BarcodeCallback {
		override fun barcodeResult(result: BarcodeResult) {
			val qrCodeData = result.text

			qrCodeData?.let {
				when (val decodeState = Eval.decode(qrCodeData)) {
					is DecodeState.SUCCESS -> {
						onDecodeSuccess(decodeState.dgc)
						view?.post { indicateInvalidQrCode(QrScannerState.VALID) }
					}
					is DecodeState.ERROR -> {
						view?.post { handleInvalidQRCodeExceptions(qrCodeData, decodeState.error) }
					}
				}
				barcodeScanner.pause()
			}
		}

		override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
	}

	enum class CameraPermissionState {
		GRANTED,
		REQUESTING,
		CANCELLED,
		DENIED,
	}
}