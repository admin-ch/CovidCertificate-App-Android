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

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ch.admin.bag.covidcertificate.common.browserstack.Normal
import ch.admin.bag.covidcertificate.sdk.android.utils.EncryptedSharedPreferencesUtil
import ch.admin.bag.covidcertificate.wallet.data.WalletSecureStorage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@Normal
@RunWith(AndroidJUnit4::class)
class ScanCountTest() {

	lateinit var context: Context
	lateinit var walletSecureStorage: WalletSecureStorage

	@Before
	fun setup() {
		context = InstrumentationRegistry.getInstrumentation().targetContext
		EncryptedSharedPreferencesUtil.initializeSharedPreferences(context, "SecureStorage").edit().clear().apply()
		walletSecureStorage = WalletSecureStorage.getInstance(context)
	}

	@Test
	fun checkInsertTodayLessThan10() {
		for (i in 1..9) {
			walletSecureStorage.addLastScanTimes(System.currentTimeMillis())
		}
		assertFalse(walletSecureStorage.has10OrMoreScansInLast24h())
	}

	@Test
	fun checkInsertTodayExactly10() {
		for (i in 1..10) {
			walletSecureStorage.addLastScanTimes(System.currentTimeMillis())
		}
		assertTrue(walletSecureStorage.has10OrMoreScansInLast24h())
	}

	@Test
	fun checkInsertTodayMoreThan10() {
		for (i in 1..15) {
			walletSecureStorage.addLastScanTimes(System.currentTimeMillis())
		}
		assertTrue(walletSecureStorage.has10OrMoreScansInLast24h())
	}

	@Test
	fun checkInsertTodayMoreThan10Yesterday() {
		for (i in 1..15) {
			walletSecureStorage.addLastScanTimes(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)
		}
		assertFalse(walletSecureStorage.has10OrMoreScansInLast24h())
	}

	@Test
	fun checkInsertTodayMoreThan10YesterdayAndToday() {
		for (i in 1..15) {
			walletSecureStorage.addLastScanTimes(System.currentTimeMillis())
			walletSecureStorage.addLastScanTimes(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)
		}
		assertTrue(walletSecureStorage.has10OrMoreScansInLast24h())
	}

}