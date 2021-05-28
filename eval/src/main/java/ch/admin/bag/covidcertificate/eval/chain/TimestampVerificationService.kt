/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.chain

import java.time.Instant

class TimestampVerificationService {
	private val TAG = TimestampVerificationService::class.java.simpleName

	fun validate(verificationResult: VerificationResult) {
		verificationResult.timestampVerified = true
		val now = Instant.now()

		verificationResult.expirationTime?.also { et ->
			if (et.isBefore(now)) {
				verificationResult.timestampVerified = false
			}
		}

		verificationResult.issuedAt?.also { ia ->
			if (ia.isAfter(now)) {
				verificationResult.timestampVerified = false
			}
		}
	}

}