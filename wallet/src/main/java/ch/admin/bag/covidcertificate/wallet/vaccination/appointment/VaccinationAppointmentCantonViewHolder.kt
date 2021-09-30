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

import androidx.recyclerview.widget.RecyclerView
import ch.admin.bag.covidcertificate.common.config.VaccinationBookingCantonModel
import ch.admin.bag.covidcertificate.wallet.databinding.ItemVaccinationAppointmentCantonBinding

class VaccinationAppointmentCantonViewHolder(
	private val binding: ItemVaccinationAppointmentCantonBinding
) : RecyclerView.ViewHolder(binding.root) {

	fun bind(canton: VaccinationBookingCantonModel, onCantonClicked: (VaccinationBookingCantonModel) -> Unit) {
		val context = binding.root.context
		binding.root.setOnClickListener { onCantonClicked.invoke(canton) }
		binding.cantonIcon.setImageResource(context.resources.getIdentifier(canton.icon, "drawable", context.packageName))
		binding.cantonName.text = canton.name
	}

}