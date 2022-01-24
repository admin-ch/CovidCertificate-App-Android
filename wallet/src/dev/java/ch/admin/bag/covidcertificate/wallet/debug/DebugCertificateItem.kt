/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.debug

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertType
import ch.admin.bag.covidcertificate.sdk.core.models.state.VerificationState
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.homescreen.pager.StatefulWalletItem
import ch.admin.bag.covidcertificate.wallet.util.getQrAlpha

data class DebugCertificateItem(val verifiedCertificate: StatefulWalletItem.VerifiedCertificate) {

	fun bindView(
		itemView: View,
		onShareClickListener: ((String) -> Unit),
	) {
		val context = itemView.context
		val certificate = verifiedCertificate.certificateHolder
		val state = verifiedCertificate.state
		val certType = certificate?.certType

		var typeBackgroundColor = R.color.blue
		var typeTextColor = R.color.white
		var typeLabelRes: Int? = null
		when (certType) {
			CertType.RECOVERY -> {
				typeLabelRes = R.string.certificate_reason_recovered
			}
			CertType.TEST -> {
				typeLabelRes = R.string.certificate_reason_tested
				typeBackgroundColor = R.color.blueish
				typeTextColor = R.color.blue
			}
			CertType.VACCINATION -> {
				typeLabelRes = R.string.certificate_reason_vaccinated
			}
			else -> {}
		}

		val isInvalid = state is VerificationState.INVALID
		if (isInvalid) {
			typeBackgroundColor = R.color.greyish
			typeTextColor = R.color.grey
		}

		// Name
		val name = certificate?.certificate?.getPersonName()?.prettyName() ?: verifiedCertificate.qrCodeData
		val qrAlpha = state.getQrAlpha()
		itemView.findViewById<TextView>(R.id.item_certificate_list_name).apply {
			text = name
			alpha = qrAlpha
		}

		// Type
		itemView.findViewById<TextView>(R.id.item_certificate_list_type).apply {
			backgroundTintList = context.resources.getColorStateList(typeBackgroundColor, context.theme)
			setTextColor(ContextCompat.getColor(context, typeTextColor))
			typeLabelRes?.let { setText(it) }
			isVisible = certType != null
		}

		// Buttons
		itemView.findViewById<Button>(R.id.button_share_string).setOnClickListener {
			onShareClickListener.invoke(verifiedCertificate.qrCodeData)
		}

	}
}