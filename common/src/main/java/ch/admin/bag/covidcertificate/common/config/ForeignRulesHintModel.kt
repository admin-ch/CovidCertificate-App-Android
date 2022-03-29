package ch.admin.bag.covidcertificate.common.config

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ForeignRulesHintModel(
	val iconAndroid: String,
	val text: String,
)