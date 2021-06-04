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
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import ch.admin.bag.covidcertificate.common.verification.VerificationState
import ch.admin.bag.covidcertificate.eval.models.DccHolder
import ch.admin.bag.covidcertificate.eval.models.CertType
import ch.admin.bag.covidcertificate.wallet.CertificatesViewModel
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.util.getQrAlpha
import ch.admin.bag.covidcertificate.wallet.util.getStatusIcon

data class VerifiedCeritificateItem(val verifiedCertificate: CertificatesViewModel.VerifiedCertificate) {

	fun bindView(itemView: View, onCertificateClickListener: ((DccHolder) -> Unit)? = null) {
		val context = itemView.context
		val certificate = verifiedCertificate.dccHolder
		val state = verifiedCertificate.state

		var typeBackgroundColor = R.color.blue
		var typeTextColor = R.color.white
		var typeLabelRes = R.string.certificate_reason_vaccinated
		when (certificate.certType) {
			CertType.RECOVERY -> {
				typeLabelRes = R.string.certificate_reason_recovered
			}
			CertType.TEST -> {
				typeLabelRes = R.string.certificate_reason_tested
				typeBackgroundColor = R.color.blueish
				typeTextColor = R.color.blue
			}
		}

		val isInvalid = state is VerificationState.INVALID
		if (isInvalid) {
			typeBackgroundColor = R.color.greyish
			typeTextColor = R.color.grey
		}

		val name = "${certificate.euDGC.nam.fn} ${certificate.euDGC.nam.gn}"
		val qrAlpha = state.getQrAlpha()
		itemView.findViewById<TextView>(R.id.item_certificate_list_name).apply {
			text = name
			alpha = qrAlpha
		}
		itemView.findViewById<View>(R.id.item_certificate_list_icon_qr).alpha = qrAlpha

		val isFinished = state != VerificationState.LOADING
		itemView.findViewById<View>(R.id.item_certificate_list_icon_loading_view).isVisible = !isFinished
		itemView.findViewById<ImageView>(R.id.item_certificate_list_icon_status).apply {
			isVisible = isFinished
			setImageResource(state.getStatusIcon())
		}
		itemView.findViewById<View>(R.id.item_certificate_list_icon_status_group).isVisible =
			state !is VerificationState.SUCCESS

		itemView.findViewById<TextView>(R.id.item_certificate_list_type).apply {
			backgroundTintList = context.resources.getColorStateList(typeBackgroundColor, context.theme)
			setTextColor(ContextCompat.getColor(context, typeTextColor))
			setText(typeLabelRes)
			isVisible = certificate.certType != null
		}

		itemView.setOnClickListener {
			onCertificateClickListener?.invoke(certificate)
		}

	}
}