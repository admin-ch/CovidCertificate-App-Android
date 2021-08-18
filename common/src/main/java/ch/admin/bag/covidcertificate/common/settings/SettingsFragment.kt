/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.common.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ch.admin.bag.covidcertificate.common.R
import ch.admin.bag.covidcertificate.common.data.ConfigSecureStorage
import ch.admin.bag.covidcertificate.common.databinding.FragmentSettingsBinding
import ch.admin.bag.covidcertificate.common.databinding.ItemLanguageOptionBinding
import ch.admin.bag.covidcertificate.common.util.LocaleUtil.DEFAULT_COUNTRY
import java.util.*

class SettingsFragment : Fragment() {

	companion object {
		fun newInstance(): SettingsFragment {
			return SettingsFragment()
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		return FragmentSettingsBinding.inflate(inflater, container, false).apply {
			settingsToolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }

			val currentLanguage = getString(R.string.language_key)

			listOf("de", "fr", "it", "rm", "en").forEach { language ->
				ItemLanguageOptionBinding.inflate(inflater, languageList, true).apply {
					val locale = Locale(language, DEFAULT_COUNTRY)
					radiobutton.text = locale.getDisplayLanguage(locale).capitalize(locale)
					radiobutton.isChecked = locale.language == currentLanguage

					radiobutton.setOnCheckedChangeListener { _, isChecked ->
						if (isChecked) {
							updateLanguage(language)
						}
					}
				}
			}
		}.root
	}

	private fun updateLanguage(language: String) {
		ConfigSecureStorage.getInstance(requireContext()).setUserLanguage(language)
		requireActivity().recreate()
	}

}