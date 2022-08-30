package ch.admin.bag.covidcertificate.common.extensions

import ch.admin.bag.covidcertificate.sdk.core.extensions.firstPositiveResult
import ch.admin.bag.covidcertificate.sdk.core.extensions.vaccineDate
import ch.admin.bag.covidcertificate.sdk.core.extensions.validFromDate
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.DccCert
import java.time.LocalDateTime

fun DccCert.getDate(): LocalDateTime? {
	var date = this.vaccinations?.firstOrNull()?.vaccineDate()

	if (date == null) {
		date = this.pastInfections?.firstOrNull()?.firstPositiveResult()
	}
	if (date == null) {
		date = this.tests?.firstOrNull()?.validFromDate()
	}
	return date
}