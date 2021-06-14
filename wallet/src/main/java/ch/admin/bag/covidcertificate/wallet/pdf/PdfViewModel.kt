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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ch.admin.bag.covidcertificate.common.qr.QRCodeReaderHelper
import ch.admin.bag.covidcertificate.eval.data.state.DecodeState
import ch.admin.bag.covidcertificate.eval.decoder.CertificateDecoder
import java.io.File
import java.io.InputStream
import ch.admin.bag.covidcertificate.eval.data.state.Error

class PdfViewModel(application: Application) : AndroidViewModel(application) {

	private val pdfImportMutableLiveData: MutableLiveData<DecodeState> = MutableLiveData()
	val pdfImportLiveData: LiveData<DecodeState> = pdfImportMutableLiveData

	fun importPDFData(clipData: ClipData) {
		if (clipData.itemCount == 1 && clipData.getItemAt(0) != null && clipData.getItemAt(0).uri != null) {
			val uri = clipData.getItemAt(0).uri
			try {
				val inputStream: InputStream? = getApplication<Application>().contentResolver.openInputStream(uri)
				val outputFile: File = File.createTempFile("certificate", ".pdf", getApplication<Application>().cacheDir)
				inputStream?.copyTo(outputFile.outputStream())
				val bitmaps = QRCodeReaderHelper.pdfToBitmap(getApplication<Application>(), outputFile)
				if (!bitmaps.isNullOrEmpty()) {
					val decode = QRCodeReaderHelper.decodeQrCode(bitmaps[0])
					if (decode != null) {
						pdfImportMutableLiveData.postValue(CertificateDecoder.decode(decode))
					} else {
						pdfImportMutableLiveData.postValue(DecodeState.ERROR(Error("PIC1", "The PDF was not be imported")))
					}
				} else {
					pdfImportMutableLiveData.postValue(DecodeState.ERROR(Error("PIC1", "The PDF was not be imported")))
				}
			} catch (e: Exception) {
				pdfImportMutableLiveData.postValue(DecodeState.ERROR(Error("PIC1", "The PDF was not be imported")))
			}
		} else {
			pdfImportMutableLiveData.postValue(DecodeState.ERROR(Error("PIC1", "The PDF was not be imported")))
		}
	}

	fun cleanPDF() {
		pdfImportMutableLiveData.value = null
	}
}