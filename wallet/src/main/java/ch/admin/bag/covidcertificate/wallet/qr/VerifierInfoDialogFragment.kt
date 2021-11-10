/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.qr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import ch.admin.bag.covidcertificate.common.R
import ch.admin.bag.covidcertificate.common.databinding.DialogFragmentInfoBoxBinding
import ch.admin.bag.covidcertificate.common.util.UrlUtil

class VerifierInfoDialogFragment : DialogFragment() {

	companion object {
		fun newInstance() = VerifierInfoDialogFragment()
	}

	private var _binding: DialogFragmentInfoBoxBinding? = null
	private val binding get() = _binding!!

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setStyle(STYLE_NO_TITLE, R.style.CovidCertificate_InfoDialog)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		_binding = DialogFragmentInfoBoxBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		setupInfo()
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun setupInfo() {
		binding.infoDialogTitle.setText(ch.admin.bag.covidcertificate.wallet.R.string.wallet_info_box_certificate_scan_title)
		binding.infoDialogText.setText(ch.admin.bag.covidcertificate.wallet.R.string.wallet_info_box_certificate_scan_text)

		binding.infoDialogUrlButton.apply {
			setText(ch.admin.bag.covidcertificate.wallet.R.string.wallet_info_box_certificate_scan_button_check_app)
			isVisible = true
			setOnClickListener {
				UrlUtil.openUrl(context, "market://details?id=ch.admin.bag.covidcertificate.verifier")
				dismiss()
			}
		}
		binding.infoDialogCloseButton.apply {
			text = getString(ch.admin.bag.covidcertificate.wallet.R.string.wallet_info_box_certificate_scan_close)
			setOnClickListener { dismiss() }
		}
	}
}