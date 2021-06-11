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
import ch.admin.bag.covidcertificate.eval.data.state.CheckNationalRulesState
import ch.admin.bag.covidcertificate.eval.data.state.VerificationState
import ch.admin.bag.covidcertificate.eval.models.CertType
import ch.admin.bag.covidcertificate.eval.models.DccHolder
import ch.admin.bag.covidcertificate.wallet.CertificatesViewModel
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.databinding.ItemCertificateListBinding
import ch.admin.bag.covidcertificate.wallet.util.getQrAlpha
import ch.admin.bag.covidcertificate.wallet.util.isOfflineMode

data class VerifiedCeritificateItem(val verifiedCertificate: CertificatesViewModel.VerifiedCertificate) {

	fun bindView(itemView: View, onCertificateClickListener: ((DccHolder) -> Unit)? = null) {
		val binding = ItemCertificateListBinding.bind(itemView)
		val state = verifiedCertificate.state
		val certificate = verifiedCertificate.dccHolder
		val certType = certificate.certType

		val name = "${certificate.euDGC.person.familyName} ${certificate.euDGC.person.givenName}"
		val qrAlpha = state.getQrAlpha()
		binding.itemCertificateListName.text = name
		binding.itemCertificateListName.alpha = qrAlpha
		binding.itemCertificateListIconQr.alpha = qrAlpha

		setCertificateType(binding.itemCertificateListType, state, certificate.certType)
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

				val statusIconId = when (state.nationalRulesState) {
					is CheckNationalRulesState.NOT_VALID_ANYMORE -> R.drawable.ic_invalid_grey
					is CheckNationalRulesState.NOT_YET_VALID -> R.drawable.ic_timelapse
					else -> R.drawable.ic_error_grey
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

		binding.root.setOnClickListener {
			onCertificateClickListener?.invoke(certificate)
		}
	}

	/**
	 * Set the text, text color and background of the certificate type, depending on the verification state and the certificate type
	 */
	private fun setCertificateType(
		view: TextView,
		state: VerificationState,
		certType: CertType?
	) {
		val backgroundColorId: Int
		val textColorId: Int
		when {
			state is VerificationState.INVALID -> {
				backgroundColorId = R.color.greyish
				textColorId = R.color.grey
			}
			certType == CertType.TEST -> {
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
			CertType.RECOVERY -> R.string.certificate_reason_recovered
			CertType.TEST -> R.string.certificate_reason_tested
			else -> R.string.certificate_reason_vaccinated
		}
		view.setText(typeLabelRes)
	}
}