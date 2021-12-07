/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.vaccination.appointment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import ch.admin.bag.covidcertificate.common.config.ConfigModel
import ch.admin.bag.covidcertificate.common.config.ConfigViewModel
import ch.admin.bag.covidcertificate.common.util.UrlUtil
import ch.admin.bag.covidcertificate.wallet.BuildConfig
import ch.admin.bag.covidcertificate.wallet.CertificatesAndConfigViewModel
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentVaccinationAppointmentBinding

class VaccinationAppointmentFragment : Fragment() {

	companion object {
		fun newInstance() = VaccinationAppointmentFragment()
	}

	private var _binding: FragmentVaccinationAppointmentBinding? = null
	private val binding get() = _binding!!

	private val configViewModel by activityViewModels<CertificatesAndConfigViewModel>()

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentVaccinationAppointmentBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }
		setupMoreInformationButton()

		configViewModel.configLiveData.observe(viewLifecycleOwner) { config ->
			setupVaccinationBookingInfo(config)
		}
		configViewModel.loadConfig(BuildConfig.BASE_URL, BuildConfig.VERSION_NAME, BuildConfig.BUILD_TIME.toString())
	}

	private fun setupVaccinationBookingInfo(config: ConfigModel) {
		val languageKey = getString(R.string.language_key)
		val vaccinationBookingInfo = config.getVaccinationBookingInfo(languageKey)
		binding.vaccinationBookingTitle.text = vaccinationBookingInfo?.title
		binding.vaccinationBookingText.text = vaccinationBookingInfo?.text
		binding.vaccinationBookingInfo.text = vaccinationBookingInfo?.info

		if (vaccinationBookingInfo?.impfcheckTitle != null && vaccinationBookingInfo.impfcheckText != null && vaccinationBookingInfo.impfcheckButton != null && vaccinationBookingInfo.impfcheckUrl != null) {
			binding.impfcheckTitle.text = vaccinationBookingInfo.impfcheckTitle
			binding.impfcheckInfoText.text = vaccinationBookingInfo.impfcheckText
			binding.impfcheckAction.text = vaccinationBookingInfo.impfcheckButton
			binding.impfcheckAction.setOnClickListener {
				UrlUtil.openUrl(it.context, vaccinationBookingInfo.impfcheckUrl)
			}
		} else {
			binding.impfcheckTitle.visibility = View.GONE
			binding.impfcheckInfoText.visibility = View.GONE
			binding.impfcheckAction.visibility = View.GONE
		}
	}

	private fun setupMoreInformationButton() {
		binding.vaccinationMoreInfoButton.setOnClickListener {
			val url = getString(R.string.vaccination_booking_info_url)
			UrlUtil.openUrl(requireContext(), url)
		}
	}

}