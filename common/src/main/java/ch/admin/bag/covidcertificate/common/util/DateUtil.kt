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
