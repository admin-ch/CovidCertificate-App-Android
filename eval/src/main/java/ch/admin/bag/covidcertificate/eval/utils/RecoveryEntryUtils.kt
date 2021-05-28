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

import ch.admin.bag.covidcertificate.eval.data.RecoveryEntry
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_DATE
import java.util.*


fun RecoveryEntry.isTargetDiseaseCorrect(): Boolean {
	return this.tg == AcceptanceCriterias.TARGET_DISEASE
}

fun RecoveryEntry.dateFormattedOfFirstPostiveResult(dateFormatter: DateTimeFormatter): String? {
	if (this.fr.isNullOrEmpty()) {
		return null
	}
	return try {
		LocalDate.parse(this.fr, ISO_DATE).format(dateFormatter)
	} catch (e: java.lang.Exception) {
		this.fr
	}
}

fun RecoveryEntry.getRecoveryCountry(): String {
	return try {
		val loc = Locale("", this.co)
		loc.displayCountry
	} catch (e: Exception) {
		this.co
	}
}

fun RecoveryEntry.getIssuer(): String {
	return this.`is`
}

fun RecoveryEntry.getCertificateIdentifier(): String {
	return this.ci
}

fun RecoveryEntry.validFromDate(): LocalDateTime? {
	val firstPositiveResultDate = this.firstPostiveResult() ?: return null
	return firstPositiveResultDate.plusDays(AcceptanceCriterias.RECOVERY_OFFSET_VALID_FROM_DAYS)
}

fun RecoveryEntry.validUntilDate(): LocalDateTime? {
	val firstPositiveResultDate = this.firstPostiveResult() ?: return null
	return firstPositiveResultDate.plusDays(AcceptanceCriterias.RECOVERY_OFFSET_VALID_UNTIL_DAYS)
}

fun RecoveryEntry.firstPostiveResult(): LocalDateTime? {
	if (this.fr.isNullOrEmpty()) {
		return null
	}
	val date: LocalDate?
	try {
		date = LocalDate.parse(this.fr, ISO_DATE)
	} catch (e: Exception) {
		return null
	}
	return date.atStartOfDay()
}



