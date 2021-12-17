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
import ch.admin.bag.covidcertificate.sdk.core.models.state.ModeValidity
import ch.admin.bag.covidcertificate.sdk.core.models.state.ModeValidityState
import ch.admin.bag.covidcertificate.wallet.CertificatesAndConfigViewModel
import ch.admin.bag.covidcertificate.wallet.databinding.DialogFragmentModeInfoBinding
import ch.admin.bag.covidcertificate.wallet.databinding.ItemModeListInfoBinding
import ch.admin.bag.covidcertificate.wallet.util.BitmapUtil.getHumanReadableName
import ch.admin.bag.covidcertificate.wallet.util.BitmapUtil.textAsBitmap

class ModeInfoDialogFragment : DialogFragment() {

	companion object {

		private const val ARG_MODE_VALIDITIES = "ARG_MODE_VALIDITIES"

		fun newInstance(modeValidities: ArrayList<ModeValidity>): ModeInfoDialogFragment {
			val fragment = ModeInfoDialogFragment()
			val arguments = Bundle()
			arguments.putSerializable(ARG_MODE_VALIDITIES, modeValidities)
			fragment.arguments = arguments
			return fragment
		}
	}

	private val certificatesViewModel by activityViewModels<CertificatesAndConfigViewModel>()
	private lateinit var modeValidities: List<ModeValidity>
	private var _binding: DialogFragmentModeInfoBinding? = null
	private val binding get() = _binding!!

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setStyle(STYLE_NO_TITLE, R.style.CovidCertificate_InfoDialog);
		modeValidities = requireArguments().getSerializable(ARG_MODE_VALIDITIES) as ArrayList<ModeValidity>
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
		for (modeValidity in modeValidities) {
			val itemBinding = ItemModeListInfoBinding.inflate(layoutInflater, binding.modeInfoList, true)
			val imageView = itemBinding.modeListInfoImage
			val walletModeModel: WalletModeModel? = checkedModes?.get(modeValidity.mode)
			val resOk =
				requireContext().resources.getIdentifier(
					walletModeModel?.ok?.iconAndroid ?: "",
					"drawable",
					requireContext().packageName
				)
			val resNotOk =
				requireContext().resources.getIdentifier(
					walletModeModel?.notOk?.iconAndroid ?: "",
					"drawable",
					requireContext().packageName
				)
			if (modeValidity.modeValidityState == ModeValidityState.SUCCESS) {
				if (resOk != 0) {
					imageView.setImageResource(resOk)
					imageView.imageTintList =
						ColorStateList.valueOf(
							ContextCompat.getColor(
								requireContext(),
								ch.admin.bag.covidcertificate.wallet.R.color.blue
							)
						)
				} else {
					val bitmap =
						textAsBitmap(
							requireContext(),
							getHumanReadableName(modeValidity.mode),
							resources.getDimensionPixelSize(ch.admin.bag.covidcertificate.wallet.R.dimen.text_size_small),
							ContextCompat.getColor(requireContext(), ch.admin.bag.covidcertificate.wallet.R.color.blue),
							ContextCompat.getColor(requireContext(), ch.admin.bag.covidcertificate.wallet.R.color.white)
						)
					imageView.setImageBitmap(bitmap)
				}
				itemBinding.modeListInfoText.text = walletModeModel?.ok?.text
			} else if (modeValidity.modeValidityState == ModeValidityState.INVALID) {
				if (resNotOk != 0) {
					imageView.setImageResource(resNotOk)
					imageView.imageTintList =
						ColorStateList.valueOf(
							ContextCompat.getColor(
								requireContext(),
								ch.admin.bag.covidcertificate.wallet.R.color.grey
							)
						)
				} else {
					val bitmap =
						textAsBitmap(
							requireContext(),
							getHumanReadableName(modeValidity.mode),
							resources.getDimensionPixelSize(ch.admin.bag.covidcertificate.wallet.R.dimen.text_size_small),
							ContextCompat.getColor(requireContext(), ch.admin.bag.covidcertificate.wallet.R.color.blue),
							ContextCompat.getColor(requireContext(), ch.admin.bag.covidcertificate.wallet.R.color.white),
							isNotOK = true
						)
					imageView.setImageBitmap(bitmap)
				}
				itemBinding.modeListInfoText.text = walletModeModel?.notOk?.text
			}
		}

		binding.infoDialogCloseButton.setOnClickListener {
			if (this@ModeInfoDialogFragment.isVisible) dismiss()
		}
	}
}
