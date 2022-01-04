package ch.admin.bag.covidcertificate.common.config

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RefreshButtonInfoModel(
	val title: String,
	val text1: String,
	val text2: String,
	val fatTitle: String,
	val text3: String,
	val linkText: String,
	val linkUrl: String,
)