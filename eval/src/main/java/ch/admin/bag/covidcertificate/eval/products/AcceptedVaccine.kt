package ch.admin.bag.covidcertificate.eval.products

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AcceptedVaccine(
	val Id: String,
	val Date: String,
	val Version: String,
	val entries: List<Vaccine>,
)

@JsonClass(generateAdapter = true)
data class Vaccine(
	val name: String,
	val code: String,
	val prophylaxis: String,
	val prophylaxis_code: String,
	val auth_holder: String,
	val auth_holder_code: String,
	val total_dosis_number: Int,
	val active: Boolean,
)
