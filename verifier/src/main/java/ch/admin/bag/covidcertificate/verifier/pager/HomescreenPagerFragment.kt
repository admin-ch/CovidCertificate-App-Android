/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.verifier.pager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import ch.admin.bag.covidcertificate.verifier.R
import ch.admin.bag.covidcertificate.verifier.databinding.FragmentHomeScreenPagerBinding

class HomescreenPagerFragment : Fragment() {

	companion object {
		private const val ARG_POSITION = "ARG_POSITION"

		fun getInstance(position: Int) = HomescreenPagerFragment().apply {
			arguments = bundleOf(ARG_POSITION to position)
		}

		fun getDescriptions(): List<Int> {
			val strings = mutableListOf<Int>()
			strings.add(R.string.verifier_homescreen_pager_description_1)
			strings.add(R.string.verifier_homescreen_pager_description_2)
			return strings
		}
	}

	private var _binding: FragmentHomeScreenPagerBinding? = null
	private val binding get() = _binding!!

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentHomeScreenPagerBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		val position = requireArguments().getInt(ARG_POSITION)
		val homescreenPagerDescriptions: List<Int> = getDescriptions()
		val homescreenImages = getImageAssets()
		with(binding) {
			homescreenImage.setImageResource(homescreenImages[position])
			homescreenDescription.setText(homescreenPagerDescriptions[position])
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun getImageAssets(): List<Int> {
		val images = mutableListOf<Int>()
		images.add(R.drawable.ic_illu_home_1)
		images.add(R.drawable.ic_illu_home_2)
		return images
	}
}