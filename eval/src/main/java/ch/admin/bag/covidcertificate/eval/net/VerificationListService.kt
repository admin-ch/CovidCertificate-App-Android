package ch.admin.bag.covidcertificate.eval.net

import ch.admin.bag.covidcertificate.eval.models.Jwks
import ch.admin.bag.covidcertificate.eval.models.RevokedList
import ch.admin.bag.covidcertificate.eval.models.RuleSet
import retrofit2.Response
import retrofit2.http.GET

interface VerificationListService {

	@GET("jwks.json")
	suspend fun getSigningKeysList(): Response<Jwks>

	@GET("revokedList.json")
	suspend fun getRevokedList(): Response<RevokedList>

	@GET("ruleset.json")
	suspend fun getRuleSet(): Response<RuleSet>

}