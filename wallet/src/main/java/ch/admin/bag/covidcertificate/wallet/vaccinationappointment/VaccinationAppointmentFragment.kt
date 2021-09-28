/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.vaccinationappointment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import ch.admin.bag.covidcertificate.wallet.R

class VaccinationAppointmentFragment : Fragment(R.layout.fragment_vaccination_appointment) {

	companion object {
		fun newInstance() = VaccinationAppointmentFragment()
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
	}

}