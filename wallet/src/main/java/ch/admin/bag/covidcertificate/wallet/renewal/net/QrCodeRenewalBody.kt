package ch.admin.bag.covidcertificate.wallet.renewal.net

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QrCodeRenewalBody(val hcert: String)
