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

import android.app.Dialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import ch.admin.bag.covidcertificate.common.R
import ch.admin.bag.covidcertificate.common.config.ConfigModel
import ch.admin.bag.covidcertificate.common.config.WalletModeModel
import ch.admin.bag.covidcertificate.wallet.CertificatesAndConfigViewModel
import ch.admin.bag.covidcertificate.wallet.databinding.DialogFragmentModeInfoBinding
import ch.admin.bag.covidcertificate.wallet.databinding.ItemModeListInfoBinding
import ch.admin.bag.covidcertificate.wallet.util.BitmapUtil.getHumanReadableName
import ch.admin.bag.covidcertificate.wallet.util.BitmapUtil.textAsBitmap

class ModeInfoDialogFragment : DialogFragment() {

	companion object {
		fun newInstance(): ModeInfoDialogFragment {
			return ModeInfoDialogFragment()
		}
	}

	private val certificatesViewModel by activityViewModels<CertificatesAndConfigViewModel>()

	private var _binding: DialogFragmentModeInfoBinding? = null
	private val binding get() = _binding!!

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setStyle(STYLE_NO_TITLE, R.style.CovidCertificate_InfoDialog);
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = DialogFragmentModeInfoBinding.inflate(inflater, container, false)
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
		val configLiveData: ConfigModel? = certificatesViewModel.configLiveData.value
		val checkedModes: Map<String, WalletModeModel>? = configLiveData?.getCheckModes(getString(R.string.language_key))
		binding.modeInfoTitle.text = configLiveData?.getInfoModeTitle(getString(R.string.language_key))
		binding.modeInfoList.removeAllViews()
		checkedModes?.forEach { mode ->
			val itemBinding = ItemModeListInfoBinding.inflate(layoutInflater, binding.modeInfoList, true)
			val imageView = itemBinding.modeListInfoImage
			val resOk =
				requireContext().resources.getIdentifier(
					mode.value.ok.iconAndroid ?: "",
					"drawable",
					requireContext().packageName
				)
			if (resOk != 0) {
				imageView.setImageResource(resOk)
			} else {
				val bitmap =
					textAsBitmap(
						requireContext(),
						getHumanReadableName(mode.key),
						resources.getDimensionPixelSize(ch.admin.bag.covidcertificate.wallet.R.dimen.text_size_small),
						ContextCompat.getColor(requireContext(), ch.admin.bag.covidcertificate.wallet.R.color.blue)
					)
				imageView.setImageBitmap(bitmap)
			}
			imageView.imageTintList =
				ColorStateList.valueOf(ContextCompat.getColor(requireContext(), ch.admin.bag.covidcertificate.wallet.R.color.blue))
			itemBinding.modeListInfoText.text = mode.value.ok.text
		}
		binding.infoDialogCloseButton.setOnClickListener {
			if (this@ModeInfoDialogFragment.isVisible) dismiss()
		}
	}
}
