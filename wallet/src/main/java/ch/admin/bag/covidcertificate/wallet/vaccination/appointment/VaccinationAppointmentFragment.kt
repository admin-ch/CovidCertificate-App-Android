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
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import ch.admin.bag.covidcertificate.common.config.ConfigModel
import ch.admin.bag.covidcertificate.common.config.VaccinationBookingCantonModel
import ch.admin.bag.covidcertificate.common.net.ConfigRepository
import ch.admin.bag.covidcertificate.common.util.UrlUtil
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentVaccinationAppointmentBinding
import ch.admin.bag.covidcertificate.wallet.getInstanceWallet

class VaccinationAppointmentFragment : Fragment() {

	companion object {
		fun newInstance() = VaccinationAppointmentFragment()
	}

	private var _binding: FragmentVaccinationAppointmentBinding? = null
	private val binding get() = _binding!!

	private lateinit var configRepository: ConfigRepository
	private val adapter = VaccinationAppointmentCantonAdapter(this::onCantonClicked)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		configRepository = ConfigRepository.getInstanceWallet(requireContext())
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentVaccinationAppointmentBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }
		setupCantonList()
		setupMoreInformationButton()

		configRepository.configLiveData.observe(viewLifecycleOwner) { config ->
			setupVaccinationBookingInfo(config)
			adapter.setItems(config.getVaccinationBookingCantons(getString(R.string.language_key)) ?: emptyList())
		}
		configRepository.loadConfig()
	}

	private fun setupCantonList() {
		binding.vaccinationAppointmentCantonList.adapter = adapter
		binding.vaccinationAppointmentCantonList.addItemDecoration(DividerItemDecoration(requireContext(), LinearLayout.HORIZONTAL))
	}

	private fun setupVaccinationBookingInfo(config: ConfigModel) {
		val languageKey = getString(R.string.language_key)
		binding.vaccinationBookingTitle.text = config.getVaccinationBookingInfo(languageKey)?.title
		binding.vaccinationBookingText.text = config.getVaccinationBookingInfo(languageKey)?.text
		binding.vaccinationBookingInfo.text = config.getVaccinationBookingInfo(languageKey)?.info
	}

	private fun setupMoreInformationButton() {
		binding.vaccinationMoreInfoButton.setOnClickListener {
			val url = getString(R.string.vaccination_booking_info_url)
			UrlUtil.openUrl(requireContext(), url)
		}
	}

	private fun onCantonClicked(canton: VaccinationBookingCantonModel) {
		UrlUtil.openUrl(requireContext(), canton.linkUrl)
	}

}