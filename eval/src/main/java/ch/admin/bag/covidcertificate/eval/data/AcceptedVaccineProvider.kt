/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.data

import android.content.Context
import ch.admin.bag.covidcertificate.eval.products.AcceptedVaccine
import ch.admin.bag.covidcertificate.eval.products.Vaccine
import ch.admin.bag.covidcertificate.eval.products.ValueSet
import ch.admin.bag.covidcertificate.eval.utils.SingletonHolder
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import okio.buffer
import okio.source
import java.io.IOException

class AcceptedVaccineProvider private constructor(context: Context) {

	companion object : SingletonHolder<AcceptedVaccineProvider, Context>(::AcceptedVaccineProvider) {
		// for national rules validation
		private const val ACCEPTED_VACCINE_FILE_NAME = "acceptedCHVaccine.json"

		// for displaying products
		private const val ACCEPTED_VACCINE_MANUFACTURES_EU_FILE_NAME = "vaccine_mah_manf_eu.json"
		private const val ACCEPTED_VACCINE_PRODUCTS_EU_FILE_NAME = "vaccine_medicinal_product_eu.json"
		private const val ACCEPTED_VACCINE_PROPHYLAXIS_EU_FILE_NAME = "vaccine_prophylaxis.json"
	}

	private val acceptedVaccine: AcceptedVaccine
	private val vaccineManufacturersEu: ValueSet
	private val vaccineProductsEu: ValueSet
	private val vaccineProphylaxisEu: ValueSet

	init {
		val acceptedVaccineAdapter: JsonAdapter<AcceptedVaccine> = Moshi.Builder().build().adapter(AcceptedVaccine::class.java)
		acceptedVaccine = acceptedVaccineAdapter.fromJson(context.assets.open(ACCEPTED_VACCINE_FILE_NAME).source().buffer())
			?: throw IOException()

		val valueSetAdapter: JsonAdapter<ValueSet> = Moshi.Builder().build().adapter(ValueSet::class.java)
		vaccineManufacturersEu =
			valueSetAdapter.fromJson(context.assets.open(ACCEPTED_VACCINE_MANUFACTURES_EU_FILE_NAME).source().buffer())
				?: throw IOException()
		vaccineProductsEu = valueSetAdapter.fromJson(context.assets.open(ACCEPTED_VACCINE_PRODUCTS_EU_FILE_NAME).source().buffer())
			?: throw IOException()
		vaccineProphylaxisEu =
			valueSetAdapter.fromJson(context.assets.open(ACCEPTED_VACCINE_PROPHYLAXIS_EU_FILE_NAME).source().buffer())
				?: throw IOException()
	}

	fun getVaccineName(vaccinationEntry: VaccinationEntry): String {
		return vaccineProductsEu.valueSetValues[vaccinationEntry.mp]?.display ?: vaccinationEntry.mp
	}

	fun getProphylaxis(vaccinationEntry: VaccinationEntry): String {
		return vaccineProphylaxisEu.valueSetValues[vaccinationEntry.vp]?.display ?: vaccinationEntry.vp
	}

	fun getAuthHolder(vaccinationEntry: VaccinationEntry): String {
		return vaccineManufacturersEu.valueSetValues[vaccinationEntry.ma]?.display ?: vaccinationEntry.ma
	}

	fun getVaccineDataFromList(vaccinationEntry: VaccinationEntry): Vaccine? {
		return acceptedVaccine.entries.firstOrNull { entry -> entry.code == vaccinationEntry.mp }
	}

}