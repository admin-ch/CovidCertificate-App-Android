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

import COSE.MessageTag
import COSE.Sign1Message

/**
 * A no-op COSE verification services.
 * Does not acutally check the signature, but simply extracts the message and returns it.
 */
class NoopVerificationCoseService {

	fun decode(input: ByteArray, verificationResult: VerificationResult): ByteArray {
		verificationResult.coseVerified = false
		return try {
			val cose: Sign1Message = Sign1Message.DecodeFromBytes(input, MessageTag.Sign1) as Sign1Message
			cose.GetContent()
		} catch (e: Throwable) {
			input
		}
	}

}