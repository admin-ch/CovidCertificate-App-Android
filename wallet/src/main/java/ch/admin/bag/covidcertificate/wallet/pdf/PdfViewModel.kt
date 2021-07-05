/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.pdf

import android.app.Application
import android.content.ClipData
import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ch.admin.bag.covidcertificate.common.qr.QRCodeReaderHelper
import ch.admin.bag.covidcertificate.sdk.android.CovidCertificateSdk
import ch.admin.bag.covidcertificate.sdk.android.utils.NetworkUtil
import ch.admin.bag.covidcertificate.sdk.core.data.ErrorCodes
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.sdk.core.models.state.DecodeState
import ch.admin.bag.covidcertificate.sdk.core.models.state.StateError
import ch.admin.bag.covidcertificate.wallet.BuildConfig
import ch.admin.bag.covidcertificate.wallet.MainApplication
import ch.admin.bag.covidcertificate.wallet.data.WalletDataSecureStorage
import ch.admin.bag.covidcertificate.wallet.pdf.export.PdfExportState
import ch.admin.bag.covidcertificate.wallet.pdf.net.PdfExportRepository
import ch.admin.bag.covidcertificate.wallet.pdf.net.PdfExportSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.io.InputStream

class PdfViewModel(application: Application) : AndroidViewModel(application) {

	private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
	private val walletDataStorage = WalletDataSecureStorage.getInstance(application.applicationContext)
	private val pdfExportRepository =
		PdfExportRepository.getInstance(PdfExportSpec(application.applicationContext, BuildConfig.BASE_URL_TRANSFORMATION))

	private val pdfImportMutableLiveData: MutableLiveData<PdfImportState> = MutableLiveData()
	val pdfImportState: LiveData<PdfImportState> = pdfImportMutableLiveData

	private val pdfExportMutableLiveData: MutableLiveData<PdfExportState> = MutableLiveData()
	val pdfExportState: LiveData<PdfExportState> = pdfExportMutableLiveData

	private var exportJob: Job? = null

	fun importPdf(clipData: ClipData) {
		if (clipData.itemCount != 1 || clipData.getItemAt(0) == null || clipData.getItemAt(0).uri == null) {
			pdfImportMutableLiveData.postValue(
				PdfImportState.DONE(DecodeState.ERROR(StateError(PdfErrorCodes.FAILED_TO_READ, "The PDF was not be imported")))
			)
			return
		}
		val uri = clipData.getItemAt(0).uri
		importPdf(uri)
	}

	fun importPdf(uri: Uri) {
		pdfImportMutableLiveData.value = PdfImportState.LOADING
		viewModelScope.launch(Dispatchers.IO) {
			try {
				val inputStream: InputStream? = getApplication<Application>().contentResolver.openInputStream(uri)
				val outputFile: File = File.createTempFile("certificate", ".pdf", getApplication<Application>().cacheDir)
				inputStream?.copyTo(outputFile.outputStream())

				val bitmaps = QRCodeReaderHelper.pdfToBitmap(getApplication<Application>(), outputFile)

				for (bitmap in bitmaps) {
					val decode = QRCodeReaderHelper.decodeQrCode(bitmap)
					if (decode != null) {
						pdfImportMutableLiveData.postValue(PdfImportState.DONE(CovidCertificateSdk.Wallet.decode(decode)))
						// Stop as soon as we found the first QR code in the PDF
						return@launch
					}
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}

			pdfImportMutableLiveData.postValue(
				PdfImportState.DONE(DecodeState.ERROR(StateError(PdfErrorCodes.NO_QR_CODE_FOUND, "The PDF was not be imported")))
			)
		}
	}

	fun exportPdf(certificateHolder: CertificateHolder) {
		exportJob?.cancel()

		pdfExportMutableLiveData.value = PdfExportState.LOADING
		exportJob = viewModelScope.launch(Dispatchers.IO) {
			val pdfData = walletDataStorage.getPdfForCertificate(certificateHolder)

			if (pdfData != null) {
				// Directly post the pdf data if it was already stored once
				pdfExportMutableLiveData.postValue(PdfExportState.SUCCESS(pdfData))
			} else {
				try {
					// Convert the certificate to a pdf by calling the backend and storing the response
					val response = pdfExportRepository.convert(certificateHolder)

					if (response != null) {
						walletDataStorage.storePdfForCertificate(certificateHolder, response.pdf)
						pdfExportMutableLiveData.postValue(PdfExportState.SUCCESS(response.pdf))
					} else {
						val error = checkIfOffline()
						pdfExportMutableLiveData.postValue(PdfExportState.ERROR(error))
					}
				} catch (e: IOException) {
					val error = checkIfOffline()
					pdfExportMutableLiveData.postValue(PdfExportState.ERROR(error))
				}
			}
		}
	}

	fun clearPdfImport() {
		pdfImportMutableLiveData.value = null
	}

	private fun checkIfOffline(): StateError {
		return if (NetworkUtil.isNetworkAvailable(connectivityManager)) {
			StateError(ErrorCodes.GENERAL_NETWORK_FAILURE)
		} else {
			StateError(ErrorCodes.GENERAL_OFFLINE)
		}
	}
}

sealed class PdfImportState {
	data class DONE(val decodeState: DecodeState) : PdfImportState()
	object LOADING : PdfImportState()
}

object PdfErrorCodes {
	const val FAILED_TO_READ = "PDF|FTR"
	const val NO_QR_CODE_FOUND = "PDF|NCF"
}