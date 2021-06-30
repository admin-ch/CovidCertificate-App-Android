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

import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.StyleSpan
import ch.admin.bag.covidcertificate.sdk.android.extensions.DEFAULT_DISPLAY_DATE_FORMATTER
import ch.admin.bag.covidcertificate.sdk.android.extensions.DEFAULT_DISPLAY_DATE_TIME_FORMATTER
import java.time.LocalDateTime

fun String.makeBold(): SpannableString = SpannableString(this).apply {
	setSpan(StyleSpan(Typeface.BOLD), 0, this@makeBold.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
}

fun String.makeSubStringBold(
	subString: String,
	startIndex: Int = 0,
	ignoreCase: Boolean = true
): SpannableString =
	SpannableString(this).apply {
		val indexSubString = this@makeSubStringBold.indexOf(subString, startIndex, ignoreCase)
		if (indexSubString >= 0) {
			setSpan(
				StyleSpan(Typeface.BOLD),
				indexSubString,
				indexSubString + subString.length,
				SpannableString.SPAN_INCLUSIVE_EXCLUSIVE
			)
		}
	}

fun String.addBoldDate(dateReplacementString: String, date: LocalDateTime): SpannableString {
	val dateString = date.format(DEFAULT_DISPLAY_DATE_FORMATTER)
	return SpannableString(this.replace(dateReplacementString, dateString)).apply {
		val indexSubString = this.indexOf(dateString)
		if (indexSubString >= 0) {
			setSpan(
				StyleSpan(Typeface.BOLD),
				indexSubString,
				indexSubString + dateString.length,
				SpannableString.SPAN_INCLUSIVE_EXCLUSIVE
			)
		}
	}
}

fun String.addBoldDateTime(dateTimeReplacementString: String, dateTime: LocalDateTime): SpannableString {
	val dateTimeString = dateTime.format(DEFAULT_DISPLAY_DATE_TIME_FORMATTER)
	return SpannableString(this.replace(dateTimeReplacementString, dateTimeString)).apply {
		val indexSubString = this.indexOf(dateTimeString)
		if (indexSubString >= 0) {
			setSpan(
				StyleSpan(Typeface.BOLD),
				indexSubString,
				indexSubString + dateTimeString.length,
				SpannableString.SPAN_INCLUSIVE_EXCLUSIVE
			)
		}
	}
}