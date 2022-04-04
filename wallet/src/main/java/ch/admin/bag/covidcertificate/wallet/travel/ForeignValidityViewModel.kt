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

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ch.admin.bag.covidcertificate.sdk.android.CovidCertificateSdk
import ch.admin.bag.covidcertificate.sdk.android.utils.NetworkUtil
import ch.admin.bag.covidcertificate.sdk.core.data.ErrorCodes
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.sdk.core.models.state.StateError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ForeignValidityViewModel(application: Application) : AndroidViewModel(application) {

	private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

	var certificateHolder: CertificateHolder? = null

	private val availableCountryCodesMutable = MutableStateFlow<List<String>>(emptyList())
	val availableCountryCodes = availableCountryCodesMutable.asStateFlow()

	private val selectedCountryCodeMutable = MutableStateFlow<String?>(null)
	val selectedCountryCode = selectedCountryCodeMutable.asStateFlow()

	private val selectedDateTimeMutable = MutableStateFlow(LocalDateTime.now())
	val selectedDateTime = selectedDateTimeMutable.asStateFlow()

	// A separate shared flow (without conflation like a StateFlow) to trigger a manual reverification without the country or date changing
	private val reverifyFlow = MutableSharedFlow<Boolean>(replay = 1).also { it.tryEmit(true) }

	val verificationState = combine(selectedCountryCode, selectedDateTime, reverifyFlow) { countryCode, checkDate, _ ->
		val certificate = certificateHolder ?: return@combine flowOf(null)
		when {
			countryCode == null -> flowOf(null)
			checkDate < LocalDateTime.now().minusMinutes(5) -> flowOf(null)
			else -> CovidCertificateSdk.Wallet.verify(certificate, emptySet(), countryCode, checkDate, viewModelScope)
		}
	}.flatMapLatest { it }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

	private val viewStateMutable = MutableStateFlow<ForeignValidityViewState>(ForeignValidityViewState.LOADING)
	val viewState = viewStateMutable.asStateFlow()

	init {
		loadAvailableCountryCodes()
	}

	fun loadAvailableCountryCodes() {
		viewModelScope.launch(Dispatchers.IO) {
			viewStateMutable.value = ForeignValidityViewState.LOADING
			try {
				availableCountryCodesMutable.value = CovidCertificateSdk.Wallet.getForeignRulesCountryCodes().toList()
				viewStateMutable.value = ForeignValidityViewState.SUCCESS
			} catch (e: Exception) {
				if (NetworkUtil.isNetworkAvailable(connectivityManager)) {
					viewStateMutable.value = ForeignValidityViewState.ERROR(StateError(ErrorCodes.GENERAL_NETWORK_FAILURE))
				} else {
					viewStateMutable.value = ForeignValidityViewState.ERROR(StateError(ErrorCodes.GENERAL_OFFLINE))
				}
			}
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

	fun reverify() {
		reverifyFlow.tryEmit(true)
	}

}