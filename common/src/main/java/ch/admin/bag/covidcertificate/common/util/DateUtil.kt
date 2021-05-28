/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.common.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_DATE

val DEFAULT_DISPLAY_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
val DEFAULT_DISPLAY_DATE_FORMAT_FULL_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
val DEFAULT_DISPLAY_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm")

fun String.parseIsoTimeAndFormat(dateTimeFormatter: DateTimeFormatter): String {
	return try {
		LocalDate.parse(this, ISO_DATE).format(dateTimeFormatter)
	} catch (e: java.lang.Exception) {
		this
	}
}
