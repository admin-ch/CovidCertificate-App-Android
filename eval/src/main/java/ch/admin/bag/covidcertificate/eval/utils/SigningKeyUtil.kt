package ch.admin.bag.covidcertificate.eval.utils

import ch.admin.bag.covidcertificate.eval.models.Jwk
import ch.admin.bag.covidcertificate.verifier.eval.BuildConfig

internal fun getHardcodedBagSigningKeys(): List<Jwk> {
	val kid = BuildConfig.SIGNING_KEY_KID
	val n = BuildConfig.SIGNING_KEY_N
	val e = BuildConfig.SIGNING_KEY_E

	return listOf(
		Jwk.fromNE(kid, n, e, use = "")
	)
}