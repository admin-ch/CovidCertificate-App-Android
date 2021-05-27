package ch.admin.bag.covidcertificate.eval.products

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ValueSet(
	val valueSetId: String,
	val valueSetDate: String,
	val valueSetValues: Map<String, ValueEntry>,
)

@JsonClass(generateAdapter = true)
data class ValueEntry(
	val display: String,
	val lang: String,
	val active: Boolean,
	val system: String,
	val version: String,
)
