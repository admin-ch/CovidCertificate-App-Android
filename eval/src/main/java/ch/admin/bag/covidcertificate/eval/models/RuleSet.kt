package ch.admin.bag.covidcertificate.eval.models

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class RuleSet(
	val rules: List<Rule>
)

@Serializable
@JsonClass(generateAdapter = true)
data class Rule(
	val name: String,
	val description: String,
	val expression: Expression
)

@Serializable
@JsonClass(generateAdapter = true)
data class Expression(
	val version: String,
	val type: String,
	val value: String
)


