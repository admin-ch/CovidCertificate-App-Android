/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.onboarding

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import ch.admin.bag.covidcertificate.wallet.R

class OnboardingContentFragment : Fragment(R.layout.fragment_onboarding_content) {

	companion object {
		private const val ARG_RES_TITLE = "RES_TITLE"
		private const val ARG_RES_SUBTITLE = "RES_SUBTITLE"
		private const val ARG_RES_DESCRIPTION_1 = "RES_DESCRIPTION_1"
		private const val ARG_RES_DESCRIPTION_2 = "RES_DESCRIPTION_2"
		private const val ARG_RES_DESCR_ICON_1 = "ARG_RES_DESCR_ICON_1"
		private const val ARG_RES_DESCR_ICON_2 = "ARG_RES_DESCR_ICON_2"
		private const val ARG_RES_ILLUSTRATION = "RES_ILLUSTRATION"

		fun newInstance(
			@StringRes title: Int, @StringRes subtitle: Int,
			@DrawableRes illustration: Int, @StringRes description1: Int, @DrawableRes iconDescription1: Int,
			@StringRes description2: Int, @DrawableRes iconDescription2: Int
		): OnboardingContentFragment {
			val args = Bundle()
			args.putInt(ARG_RES_TITLE, title)
			args.putInt(ARG_RES_SUBTITLE, subtitle)
			args.putInt(ARG_RES_ILLUSTRATION, illustration)
			args.putInt(ARG_RES_DESCR_ICON_1, iconDescription1)
			args.putInt(ARG_RES_DESCRIPTION_1, description1)
			args.putInt(ARG_RES_DESCR_ICON_2, iconDescription2)
			args.putInt(ARG_RES_DESCRIPTION_2, description2)

			val fragment = OnboardingContentFragment()
			fragment.arguments = args
			return fragment
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val args = requireArguments()
		(view.findViewById<View>(R.id.onboarding_title) as TextView).setText(args.getInt(ARG_RES_TITLE))
		val subtitle = view.findViewById<TextView>(R.id.onboarding_subtitle)
		subtitle.setText(args.getInt(ARG_RES_SUBTITLE))
		(view.findViewById<View>(R.id.onboarding_illustration) as ImageView).setImageResource(args.getInt(ARG_RES_ILLUSTRATION))
		val icon1 = view.findViewById<ImageView>(R.id.onboarding_description_1_icon)
		icon1.setImageResource(args.getInt(ARG_RES_DESCR_ICON_1))
		(view.findViewById<View>(R.id.onboarding_description_1) as TextView).setText(args.getInt(ARG_RES_DESCRIPTION_1))
		val icon2 = view.findViewById<ImageView>(R.id.onboarding_description_2_icon)
		icon2.setImageResource(args.getInt(ARG_RES_DESCR_ICON_2))
		(view.findViewById<View>(R.id.onboarding_description_2) as TextView).setText(args.getInt(ARG_RES_DESCRIPTION_2))
		val continueButton = view.findViewById<Button>(R.id.onboarding_continue_button)
		continueButton.setOnClickListener { v: View? -> (activity as OnboardingActivity?)!!.continueToNextPage() }
	}
}