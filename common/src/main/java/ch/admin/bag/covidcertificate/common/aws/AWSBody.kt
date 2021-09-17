package ch.admin.bag.covidcertificate.common.aws

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AWSBody(
	val image: String,
	val success: Boolean
)
