/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.data

import android.content.Context
import ch.admin.bag.covidcertificate.sdk.android.utils.EncryptedSharedPreferencesUtil
import ch.admin.bag.covidcertificate.sdk.android.utils.SingletonHolder
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.wallet.data.adapter.InstantJsonAdapter
import ch.admin.bag.covidcertificate.wallet.transfercode.model.TransferCodeModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import java.time.Instant

class WalletDataSecureStorage private constructor(context: Context) {

	companion object : SingletonHolder<WalletDataSecureStorage, Context>(::WalletDataSecureStorage) {
		private const val SHARED_PREFERENCES_NAME: String = "WalletDataSecureStorageName"
		private const val KEY_WALLET_DATA_ITEMS = "KEY_WALLET_DATA_ITEMS"

		private val factory = PolymorphicJsonAdapterFactory.of(WalletDataItem::class.java, "type")
			.withSubtype(WalletDataItem.CertificateWalletData::class.java, "certificate")
			.withSubtype(WalletDataItem.TransferCodeWalletData::class.java, "transferCode")
		private val moshi = Moshi.Builder().add(factory).add(InstantJsonAdapter()).build()
		private val walletDataItemAdapter =
			moshi.adapter<List<WalletDataItem>>(Types.newParameterizedType(List::class.java, WalletDataItem::class.java))
	}

	private val prefs = EncryptedSharedPreferencesUtil.initializeSharedPreferences(context, SHARED_PREFERENCES_NAME)

	fun saveWalletDataItem(dataItem: WalletDataItem) {
		val walletData = getWalletData().toMutableList()
		if ((dataItem is WalletDataItem.CertificateWalletData && walletData.containsCertificate(dataItem.qrCodeData)
					|| walletData.contains(dataItem))
		) {
			return
		}

		walletData.add(0, dataItem)
		updateWalletData(walletData)
	}

	fun containsCertificate(certificateQrCodeData: String): Boolean {
		return getWalletData().filterIsInstance<WalletDataItem.CertificateWalletData>()
			.map { it.qrCodeData }
			.contains(certificateQrCodeData)
	}

	fun updateTransferCodeLastUpdated(transferCode: TransferCodeModel): TransferCodeModel {
		val walletData = getWalletData().toMutableList()
		val index =
			walletData.indexOfFirst { it is WalletDataItem.TransferCodeWalletData && it.transferCode.code == transferCode.code }
		if (index >= 0) {
			val updatedTransferCode = transferCode.copy(lastUpdatedTimestamp = Instant.now())
			walletData.removeAt(index)
			walletData.add(index, WalletDataItem.TransferCodeWalletData(updatedTransferCode))
			updateWalletData(walletData)
			return updatedTransferCode
		}
		return transferCode
	}

	fun deleteCertificate(certificateQrCodeData: String) {
		val walletData = getWalletData().toMutableList()
		walletData.removeIf { it is WalletDataItem.CertificateWalletData && it.qrCodeData == certificateQrCodeData }
		updateWalletData(walletData)
	}

	fun deleteTransferCode(transferCode: TransferCodeModel) {
		val walletData = getWalletData().toMutableList()
		walletData.removeIf { it is WalletDataItem.TransferCodeWalletData && it.transferCode.code == transferCode.code }
		updateWalletData(walletData)
	}

	fun replaceTransferCodeWithCertificate(
		transferCode: TransferCodeModel,
		certificateQrCodeData: String,
		pdfData: String? = null
	) {
		val walletData = getWalletData().toMutableList()
		val index =
			walletData.indexOfFirst { it is WalletDataItem.TransferCodeWalletData && it.transferCode.code == transferCode.code }
		if (index >= 0) {
			walletData.removeAt(index)
			if (!walletData.containsCertificate(certificateQrCodeData)) {
				walletData.add(index, WalletDataItem.CertificateWalletData(certificateQrCodeData, pdfData))
			}
			updateWalletData(walletData)
		}
	}

	fun changeWalletDataItemPosition(oldPosition: Int, newPosition: Int) {
		val walletData = getWalletData().toMutableList()
		if (newPosition < 0 || oldPosition < 0) {
			return
		}

		val movedDataItem = walletData.removeAt(oldPosition)
		walletData.add(newPosition, movedDataItem)
		updateWalletData(walletData)
	}

	fun getWalletData(): List<WalletDataItem> {
		val json = prefs.getString(KEY_WALLET_DATA_ITEMS, null)
		if (json == null || json.isEmpty()) {
			return emptyList()
		}
		return walletDataItemAdapter.fromJson(json) ?: emptyList()
	}

	fun storeCertificateLight(fullCertificate: CertificateHolder, certificateLightData: String, certificateLightQrCode: String) {
		val walletData = getWalletData().toMutableList()
		val index = walletData.indexOfFirst {
			it is WalletDataItem.CertificateWalletData && it.qrCodeData == fullCertificate.qrCodeData
		}

		if (index >= 0) {
			val item = walletData.removeAt(index) as WalletDataItem.CertificateWalletData
			val updatedItem =
				item.copy(certificateLightData = certificateLightData, certificateLightQrCode = certificateLightQrCode)
			walletData.add(index, updatedItem)
			updateWalletData(walletData)
		}
	}

	fun deleteCertificateLight(qrCodeData: String) {
		val walletData = getWalletData().toMutableList()
		val index = walletData.indexOfFirst {
			it is WalletDataItem.CertificateWalletData && (it.qrCodeData == qrCodeData || it.certificateLightData == qrCodeData)
		}

		if (index >= 0) {
			val item = walletData.removeAt(index) as WalletDataItem.CertificateWalletData
			val updatedItem = item.copy(certificateLightData = null, certificateLightQrCode = null)
			walletData.add(index, updatedItem)
			updateWalletData(walletData)
		}
	}

	fun getPdfForCertificate(certificateHolder: CertificateHolder): String? {
		val walletData = getWalletData()
		return walletData.filterIsInstance(WalletDataItem.CertificateWalletData::class.java)
			.firstOrNull { it.qrCodeData == certificateHolder.qrCodeData }
			?.pdfData
	}

	fun storePdfForCertificate(certificateHolder: CertificateHolder, pdfData: String) {
		val walletData = getWalletData().toMutableList()
		val index = walletData.indexOfFirst {
			it is WalletDataItem.CertificateWalletData && (it.qrCodeData == certificateHolder.qrCodeData)
		}

		if (index >= 0) {
			val item = walletData.removeAt(index) as WalletDataItem.CertificateWalletData
			val updatedItem = item.copy(pdfData = pdfData)
			walletData.add(index, updatedItem)
			updateWalletData(walletData)
		}
	}

	private fun updateWalletData(walletData: List<WalletDataItem>) {
		val json = walletDataItemAdapter.toJson(walletData)
		prefs.edit().putString(KEY_WALLET_DATA_ITEMS, json).apply()
	}

	private fun List<WalletDataItem>.containsCertificate(qrCodeData: String) =
		this.filterIsInstance<WalletDataItem.CertificateWalletData>().find { it.qrCodeData == qrCodeData } != null

}