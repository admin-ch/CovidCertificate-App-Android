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

		fun getDescriptions() = listOf(
			R.string.verifier_homescreen_pager_description_1,
			R.string.verifier_homescreen_pager_description_2
		)
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

			root.post {
				// It seems that the viewpager has an issue with wrap_content and multiline text views. That's why we have to
				// request a layout on the description again after the full view is laid out, to ensure the whole text is visible.
				homescreenDescription.requestLayout()
			}
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun getImageAssets() = listOf(
		R.drawable.ic_illu_home_1,
		R.drawable.ic_illu_home_2
	)
}