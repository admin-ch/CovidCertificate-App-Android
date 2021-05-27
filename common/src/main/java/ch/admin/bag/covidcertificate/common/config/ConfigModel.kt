package ch.admin.bag.covidcertificate.common.config

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ConfigModel(
	val forceUpdate: Boolean,
	val infoBox: Map<String, InfoBoxModel>?,
	val questions: Map<String, FaqModel>?,
	val works: Map<String, FaqModel>?
) {
	fun getInfoBox(languageKey: String?): InfoBoxModel? = infoBox?.get(languageKey)
	fun getQuestionsFaqs(languageKey: String): FaqModel? = questions?.get(languageKey)
	fun getWorksFaqs(languageKey: String): FaqModel? = works?.get(languageKey)
}