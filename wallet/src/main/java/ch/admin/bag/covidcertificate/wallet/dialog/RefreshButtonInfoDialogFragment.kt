/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.dialog

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import ch.admin.bag.covidcertificate.common.R
import ch.admin.bag.covidcertificate.common.config.RefreshButtonInfoModel
import ch.admin.bag.covidcertificate.common.util.UrlUtil
import ch.admin.bag.covidcertificate.wallet.BuildConfig
import ch.admin.bag.covidcertificate.wallet.databinding.DialogFragmentRefreshButtonInfoBinding

class RefreshButtonInfoDialogFragment : DialogFragment() {

	companion object {
		private const val ARG_REFRESH_BUTTON_INFO = "ARG_REFRESH_BUTTON_INFO"

		fun newInstance(info: RefreshButtonInfoModel) = RefreshButtonInfoDialogFragment().apply {
			arguments = bundleOf(ARG_REFRESH_BUTTON_INFO to info)
		}
	}

	private var _binding: DialogFragmentRefreshButtonInfoBinding? = null
	private val binding get() = _binding!!

	private lateinit var info: RefreshButtonInfoModel

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setStyle(STYLE_NO_TITLE, R.style.CovidCertificate_ChooseModeDialog)
		info = requireArguments().getSerializable(ARG_REFRESH_BUTTON_INFO) as RefreshButtonInfoModel
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = DialogFragmentRefreshButtonInfoBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		binding.apply {
			title.text = info.title
			text1.text = info.text1
			text2.text = info.text2
			fatTitle.text = info.fatTitle
			text3.text = info.text3
			learnMoreButton.text = info.linkText

			covidCheckAppButton.setOnClickListener {
				val checkAppIntent = getCheckAppIntent()
				if (checkAppIntent != null) {
					startActivity(checkAppIntent)
				} else {
					UrlUtil.openUrl(requireContext(), getString(R.string.verifier_android_app_google_play_store_url))
				}
			}

			learnMoreButton.setOnClickListener {
				UrlUtil.openUrl(requireContext(), info.linkUrl)
			}

			dialogCloseButton.setOnClickListener {
				if (this@RefreshButtonInfoDialogFragment.isVisible) dismiss()
			}
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun getCheckAppIntent(): Intent? {
		var checkAppPackageName = "ch.admin.bag.covidcertificate.verifier"
		if (BuildConfig.FLAVOR == "dev" || BuildConfig.FLAVOR == "abn") {
			checkAppPackageName += ".${BuildConfig.FLAVOR}"
		}

		val packageManager = requireContext().packageManager
		val intent = packageManager.getLaunchIntentForPackage(checkAppPackageName) ?: return null
		val list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
		return if (list.isNotEmpty()) {
			return intent
		} else {
			null
		}
	}

}