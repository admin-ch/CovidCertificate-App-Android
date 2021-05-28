/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.utils

import android.util.Base64
import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.*

fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.DEFAULT).trim()
fun ByteArray.toBase64NoPadding(): String = Base64.encodeToString(this, Base64.NO_PADDING).trim()
fun String.toBase64NoPadding(): String = Base64.encodeToString(this.toByteArray(), Base64.NO_PADDING).trim()

fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.DEFAULT)
fun String.fromBase64NoPadding(): ByteArray = Base64.decode(this, Base64.NO_PADDING)

// NB: toString() does not work!
fun String.fromBase64ToString(): String = String(bytes = this.fromBase64())
fun String.fromBase64NoPaddingToString(): String = String(bytes = this.fromBase64NoPadding())


object CryptoUtil {
	/**
	 * Creates a {@link java.security.PublicKey} from a coordinate point (x, y).
	 * Assumes curve P-256.
	 */
	fun ecPublicKeyFromCoordinate(x: ByteArray, y: ByteArray): PublicKey {
		// x, y are unsigned (recall that they are just field elements)
		val x = BigInteger(1, x)
		val y = BigInteger(1, y)

		val ecParameters = ECGenParameterSpec("prime256v1")
		val algorithmParameters = AlgorithmParameters.getInstance("EC")
		algorithmParameters.init(ecParameters)

		val ecParameterSpec: ECParameterSpec = algorithmParameters.getParameterSpec(ECParameterSpec::class.java)
		val ecPoint = ECPoint(x, y)
		val ecKeySpec = ECPublicKeySpec(ecPoint, ecParameterSpec)

		val keyFactory = KeyFactory.getInstance("EC")
		return keyFactory.generatePublic(ecKeySpec)
	}

	/**
	 * Creates a {@link java.security.PublicKey} from an RSA modulus n exponent e
	 */
	fun rsaPublicKeyFromModulusExponent(n: ByteArray, e: ByteArray): PublicKey {
		val rsaPublicKeySpec = RSAPublicKeySpec(BigInteger(1, n), BigInteger(1, e))
		val keyFactory = KeyFactory.getInstance("RSA")
		return keyFactory.generatePublic(rsaPublicKeySpec)
	}
}