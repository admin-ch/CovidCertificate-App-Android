/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.transfercode.model

import java.io.Serializable
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

data class TransferCodeModel(
	val code: String,
	val publicKey: String,
	val algorithm: PublicKeyAlgorithm,
	val signaturePayload: String,
	val signature: String,
	val creationTimestamp: Long,
	val expirationTimestamp: Long
): Serializable {

	fun getDaysUntilExpiration(): Int {
		val now = Instant.now().toEpochMilli()
		val diff = expirationTimestamp - now
		return (diff.toDouble() / TimeUnit.DAYS.toMillis(1)).roundToInt()
	}
}

enum class PublicKeyAlgorithm {
	EC256, RSA2048
}