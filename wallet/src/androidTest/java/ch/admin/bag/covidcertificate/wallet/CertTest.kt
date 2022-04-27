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


private val PFIZER = "EU/1/20/1507"
private val DAYS = 24 * 60 * 60 *1000L

@Certificates
@RunWith(AndroidJUnit4::class)
class CertTest : EspressoUtil() {
	@Rule
	@JvmField
	var mActivityTestRule = ActivityTestRule(MainActivity::class.java)

	@Test
	fun testCertDownload() {
		Intents.init()
		doOnboarding()

		val now = System.currentTimeMillis()


		val uri = downloadVaccineCert(PFIZER, 2,2, Date( now - 365 * DAYS))
		importCert(uri)
		checkCertValidity(UiValidityState.EXPIRED)
		importCert(downloadVaccineCert(PFIZER, 3, 3, Date(now - 1 * DAYS)))
		checkCertValidity(UiValidityState.VALID)
	}

}
