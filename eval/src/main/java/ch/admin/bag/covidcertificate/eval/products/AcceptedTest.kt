package ch.admin.bag.covidcertificate.eval.products

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AcceptedTest(
	val Id: String,
	val Date: String,
	val Version: String,
	val entries: List<Test>,
)

@JsonClass(generateAdapter = true)
data class Test(
	val name: String,
	val type: String,
	val type_code: String,
	val manufacturer: String,
	val swiss_test_kit: String,
	val manufacturer_code_eu: String,
	val eu_accepted: Boolean,
	val ch_accepted: Boolean,
	val active: Boolean,
)
