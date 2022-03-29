/*
 * Copyright (c) 2022 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.travel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.admin.bag.covidcertificate.sdk.android.CovidCertificateSdk
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ForeignValidityViewModel : ViewModel() {

	var certificateHolder: CertificateHolder? = null

	private val availableCountryCodesMutable = MutableStateFlow<List<String>>(emptyList())
	val availableCountryCodes = availableCountryCodesMutable.asStateFlow()

	private val selectedCountryCodeMutable = MutableStateFlow<String?>(null)
	val selectedCountryCode = selectedCountryCodeMutable.asStateFlow()

	private val selectedDateTimeMutable = MutableStateFlow(LocalDateTime.now())
	val selectedDateTime = selectedDateTimeMutable.asStateFlow()

	val verificationState = combine(selectedCountryCode, selectedDateTime) { countryCode, checkDate ->
		val certificate = certificateHolder ?: return@combine null
		when {
			countryCode == null -> null
			checkDate < LocalDateTime.now() -> null
			else -> CovidCertificateSdk.Wallet.verify(certificate, emptySet(), countryCode, checkDate, viewModelScope)
		}
	}.filterNotNull().flattenMerge()

	init {
		viewModelScope.launch(Dispatchers.IO) {
			availableCountryCodesMutable.value = CovidCertificateSdk.Wallet.getForeignRulesCountryCodes()
		}
	}

	fun setSelectedCountry(countryCode: String) {
		selectedCountryCodeMutable.value = countryCode
	}

	fun setSelectedDate(date: LocalDate) {
		selectedDateTimeMutable.update {
			it.withYear(date.year).withMonth(date.monthValue).withDayOfMonth(date.dayOfMonth)
		}
	}

	fun setSelectedTime(time: LocalTime) {
		selectedDateTimeMutable.update {
			it.withHour(time.hour).withMinute(time.minute)
		}
	}

}