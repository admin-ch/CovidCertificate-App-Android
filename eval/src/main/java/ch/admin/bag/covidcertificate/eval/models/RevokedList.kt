package ch.admin.bag.covidcertificate.eval.models

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class RevokedList(
	val revokedCertificates: List<CertIdentifier> = emptyList()
)

/**
 * Unique Vaccination Certificate Identifier UVCI
 *
 * See https://ec.europa.eu/health/sites/default/files/ehealth/docs/vaccination-proof_interoperability-guidelines_en.pdf#page=11
 */
@Serializable
@JsonClass(generateAdapter = true)
data class CertIdentifier(
	val dgci: String
)