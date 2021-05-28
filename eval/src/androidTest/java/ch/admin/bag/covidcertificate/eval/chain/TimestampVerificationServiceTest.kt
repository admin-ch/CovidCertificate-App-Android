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

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class TimestampVerificationServiceTest {

	private val service = TimestampVerificationService()

	@Test
	fun future_expiration(){
		val vr = VerificationResult().apply {
			expirationTime = Instant.now().plusSeconds(60)
		}
		service.validate(vr)
		assertTrue(vr.timestampVerified)
	}

	@Test
	fun past_expiration(){
		val vr = VerificationResult().apply {
			expirationTime = Instant.now().minusSeconds(60)
		}
		service.validate(vr)
		assertFalse(vr.timestampVerified)
	}

	@Test
	fun no_expiration(){
		val vr = VerificationResult().apply {
			expirationTime = null
		}
		service.validate(vr)
		assertTrue(vr.timestampVerified)
	}

	@Test
	fun future_issuedAt(){
		val vr = VerificationResult().apply {
			issuedAt = Instant.now().plusSeconds(60)
		}
		service.validate(vr)
		assertFalse(vr.timestampVerified)
	}

	@Test
	fun past_issuedAt(){
		val vr = VerificationResult().apply {
			issuedAt = Instant.now().minusSeconds(60)
		}
		service.validate(vr)
		assertTrue(vr.timestampVerified)
	}

	@Test
	fun no_issuedAt(){
		val vr = VerificationResult().apply {
			issuedAt = null
		}
		service.validate(vr)
		assertTrue(vr.timestampVerified)
	}

	@Test
	fun combined_invalid(){
		val vr = VerificationResult().apply {
			expirationTime = Instant.now().plusSeconds(120)
			issuedAt = Instant.now().plusSeconds(60)
		}
		service.validate(vr)
		assertFalse(vr.timestampVerified)
	}

	@Test
	fun combined_valid(){
		val vr = VerificationResult().apply {
			expirationTime = Instant.now().plusSeconds(120)
			issuedAt = Instant.now().minusSeconds(60)
		}
		service.validate(vr)
		assertTrue(vr.timestampVerified)
	}
}