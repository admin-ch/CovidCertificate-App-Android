/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.list

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import ch.admin.bag.covidcertificate.sdk.core.data.ErrorCodes
import ch.admin.bag.covidcertificate.sdk.core.extensions.isChAusnahmeTest
import ch.admin.bag.covidcertificate.sdk.core.extensions.isPositiveRatTest
import ch.admin.bag.covidcertificate.sdk.core.extensions.isSerologicalTest
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertType
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.DccCert
import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckNationalRulesState
import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckRevocationState
import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckSignatureState
import ch.admin.bag.covidcertificate.sdk.core.models.state.VerificationState
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.databinding.ItemCertificateListBinding
import ch.admin.bag.covidcertificate.wallet.databinding.ItemTransferCodeListBinding
import ch.admin.bag.covidcertificate.wallet.homescreen.pager.StatefulWalletItem
import ch.admin.bag.covidcertificate.wallet.transfercode.model.TransferCodeConversionState
import ch.admin.bag.covidcertificate.wallet.transfercode.model.TransferCodeModel
import ch.admin.bag.covidcertificate.wallet.transfercode.net.DeliveryRepository
import ch.admin.bag.covidcertificate.wallet.util.getQrAlpha
import ch.admin.bag.covidcertificate.wallet.util.isOfflineMode

sealed class WalletDataListItem {
	data class VerifiedCeritificateItem(
		val verifiedCertificate: StatefulWalletItem.VerifiedCertificate,
		val qrCodeImage: String?,
	) : WalletDataListItem() {

		fun bindView(itemView: View, onCertificateClickListener: ((Pair<CertificateHolder, String?>) -> Unit)? = null) {
			val binding = ItemCertificateListBinding.bind(itemView)
			val state = verifiedCertificate.state
			val certificate = verifiedCertificate.certificateHolder
			val certType = certificate?.certType

			val name = certificate?.certificate?.getPersonName()?.let { "${it.familyName} ${it.givenName}" }
			val qrAlpha = state.getQrAlpha()
			binding.itemCertificateListName.text = name
			binding.itemCertificateListName.alpha = qrAlpha
			binding.itemCertificateListIconQr.alpha = qrAlpha

			setCertificateType(binding.itemCertificateListType, certificate, state, certType)
			binding.itemCertificateListType.isVisible = certType != null

			when (state) {
				is VerificationState.LOADING -> {
					binding.itemCertificateListIconLoadingView.isVisible = true
					binding.itemCertificateListIconStatusGroup.isVisible = true
					binding.itemCertificateListIconStatus.isVisible = false
					binding.itemCertificateListIconStatus.setImageResource(0)
				}
				is VerificationState.SUCCESS -> {
					binding.itemCertificateListIconLoadingView.isVisible = false
					binding.itemCertificateListIconStatusGroup.isVisible = false
					binding.itemCertificateListIconStatus.isVisible = true
					binding.itemCertificateListIconStatus.setImageResource(R.drawable.ic_info_blue)
				}
				is VerificationState.INVALID -> {
					binding.itemCertificateListIconLoadingView.isVisible = false
					binding.itemCertificateListIconStatusGroup.isVisible = true
					binding.itemCertificateListIconStatus.isVisible = true

					val signatureState = state.signatureState
					val revocationState = state.revocationState
					val nationalRulesState = state.nationalRulesState
					val statusIconId: Int
					when {
						signatureState is CheckSignatureState.INVALID -> {
							statusIconId = R.drawable.ic_error_grey
						}
						revocationState is CheckRevocationState.INVALID -> {
							statusIconId = R.drawable.ic_error_grey
						}
						nationalRulesState is CheckNationalRulesState.NOT_VALID_ANYMORE -> {
							statusIconId = R.drawable.ic_invalid_grey
						}
						nationalRulesState is CheckNationalRulesState.NOT_YET_VALID -> {
							statusIconId = R.drawable.ic_timelapse
						}
						else -> {
							statusIconId = R.drawable.ic_error_grey
						}
					}
					binding.itemCertificateListIconStatus.setImageResource(statusIconId)
				}
				is VerificationState.ERROR -> {
					binding.itemCertificateListIconLoadingView.isVisible = false
					binding.itemCertificateListIconStatusGroup.isVisible = true
					binding.itemCertificateListIconStatus.isVisible = true

					val statusIconId = if (state.isOfflineMode()) R.drawable.ic_offline else R.drawable.ic_process_error_grey
					binding.itemCertificateListIconStatus.setImageResource(statusIconId)
				}
			}

			if (certificate != null) {
				binding.root.setOnClickListener {
					onCertificateClickListener?.invoke(certificate to qrCodeImage)
				}
				binding.root.isClickable = true
			} else {
				binding.root.setOnClickListener(null)
				binding.root.isClickable = false
			}
		}

		/**
		 * Set the text, text color and background of the certificate type, depending on the verification state and the certificate type
		 */
		private fun setCertificateType(
			view: TextView,
			certificate: CertificateHolder?,
			state: VerificationState,
			certType: CertType?,
		) {
			val isSerologicalTest = (certificate?.certificate as? DccCert)?.tests?.firstOrNull()?.isSerologicalTest() ?: false
			val isChAusnahmeTest = (certificate?.certificate as? DccCert)?.tests?.firstOrNull()?.isChAusnahmeTest() ?: false
			val isPositiveRatTest = (certificate?.certificate as? DccCert)?.tests?.firstOrNull()?.isPositiveRatTest() ?: false

			val backgroundColorId: Int
			val textColorId: Int
			when {
				state is VerificationState.INVALID -> {
					backgroundColorId = R.color.greyish
					textColorId = R.color.grey
				}
				certType == CertType.TEST -> {
					when {
						isSerologicalTest || isChAusnahmeTest || isPositiveRatTest -> {
							backgroundColorId = R.color.blue
							textColorId = R.color.white
						}
						else -> {
							backgroundColorId = R.color.blueish
							textColorId = R.color.blue
						}
					}
				}
				certType == CertType.LIGHT -> {
					backgroundColorId = R.color.blueish
					textColorId = R.color.blue
				}
				else -> {
					backgroundColorId = R.color.blue
					textColorId = R.color.white
				}
			}

			val context = view.context
			val colorStateList = context.resources.getColorStateList(backgroundColorId, context.theme)
			view.backgroundTintList = colorStateList
			view.setTextColor(ContextCompat.getColor(context, textColorId))

			val typeLabelRes: Int = when (certType) {
				CertType.LIGHT -> R.string.wallet_certificate_list_light_certificate_badge
				CertType.RECOVERY -> R.string.certificate_reason_recovered
				CertType.TEST -> {
					when {
						isSerologicalTest -> R.string.certificate_reason_recovered
						isChAusnahmeTest -> R.string.covid_certificate_ch_ausnahme_list_label
						isPositiveRatTest -> R.string.certificate_reason_recovered
						else -> R.string.certificate_reason_tested
					}
				}
				else -> R.string.certificate_reason_vaccinated
			}
			view.setText(typeLabelRes)
		}
	}

	data class TransferCodeItem(
		val conversionItem: StatefulWalletItem.TransferCodeConversionItem
	) : WalletDataListItem() {
		fun bindView(itemView: View, onTransferCodeClickListener: ((TransferCodeModel) -> Unit)? = null) {
			val binding = ItemTransferCodeListBinding.bind(itemView)

			when {
				conversionItem.transferCode.isFailed() -> {
					binding.itemTransferCodeListIcon.setImageResource(R.drawable.ic_transfer_code_list_failed)
					binding.itemTransferCodeListTitle.setText(R.string.wallet_transfer_code_state_expired)
					binding.itemTransferCodeListInfo.setText(R.string.wallet_transfer_code_old_code)
					binding.itemTransferCodeListInfo.setTextColor(ContextCompat.getColor(itemView.context, R.color.red))
					binding.itemTransferCodeListInfo.isVisible = true
					binding.itemTransferCodeListCode.isVisible = false
				}
				conversionItem.transferCode.isExpired() -> {
					binding.itemTransferCodeListIcon.setImageResource(R.drawable.ic_transfer_code_list_valid)
					binding.itemTransferCodeListTitle.setText(R.string.wallet_transfer_code_state_waiting)
					binding.itemTransferCodeListInfo.setText(R.string.wallet_transfer_code_old_code)
					binding.itemTransferCodeListInfo.setTextColor(ContextCompat.getColor(itemView.context, R.color.blue))
					binding.itemTransferCodeListInfo.isVisible = true
					binding.itemTransferCodeListCode.isVisible = false
				}
				conversionItem.conversionState is TransferCodeConversionState.ERROR
						&& conversionItem.conversionState.error.code != ErrorCodes.GENERAL_OFFLINE
						&& conversionItem.conversionState.error.code != DeliveryRepository.ERROR_CODE_INVALID_TIME -> {
					binding.itemTransferCodeListIcon.setImageResource(R.drawable.ic_scanner_alert)
					binding.itemTransferCodeListTitle.setText(R.string.wallet_transfer_code_state_expired)
					binding.itemTransferCodeListInfo.setText(R.string.wallet_transfer_code_unexpected_error_title)
					binding.itemTransferCodeListInfo.setTextColor(ContextCompat.getColor(itemView.context, R.color.red))
					binding.itemTransferCodeListInfo.isVisible = true
					binding.itemTransferCodeListCode.isVisible = false
				}
				else -> {
					binding.itemTransferCodeListIcon.setImageResource(R.drawable.ic_transfer_code_list_valid)
					binding.itemTransferCodeListTitle.setText(R.string.wallet_transfer_code_state_waiting)
					binding.itemTransferCodeListCode.text = formatTransferCode(conversionItem.transferCode.code)
					binding.itemTransferCodeListInfo.isVisible = false
					binding.itemTransferCodeListCode.isVisible = true
				}
			}

			binding.root.setOnClickListener {
				onTransferCodeClickListener?.invoke(conversionItem.transferCode)
			}
		}

		private fun formatTransferCode(code: String): String {
			return code.chunked(3).joinToString(" ")
		}
	}
}
