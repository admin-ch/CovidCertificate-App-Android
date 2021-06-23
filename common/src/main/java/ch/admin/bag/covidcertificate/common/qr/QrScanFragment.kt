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
import android.util.Size
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.appcompat.widget.Toolbar
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import ch.admin.bag.covidcertificate.common.R
import ch.admin.bag.covidcertificate.common.util.ErrorHelper
import ch.admin.bag.covidcertificate.common.util.ErrorState
import ch.admin.bag.covidcertificate.eval.data.state.DecodeState
import ch.admin.bag.covidcertificate.eval.data.state.Error
import ch.admin.bag.covidcertificate.eval.decoder.CertificateDecoder
import ch.admin.bag.covidcertificate.eval.models.DccHolder
import java.util.concurrent.Executor

abstract class QrScanFragment : Fragment() {

	companion object {
		private const val STATE_IS_TORCH_ON = "STATE_IS_TORCH_ON"
		private const val PERMISSION_REQUEST_CAMERA = 13
		private const val MIN_ERROR_VISIBILITY = 1000L
	}

	// These need to be set by implementing classes during onCreateView. That's why they are not private.
	lateinit var toolbar: Toolbar
	lateinit var flashButton: ImageButton
	lateinit var errorView: View

	lateinit var invalidCodeText: TextView
	lateinit var viewFinderTopLeftIndicator: View
	lateinit var viewFinderTopRightIndicator: View
	lateinit var viewFinderBottomLeftIndicator: View
	lateinit var viewFinderBottomRightIndicator: View
	lateinit var qrCodeScanner: PreviewView
	lateinit var cutOut: View
	private var preview: Preview? = null
	private var imageCapture: ImageCapture? = null
	private var imageAnalyzer: ImageAnalysis? = null
	private lateinit var mainExecutor: Executor
	private var camera: Camera? = null


	abstract val viewFinderColor: Int
	abstract val viewFinderErrorColor: Int
	abstract val torchOnDrawable: Int
	abstract val torchOffDrawable: Int

	private var lastUIErrorUpdate = 0L

	private var cameraPermissionState = CameraPermissionState.REQUESTING
	private var isTorchOn: Boolean = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		mainExecutor = ContextCompat.getMainExecutor(requireContext())
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		isTorchOn = savedInstanceState?.getBoolean(STATE_IS_TORCH_ON, isTorchOn) ?: isTorchOn
		toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }

		// Wait for the views to be properly laid out
		qrCodeScanner.post {
			bindCameraUseCases()
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

	abstract fun onDecodeSuccess(dccHolder: DccHolder)

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
		if (requestCode == PERMISSION_REQUEST_CAMERA) {
			val isGranted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED

			cameraPermissionState = if (isGranted) CameraPermissionState.GRANTED else CameraPermissionState.DENIED
			refreshView()
		}
	}

	private fun bindCameraUseCases() {
		val rotation = qrCodeScanner.display.rotation
		val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
		val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
		cameraProviderFuture.addListener({
			val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
			preview = Preview.Builder()
				.setTargetResolution(Size(720, 1280))
				.setTargetRotation(rotation)
				.build()

			preview?.setSurfaceProvider(qrCodeScanner.surfaceProvider)

			imageCapture = ImageCapture.Builder()
				.setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
				.setTargetResolution(Size(720, 1280))
				.setTargetRotation(rotation)
				.build()

			imageAnalyzer = ImageAnalysis.Builder()
				.setTargetResolution(Size(720, 1280))
				.setTargetRotation(rotation)
				.setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
				.build()
				.also { imageAnalysis ->
					imageAnalysis.setAnalyzer(mainExecutor, QRCodeAnalyzer { decodeCertificateState: DecodeCertificateState ->
						when (decodeCertificateState) {
							is DecodeCertificateState.ERROR -> {
								handleInvalidQRCodeExceptions(null, decodeCertificateState.error)
							}
							DecodeCertificateState.SCANNING -> {
								//do nothing
							}
							is DecodeCertificateState.SUCCESS -> {
								// Once successfully decoded, clear the analyzer from stopping more frames being analyzed and possibly decoded successfully
								imageAnalysis.clearAnalyzer()

								val qrCodeData = decodeCertificateState.qrCode
								qrCodeData?.let {
									when (val decodeState = CertificateDecoder.decode(it)) {
										is DecodeState.SUCCESS -> {
											onDecodeSuccess(decodeState.dccHolder)
											view?.post { indicateInvalidQrCode(QrScannerState.VALID) }
										}
										is DecodeState.ERROR -> {
											view?.post { handleInvalidQRCodeExceptions(qrCodeData, decodeState.error) }
										}
									}
								}
							}

						}
					})

				}

			cameraProvider.unbindAll()

			try {
				camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalyzer)
				// Set focus to the center of the viewfinder to help the auto focus
				val metricPointFactory = qrCodeScanner.meteringPointFactory
				val centerX = cutOut.left + cutOut.width / 2.0f
				val centerY = cutOut.top + cutOut.height / 2.0f
				val point = metricPointFactory.createPoint(centerX, centerY)
				val action = FocusMeteringAction.Builder(point).build()
				camera?.cameraControl?.startFocusAndMetering(action)
				setupFlashButton()
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}, mainExecutor)

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

	private fun setupFlashButton() {
		val camera = camera ?: return

		if (camera.cameraInfo.hasFlashUnit()) {
			flashButton.isVisible = true
			setFlashAndButtonStyle()
		} else {
			flashButton.isVisible = false
		}

		flashButton.setOnClickListener {
			isTorchOn = !flashButton.isSelected
			setFlashAndButtonStyle()
		}
	}

	private fun setFlashAndButtonStyle() {
		camera?.cameraControl?.enableTorch(isTorchOn)
		val drawableId = if (isTorchOn) torchOnDrawable else torchOffDrawable
		flashButton.isSelected = isTorchOn
		flashButton.setImageResource(drawableId)
	}

	private fun handleInvalidQRCodeExceptions(qrCodeData: String?, error: Error?) {
		//TODO Show error code on screen
		indicateInvalidQrCode(QrScannerState.INVALID_FORMAT)
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

	enum class CameraPermissionState {
		GRANTED,
		REQUESTING,
		CANCELLED,
		DENIED,
	}
}