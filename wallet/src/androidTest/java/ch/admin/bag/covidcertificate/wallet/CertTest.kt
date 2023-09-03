/*
 * Copyright (c) 2022 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet

import androidx.test.espresso.intent.Intents
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import ch.admin.bag.covidcertificate.common.browserstack.Certificates
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import ch.admin.bag.covidcertificate.wallet.VaccineType.*


private val DAYS = 24 * 60 * 60 *1000L
private val HOURS = 60 * 60 *1000L
private val NOW = System.currentTimeMillis()

private val VACCINE_VALIDITY_2OF2_END = 270
private val VACCINE_VALIDITY_1OF1_START = 21
private val VACCINE_VALIDITY_1OF1_END = 270 + VACCINE_VALIDITY_1OF1_START

private val PCR_VALIDITY = 72
private val RAT_VALIDITY = 24

private val RECOVERY_VALIDITY_START = 10
private val RECOVERY_VALIDITY_END = 180

@Certificates
@RunWith(AndroidJUnit4::class)
class CertTest : EspressoUtil() {

	@Rule
	@JvmField
	var mActivityTestRule = ActivityTestRule(MainActivity::class.java)

	@Test
	fun testValidVaccines() {
		Intents.init()
		if(needsOnboarding()) doOnboarding()



		importCert(
			downloadVaccineCert(PFIZER, 3, 3, Date(NOW - (VACCINE_VALIDITY_2OF2_END-1) * DAYS))
		)
		checkCertValidity(UiValidityState.VALID)

		importCert(
			downloadVaccineCert(MODERNA, 2, 2, Date(NOW - (VACCINE_VALIDITY_2OF2_END-1) * DAYS))
		)
		checkCertValidity(UiValidityState.VALID)

		importCert(
			downloadVaccineCert(ASTRAZENECA, 3, 3, Date(NOW - (VACCINE_VALIDITY_2OF2_END-1) * DAYS))
		)
		checkCertValidity(UiValidityState.VALID)

		importCert(
			downloadVaccineCert(NUVOVAXOVID, 3, 3, Date(NOW - (VACCINE_VALIDITY_2OF2_END-1) * DAYS))
		)
		checkCertValidity(UiValidityState.VALID)

		importCert(
			downloadVaccineCert(CORONAVAC, 3, 3, Date(NOW - (VACCINE_VALIDITY_2OF2_END-1) * DAYS))
		)
		checkCertValidity(UiValidityState.VALID)

		importCert(
			downloadVaccineCert(CORV, 3, 3, Date(NOW - (VACCINE_VALIDITY_2OF2_END-1) * DAYS))
		)
		checkCertValidity(UiValidityState.VALID)

		importCert(
			downloadVaccineCert(COVISHIELD, 3, 3, Date(NOW - (VACCINE_VALIDITY_2OF2_END-1) * DAYS))
		)
		checkCertValidity(UiValidityState.VALID)

		importCert(
			downloadVaccineCert(COVAXIN, 3, 3, Date(NOW - (VACCINE_VALIDITY_2OF2_END-1) * DAYS))
		)
		checkCertValidity(UiValidityState.VALID)

		importCert(
			downloadVaccineCert(R_COVI, 3, 3, Date(NOW - (VACCINE_VALIDITY_2OF2_END-1) * DAYS))
		)
		checkCertValidity(UiValidityState.VALID)

		importCert(
			downloadVaccineCert(RECOMBININANT, 3, 3, Date(NOW - (VACCINE_VALIDITY_2OF2_END-1) * DAYS))
		)
		checkCertValidity(UiValidityState.VALID)

		importCert(
			downloadVaccineCert(COVOVAX, 3, 3, Date(NOW - (VACCINE_VALIDITY_2OF2_END-1) * DAYS))
		)
		checkCertValidity(UiValidityState.VALID)

		importCert(
			downloadVaccineCert(JANSSEN, 1, 1, Date(NOW - (VACCINE_VALIDITY_1OF1_START) * DAYS))
		)
		checkCertValidity(UiValidityState.VALID)

		importCert(
			downloadVaccineCert(JANSSEN, 1, 1, Date(NOW - (VACCINE_VALIDITY_2OF2_END-1) * DAYS))
		)
		checkCertValidity(UiValidityState.VALID)

		importCert(
			downloadVaccineCert(JANSSEN, 2, 2, Date(NOW))
		)
		checkCertValidity(UiValidityState.VALID)

	}

	@Test
	fun testExpiredVaccines() {
		Intents.init()
		if(needsOnboarding()) doOnboarding()

		importCert(
			downloadVaccineCert(MODERNA, 2,2, Date( NOW - VACCINE_VALIDITY_2OF2_END * DAYS))
		)
		checkCertValidity(UiValidityState.EXPIRED)

		importCert(
			downloadVaccineCert(PFIZER, 3,3, Date( NOW - VACCINE_VALIDITY_2OF2_END * DAYS))
		)
		checkCertValidity(UiValidityState.EXPIRED)

		importCert(
			downloadVaccineCert(JANSSEN, 2,2, Date( NOW - VACCINE_VALIDITY_2OF2_END * DAYS))
		)
		checkCertValidity(UiValidityState.EXPIRED)

		importCert(
			downloadVaccineCert(JANSSEN, 1,1, Date( NOW - VACCINE_VALIDITY_1OF1_END * DAYS))
		)
		checkCertValidity(UiValidityState.EXPIRED)

	}

	@Test
	fun testNotYetValidVaccines(){
		Intents.init()
		if(needsOnboarding()) doOnboarding()

		importCert(
			downloadVaccineCert(JANSSEN, 1,1, Date( NOW - (VACCINE_VALIDITY_1OF1_START-1) * DAYS))
		)
		checkCertValidity(UiValidityState.NOT_YET_VALID)

	}

	@Test
	fun testValidTestCerts(){
		Intents.init()
		if(needsOnboarding()) doOnboarding()
		importCert(
			downloadTestCert(TestType.PCR, Date(NOW - (PCR_VALIDITY - 1) * HOURS))
		)
		checkCertValidity(UiValidityState.VALID)

		importCert(
			downloadTestCert(TestType.RAT, Date(NOW - (RAT_VALIDITY - 1) * HOURS))
		)
		checkCertValidity(UiValidityState.VALID)
	}

	@Test
	fun testExpiredTestCerts(){
		Intents.init()
		if(needsOnboarding()) doOnboarding()
		//extra time added to account for timezones and summer time
		importCert(
			downloadTestCert(TestType.PCR, Date(NOW - (PCR_VALIDITY + 2) * HOURS))
		)
		checkCertValidity(UiValidityState.EXPIRED)

		importCert(
			downloadTestCert(TestType.RAT, Date(NOW - (RAT_VALIDITY + 2) * HOURS))
		)
		checkCertValidity(UiValidityState.EXPIRED)
	}


	@Test
	fun testRecoveryCerts(){
		Intents.init()
		if(needsOnboarding()) doOnboarding()
		importCert(
			downloadRecoveryCert(Date(NOW - (RECOVERY_VALIDITY_START - 1 ) * DAYS))
		)
		checkCertValidity(UiValidityState.NOT_YET_VALID)

		importCert(
			downloadRecoveryCert(Date(NOW - (RECOVERY_VALIDITY_START ) * DAYS))
		)
		checkCertValidity(UiValidityState.VALID)

		importCert(
			downloadRecoveryCert(Date(NOW - (RECOVERY_VALIDITY_END - 1 ) * DAYS))
		)
		checkCertValidity(UiValidityState.VALID)


		importCert(
			downloadRecoveryCert(Date(NOW - (RECOVERY_VALIDITY_END  ) * DAYS))
		)
		checkCertValidity(UiValidityState.EXPIRED)
	}


}
