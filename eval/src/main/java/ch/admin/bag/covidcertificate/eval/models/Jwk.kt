/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.models

import android.util.Log
import ch.admin.bag.covidcertificate.eval.utils.CryptoUtil
import ch.admin.bag.covidcertificate.eval.utils.fromBase64
import com.squareup.moshi.JsonClass
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.security.PublicKey

private const val TAG = "Jwk"

@Serializable
@JsonClass(generateAdapter = true)
data class Jwks(
	val keys: List<Jwk>
)

/**
 * Note that some fields are base64 encoded. E.g. kid, x, y, n, e.
 */
@Serializable
@JsonClass(generateAdapter = true)
data class Jwk(
	val kid: String,
	val kty: String,
	val alg: String,
	val use: String, // For which type of health cert the signing key is valid. One of: r,v,t.
	val x5a: X5A? = null,
	@SerialName("x5t#S256")
	val x5aS256: String? = null,
	val crv: String? = null,
	val x: String? = null,
	val y: String? = null,
	val n: String? = null,
	val e: String? = null,
) {

	companion object {
		fun fromNE(kid: String, n: String, e: String, use: String) = Jwk(
			kid = kid,
			kty = "RSA",
			alg = "RSA256",
			use = use,
			n = n,
			e = e,
		)

		fun fromXY(kid: String, x: String, y: String, use: String) = Jwk(
			kid = kid,
			kty = "EC",
			alg = "ES256",
			use = use,
			x = x,
			y = y,
		)
	}

	fun getKid(): ByteArray = kid.fromBase64()

	fun getPublicKey(): PublicKey? {
		try {
			return when (kty) {
				"EC" -> CryptoUtil.ecPublicKeyFromCoordinate(x!!.fromBase64(), y!!.fromBase64())
				"RSA" -> CryptoUtil.rsaPublicKeyFromModulusExponent(n!!.fromBase64(), e!!.fromBase64())
				else -> {
					Log.e(TAG, "Invalid kty!")
					null
				}
			}
		} catch (e: Exception) {
			// Can throw e.g. if the (x, y) pair is not a point on the curve
			e.printStackTrace()
		}
		Log.w(TAG, "Failed to create PublicKey for kid $kid")
		return null
	}

	fun isAllowedToSign(certType: CertType): Boolean {
		return getKeyUsageTypes().contains(certType)
				|| use.isEmpty() // if key.use is empty, we assume the key is allowed to be used for all operations
	}

	fun getKeyUsageTypes(): List<CertType> {
		val certTypes = mutableListOf<CertType>()

		if (use.contains(CertType.VACCINATION.use)) {
			certTypes.add(CertType.VACCINATION)
		}
		if (use.contains(CertType.RECOVERY.use)) {
			certTypes.add(CertType.RECOVERY)
		}
		if (use.contains(CertType.TEST.use)) {
			certTypes.add(CertType.TEST)
		}

		return certTypes
	}
}

@Serializable
@JsonClass(generateAdapter = true)
data class X5A(
	val issuer: String,
	val serial: Long,
	var subject: String
)
