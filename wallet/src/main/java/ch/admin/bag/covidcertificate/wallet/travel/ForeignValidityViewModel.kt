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
import ch.admin.bag.covidcertificate.sdk.core.models.state.VerificationState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
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

	private val foreignValidityFormState = combine(selectedCountryCode, selectedDateTime) { countryCode, checkDate ->
		countryCode to checkDate
	}

	private val verificationStateMutable = MutableStateFlow<VerificationState?>(null)
	val verificationState = verificationStateMutable.asStateFlow()

	private var verificationJob: Job? = null

	private val viewStateMutable = MutableStateFlow<ForeignValidityViewState>(ForeignValidityViewState.LOADING)
	val viewState = viewStateMutable.asStateFlow()

	init {
		loadAvailableCountryCodes()

		viewModelScope.launch(Dispatchers.IO) {
			foreignValidityFormState.collect { (countryCode, checkDate) ->
				startVerification(countryCode, checkDate)
			}
		}
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
		startVerification(selectedCountryCode.value, selectedDateTime.value)
	}

	private fun startVerification(countryCode: String?, checkDate: LocalDateTime) {
		val certificate = certificateHolder ?: return

		if (countryCode == null || checkDate < LocalDateTime.now().minusMinutes(5)) {
			verificationStateMutable.value = null
			return
		}

		verificationJob?.cancel()
		verificationJob = viewModelScope.launch {
			// Add an artificial delay because changing the date only would not really show a loading indication to the user
			verificationStateMutable.value = VerificationState.LOADING
			delay(1000L)

			val verificationStateFlow = CovidCertificateSdk.Wallet.verify(certificate, emptySet(), countryCode, checkDate, viewModelScope)
			verificationStateFlow.collect { state ->
				verificationStateMutable.value = state

				// Once the verification state is not loading anymore, cancel the flow collection job (otherwise the flow stays active without emitting anything)
				if (state !is VerificationState.LOADING) {
					verificationJob?.cancel()
					verificationJob = null
				}
			}
		}
	}

}