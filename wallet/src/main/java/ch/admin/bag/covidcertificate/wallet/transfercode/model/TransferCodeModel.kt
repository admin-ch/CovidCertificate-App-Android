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

/**
 * The expiresAt and failsAt default values are set to 7d/72h due to the extended validity starting with the 2.7.0 release.
 * Existing transfer codes should automatically be migrated with these default values, while newer codes should set the correct
 * validity range based on the server configuration.
 */
@JsonClass(generateAdapter = true)
data class TransferCodeModel(
	val code: String,
	val creationTimestamp: Instant,
	val lastUpdatedTimestamp: Instant,
	val expiresAtTimestamp: Instant = creationTimestamp.plus(7, ChronoUnit.DAYS),
	val failsAtTimestamp: Instant = expiresAtTimestamp.plus(72, ChronoUnit.HOURS),
): Serializable {

	fun isExpired() = expiresAtTimestamp.isBefore(Instant.now())

	fun isFailed() = failsAtTimestamp.isBefore(Instant.now())

	fun getDaysUntilExpiration(): Int {
		val now = Instant.now().toEpochMilli()
		val diff = expiresAtTimestamp.toEpochMilli() - now
		return (diff.toDouble() / TimeUnit.DAYS.toMillis(1)).roundToInt()
	}
}