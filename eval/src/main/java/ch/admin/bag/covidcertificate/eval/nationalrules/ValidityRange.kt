package ch.admin.bag.covidcertificate.eval.nationalrules

import java.time.LocalDateTime

data class ValidityRange(val validFrom: LocalDateTime?, val validUntil: LocalDateTime?)