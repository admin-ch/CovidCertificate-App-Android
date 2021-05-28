/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.common.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import ch.admin.bag.covidcertificate.common.R
import ch.admin.bag.covidcertificate.common.config.InfoBoxModel
import ch.admin.bag.covidcertificate.common.databinding.DialogFragmentInfoBoxBinding
import ch.admin.bag.covidcertificate.common.util.UrlUtil

class InfoDialogFragment : DialogFragment() {

	companion object {
		private const val ARG_INFO_BOX_MODEL = "ARG_INFO_BOX_MODEL"

		fun newInstance(infoBoxModel: InfoBoxModel): InfoDialogFragment = InfoDialogFragment().apply {
			arguments = bundleOf(ARG_INFO_BOX_MODEL to infoBoxModel)
		}
	}

	private lateinit var infoBoxModel: InfoBoxModel

	private var _binding: DialogFragmentInfoBoxBinding? = null
	private val binding get() = _binding!!

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setStyle(STYLE_NO_TITLE, R.style.CovidCertificate_InfoDialog);
		infoBoxModel = requireArguments().getParcelable(ARG_INFO_BOX_MODEL)
			?: throw IllegalStateException("No infoBox information supplied to DialogFragment!")
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
		binding.infoDialogTitle.text = infoBoxModel.title
		binding.infoDialogText.text = infoBoxModel.title
		val hasCustomButton = !infoBoxModel.urlTitle.isNullOrEmpty()
		binding.infoDialogUrlButton.apply {
			text = infoBoxModel.urlTitle
			isVisible = hasCustomButton
			infoBoxModel.url?.let { url ->
				setOnClickListener { UrlUtil.openUrl(context, url) }
			}
		}
		binding.infoDialogCloseButton.apply {
			text = getString(if (hasCustomButton) R.string.accessibility_close_button else R.string.ok_button)
			setOnClickListener { if (this@InfoDialogFragment.isVisible) dismiss() }
		}
	}

	fun getInfoId(): Long = infoBoxModel.infoId
}