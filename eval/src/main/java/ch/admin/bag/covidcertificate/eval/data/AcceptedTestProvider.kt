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
import ch.admin.bag.covidcertificate.eval.products.ValueSet
import ch.admin.bag.covidcertificate.eval.utils.SingletonHolder
import ch.admin.bag.covidcertificate.eval.utils.TestType
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import okio.buffer
import okio.source
import java.io.IOException

class AcceptedTestProvider private constructor(context: Context) {

	companion object : SingletonHolder<AcceptedTestProvider, Context>(::AcceptedTestProvider) {
		private const val ACCEPTED_TEST_MANUFACTURE_FILE_NAME = "test_manf_eu.json"
		private const val ACCEPTED_TEST_TYPE_FILE_NAME = "test_type_eu.json"
	}

	private val manufactures: ValueSet
	private val acceptedEuTest: ValueSet

	init {
		val manufactureAdapter: JsonAdapter<ValueSet> = Moshi.Builder().build().adapter(ValueSet::class.java)
		manufactures =
			manufactureAdapter.fromJson(context.assets.open(ACCEPTED_TEST_MANUFACTURE_FILE_NAME).source().buffer())
				?: throw IOException()
		acceptedEuTest =
			manufactureAdapter.fromJson(context.assets.open(ACCEPTED_TEST_TYPE_FILE_NAME).source().buffer())
				?: throw IOException()
	}

	fun getTestType(testEntry: TestEntry): String {
		return acceptedEuTest.valueSetValues[testEntry.tt]?.display ?: testEntry.tt
	}

	fun testIsPCRorRAT(testEntry: TestEntry): Boolean {
		return testEntry.tt.equals(TestType.PCR.code) || testEntry.tt.equals(TestType.RAT.code)
	}

	fun testIsAcceptedInEuAndCH(testEntry: TestEntry): Boolean {
		if (testEntry.tt.equals(TestType.PCR.code)) {
			return true
		} else if (testEntry.tt.equals(TestType.RAT.code)) {
			return manufactures.valueSetValues[testEntry.ma]?.let { return true } ?: return false
		}
		return false
	}

	fun getTestName(testEntry: TestEntry): String? {
		if (testEntry.tt.equals(TestType.PCR.code)) {
			return testEntry.nm ?: "PCR"
		} else if (testEntry.tt.equals(TestType.RAT.code)) {
			return testEntry.nm
		}
		return null
	}

	fun getManufacturesIfExists(testEntry: TestEntry): String? {
		val ma = manufactures.valueSetValues[testEntry.ma]?.display?.replace(testEntry.nm, "")?.trim()?.removeSuffix(",")
		return if (ma.isNullOrEmpty()) {
			null
		} else {
			ma
		}
	}
}