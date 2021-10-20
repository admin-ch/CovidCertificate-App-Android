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

import com.squareup.moshi.JsonClass
import java.io.Serializable
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@JsonClass(generateAdapter = true)
data class TransferCodeModel(
	val code: String,
	val creationTimestamp: Instant,
	val lastUpdatedTimestamp: Instant,
): Serializable {

	val expirationTimestamp = creationTimestamp.plus(30, ChronoUnit.DAYS)
	val failureTimestamp = expirationTimestamp.plus(72, ChronoUnit.HOURS)

	fun isExpired() = expirationTimestamp.isBefore(Instant.now())

	fun isFailed() = failureTimestamp.isBefore(Instant.now())

	fun getDaysUntilExpiration(): Int {
		val now = Instant.now().toEpochMilli()
		val diff = expirationTimestamp.toEpochMilli() - now
		return (diff.toDouble() / TimeUnit.DAYS.toMillis(1)).roundToInt()
	}
}