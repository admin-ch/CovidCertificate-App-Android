/*
 * Copyright (c) 2022 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import ch.admin.bag.covidcertificate.common.R
import ch.admin.bag.covidcertificate.common.config.EolBannerInfoModel
import ch.admin.bag.covidcertificate.common.util.UrlUtil
import ch.admin.bag.covidcertificate.wallet.databinding.DialogFragmentCertificateBannerInfoBinding

class CertificateBannerInfoDialogFragment : DialogFragment() {

	companion object {
		private const val ARG_EOL_BANNER_INFO = "ARG_EOL_BANNER_INFO"

		fun newInstance(info: EolBannerInfoModel) = CertificateBannerInfoDialogFragment().apply {
			arguments = bundleOf(ARG_EOL_BANNER_INFO to info)
		}
	}

	private var _binding: DialogFragmentCertificateBannerInfoBinding? = null
	private val binding get() = _binding!!

	private lateinit var info: EolBannerInfoModel

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setStyle(STYLE_NO_TITLE, R.style.CovidCertificate_ChooseModeDialog)
		info = requireArguments().getSerializable(ARG_EOL_BANNER_INFO) as EolBannerInfoModel
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = DialogFragmentCertificateBannerInfoBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		binding.apply {
			title.text = info.popupTitle
			text1.isVisible = info.popupText1 != null
			text1.text = info.popupText1
			fatText.isVisible = info.popupBoldText != null
			fatText.text = info.popupBoldText
			text2.isVisible = info.popupText2 != null
			text2.text = info.popupText2
			learnMoreButton.text = info.popupLinkText

			learnMoreButton.setOnClickListener {
				UrlUtil.openUrl(requireContext(), info.popupLinkUrl)
			}

			dialogCloseButton.setOnClickListener {
				if (this@CertificateBannerInfoDialogFragment.isVisible) dismiss()
			}
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

}