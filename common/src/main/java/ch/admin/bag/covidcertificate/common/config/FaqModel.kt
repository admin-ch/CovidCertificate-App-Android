package ch.admin.bag.covidcertificate.common.config

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FaqModel(
	val faqTitle: String,
	val faqSubTitle: String?,
	val faqIconAndroid: String?,
	val faqEntries: List<FaqEntryModel>?
)