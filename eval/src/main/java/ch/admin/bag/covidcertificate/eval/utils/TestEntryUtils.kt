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

import android.text.TextUtils
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

fun TestEntry.getFormattedSampleDate(dateTimeFormatter: DateTimeFormatter): String {
	return this.sc.toInstant().atZone(ZoneId.systemDefault()).format(dateTimeFormatter)
}

fun TestEntry.getFormattedResultDate(dateTimeFormatter: DateTimeFormatter): String {
	return this.dr.toInstant().atZone(ZoneId.systemDefault()).format(dateTimeFormatter)
}

fun TestEntry.getTestCenter(): String? {
	if (!TextUtils.isEmpty(this.tc)) {
		return this.tc
	}
	return null
}

fun TestEntry.getTestCountry(): String {
	val loc = Locale("", this.co)
	return loc.displayCountry
}

fun TestEntry.getIssuer(): String {
	return this.`is`
}

fun TestEntry.getCertificateIdentifier(): String {
	return this.ci
}

fun TestEntry.validFromDate(): LocalDateTime? {
	return this.sc.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
}

fun TestEntry.validUntilDate(testEntry: TestEntry): LocalDateTime? {
	val startDate = this.validFromDate() ?: return null
	if (testEntry.tt.equals(TestType.PCR.code)) {
		return startDate.plusHours(AcceptanceCriterias.PCR_TEST_VALIDITY_IN_HOURS)
	} else if (testEntry.tt.equals(TestType.RAT.code)) {
		return startDate.plusHours(AcceptanceCriterias.RAT_TEST_VALIDITY_IN_HOURS)
	}
	return null
}

