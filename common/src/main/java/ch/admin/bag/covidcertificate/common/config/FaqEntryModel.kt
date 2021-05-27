package ch.admin.bag.covidcertificate.common.config

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FaqEntryModel(
	val title: String,
	val text: String,
	val iconAndroid: String?,
	val linkTitle: String?,
	val linkUrl: String?
)