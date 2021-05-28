/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.utils

import ch.admin.bag.covidcertificate.eval.data.TestEntry
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*


fun TestEntry.isNegative(): Boolean {
	return this.tr == AcceptanceCriterias.NEGATIVE_CODE
}

fun TestEntry.isTargetDiseaseCorrect(): Boolean {
	return this.tg == AcceptanceCriterias.TARGET_DISEASE
}

fun TestEntry.getFormattedSampleDate(dateTimeFormatter: DateTimeFormatter): String? {
	if (this.sc == null) {
		return null
	}
	return try {
		return this.sc.toInstant().atZone(ZoneId.systemDefault()).format(dateTimeFormatter)
	} catch (e: Exception) {
		null
	}
}

fun TestEntry.getFormattedResultDate(dateTimeFormatter: DateTimeFormatter): String? {
	if (this.dr == null) {
		return null
	}
	return try {
		this.dr.toInstant().atZone(ZoneId.systemDefault()).format(dateTimeFormatter)
	} catch (e: Exception) {
		null
	}
}

fun TestEntry.getTestCenter(): String? {
	if (!this.tc.isNullOrEmpty()) {
		return this.tc
	}
	return null
}

fun TestEntry.getTestCountry(): String {
	return try {
		val loc = Locale("", this.co)
		loc.displayCountry
	} catch (e: Exception) {
		this.co
	}
}

fun TestEntry.getIssuer(): String {
	return this.`is`
}

fun TestEntry.getCertificateIdentifier(): String {
	return this.ci
}

fun TestEntry.validFromDate(): LocalDateTime? {
	if (this.sc == null) {
		return null
	}
	return try {
		this.sc.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
	} catch (e: Exception) {
		return null
	}
}

fun TestEntry.validUntilDate(testEntry: TestEntry): LocalDateTime? {
	val startDate = this.validFromDate() ?: return null
	val testEntryCode = testEntry.tt ?: return null
	if (testEntryCode == TestType.PCR.code) {
		return startDate.plusHours(AcceptanceCriterias.PCR_TEST_VALIDITY_IN_HOURS)
	} else if (testEntryCode == TestType.RAT.code) {
		return startDate.plusHours(AcceptanceCriterias.RAT_TEST_VALIDITY_IN_HOURS)
	}
	return null
}

