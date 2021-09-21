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
import android.util.Size
import android.view.MotionEvent
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
import ch.admin.bag.covidcertificate.common.aws.AWSRepository
import ch.admin.bag.covidcertificate.common.util.ErrorHelper
import ch.admin.bag.covidcertificate.common.util.ErrorState
import ch.admin.bag.covidcertificate.sdk.core.models.state.StateError
import com.google.gson.Gson
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger

abstract class QrScanFragment : Fragment() {

	companion object {
		private const val STATE_IS_TORCH_ON = "STATE_IS_TORCH_ON"
		private const val PERMISSION_REQUEST_CAMERA = 13
		private const val MIN_ERROR_VISIBILITY = 1000L
	}

	// These need to be set by implementing classes during onCreateView. That's why they are not private.
	protected lateinit var toolbar: Toolbar
	protected lateinit var flashButton: ImageButton
	protected lateinit var uploadButton: ImageButton
	protected lateinit var errorView: View
	protected lateinit var errorCodeView: TextView

	protected lateinit var invalidCodeText: TextView
	protected lateinit var viewFinderTopLeftIndicator: View
	protected lateinit var viewFinderTopRightIndicator: View
	protected lateinit var viewFinderBottomLeftIndicator: View
	protected lateinit var viewFinderBottomRightIndicator: View
	protected lateinit var qrCodeScanner: PreviewView
	protected lateinit var cutOut: View
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
	private var cameraPermissionExplanationDialog: CameraPermissionExplanationDialog? = null
	private var isTorchOn: Boolean = false
	protected lateinit var repository: AWSRepository
	private val frameCount: AtomicInteger = AtomicInteger(5)
	val options = BarcodeScannerOptions.Builder()
		.setBarcodeFormats(
			Barcode.FORMAT_QR_CODE,
		)
		.build()

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

		uploadButton.setOnClickListener {
			frameCount.set(0)
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

	@SuppressLint("ClickableViewAccessibility")
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
					imageAnalysis.setAnalyzer(
						mainExecutor,
						QRCodeAnalyzer() { decodeCertificateState: DecodeCertificateState ->
							when (decodeCertificateState) {
								is DecodeCertificateState.ERROR -> {
									handleInvalidQRCodeExceptions(decodeCertificateState.error)
								}
								is DecodeCertificateState.SCANNING -> {
									view?.post { updateQrCodeScannerState(QrScannerState.NO_CODE_FOUND) }
									decodeCertificateState.simpleImage.let { simpleImage ->
										val simpleImageAsJson = Gson().toJson(simpleImage)
										upload(simpleImageAsJson, false)
									}
								}
								is DecodeCertificateState.SUCCESS -> {
									val qrCodeData = decodeCertificateState.qrCode
									qrCodeData?.let {
										decodeQrCodeData(
											it,
											onDecodeSuccess = {
												// Once successfully decoded, clear the analyzer from stopping more frames being
												// analyzed and possibly decoded successfully
												imageAnalysis.clearAnalyzer()

												view?.post { updateQrCodeScannerState(QrScannerState.VALID) }
											},
											onDecodeError = { error ->
												view?.post { handleInvalidQRCodeExceptions(error) }
											}
										)
									}
									CoroutineScope(Dispatchers.Main).launch {
										try {
											decodeCertificateState.simpleImage.let { simpleImage ->
												val simpleImageAsJson = Gson().toJson(simpleImage)
												repository.upload(
													simpleImageAsJson,
													true
												)
											}
										} catch (e: java.lang.Exception) {
											e.printStackTrace()
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

		cutOut.setOnTouchListener { _: View, motionEvent: MotionEvent ->
			when (motionEvent.action) {
				MotionEvent.ACTION_DOWN -> true
				MotionEvent.ACTION_UP -> {
					// Create a MeteringPoint from the tap coordinates
					val factory = qrCodeScanner.meteringPointFactory
					val point = factory.createPoint(motionEvent.x, motionEvent.y)

					// Create a MeteringAction from the MeteringPoint
					val action = FocusMeteringAction.Builder(point).build()

					// Trigger the focus and metering as a fire-and-forget, ignoring the ListenableFuture return value
					camera?.cameraControl?.startFocusAndMetering(action)
					true
				}
				else -> false
			}
		}
	}

	private fun upload(bitmap: String, success: Boolean) {
		while (frameCount.getAndIncrement() < 5) {
			CoroutineScope(Dispatchers.Main).launch {
				try {
					repository.upload(bitmap, success)
				} catch (e: java.lang.Exception) {
					e.printStackTrace()
				}
			}
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