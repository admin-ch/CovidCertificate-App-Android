/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.verifier.modes

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import ch.admin.bag.covidcertificate.common.config.CheckModeInfoEntry
import ch.admin.bag.covidcertificate.verifier.R
import ch.admin.bag.covidcertificate.verifier.databinding.DialogFragmentChooseModeBinding
import ch.admin.bag.covidcertificate.verifier.databinding.ItemModeButtonBinding
import ch.admin.bag.covidcertificate.verifier.databinding.ItemModeInfoBinding

class ChooseModeDialogFragment : DialogFragment() {

	companion object {
		fun newInstance(): ChooseModeDialogFragment = ChooseModeDialogFragment()
	}

	private var _binding: DialogFragmentChooseModeBinding? = null
	private val binding get() = _binding!!

	private val viewModel by activityViewModels<ModesAndConfigViewModel>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setStyle(STYLE_NO_TITLE, R.style.CovidCertificate_ChooseModeDialog)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		_binding = DialogFragmentChooseModeBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

		val observers = mutableListOf<Observer<ModesAndConfigViewModel.ModeState?>>()
		viewModel.modesLiveData.observe(viewLifecycleOwner) { modeState ->

			binding.modeItemsLayout.removeAllViews()
			observers.forEach { viewModel.modesLiveData.removeObserver(it) }
			observers.clear()

			for (mode in modeState.availableModes) {
				val itemView = ItemModeButtonBinding.inflate(layoutInflater, binding.infoItemsLayout, false)
				itemView.title.text = mode.title
				val observer = Observer<ModesAndConfigViewModel.ModeState?> { modeState ->
					if (modeState.selectedMode?.id == mode.id) {
						itemView.buttonIcon.setImageResource(R.drawable.ic_checkbox_filled)
						itemView.root.backgroundTintList = ColorStateList.valueOf(mode.hexColor.toColorInt())
					} else {
						itemView.buttonIcon.setImageResource(R.drawable.ic_checkbox_empty)
						itemView.root.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.grey_light, null))
					}
				}
				viewModel.modesLiveData.observe(viewLifecycleOwner, observer)
				observers.add(observer)

				itemView.root.setOnClickListener {
					viewModel.setSelectedMode(mode.id)
				}

				binding.modeItemsLayout.addView(itemView.root)

			}
		}

		viewModel.modesLiveData.observe(viewLifecycleOwner) { modeState ->

			binding.infoItemsLayout.removeAllViews()

			val mode = modeState.selectedMode
			val items: ArrayList<CheckModeInfoEntry> = arrayListOf()
			if (mode == null) {
				binding.chooseModeButton.isVisible = false
				val unselectedModeInfos = viewModel.configLiveData.value?.getUnselectedModesInfos(getString(R.string.language_key))
				unselectedModeInfos?.infos?.forEach { unselectedModeInfo ->
					items.add(CheckModeInfoEntry(unselectedModeInfo.iconAndroid, unselectedModeInfo.text))
				}
			} else {
				binding.chooseModeButton.isVisible = true
				items.addAll(mode.infos)
			}

			for (item in items) {
				val itemView = ItemModeInfoBinding.inflate(layoutInflater, binding.infoItemsLayout, false)

				val iconId = resources.getIdentifier(item.iconAndroid, "drawable", context?.packageName)
				if (iconId != 0) {
					itemView.icon.setImageResource(iconId)
				} else {
					itemView.icon.setImageResource(R.drawable.ic_info_outline)
				}
				itemView.infoText.text = item.text

				binding.infoItemsLayout.addView(itemView.root)
			}
		}

		binding.chooseModeButton.setOnClickListener {
			dismiss()
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

}