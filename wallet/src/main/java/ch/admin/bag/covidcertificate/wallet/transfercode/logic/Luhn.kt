/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package ch.admin.bag.covidcertificate.wallet.transfercode.logic


object Luhn {

	// Missing: G, I, J, L, O, Q, V
	internal val ALPHABET = "1234567890ABCDEFHKMNPRSTUWXYZ".toCharArray()
	private val N = ALPHABET.size
	internal const val TRANSFER_CODE_LEN = 9

	internal fun computeCheckCharacter(code: CharArray): Char {
		val reversed = code.reversedArray()
		var sum = 0
		var factor = 2

		for (char in reversed) {
			// Assumption: code contains only characters from the alphabet
			val toAdd = factor * ALPHABET.indexOf(char)

			sum += (toAdd / N) + (toAdd % N)
			factor = if (factor == 1) 2 else 1
		}

		// The check digit is the smallest number that makes the sum add up to a multiple of N
		var checkChar = N - (sum % N)
		if (checkChar == N) {
			checkChar = 0
		}

		return ALPHABET[checkChar]
	}

	/**
	 * Generates a fresh 9-character transfer code.
	 *
	 * The first 8 chars are chosen at random, the last character is the check character computed using Luhn mod N.
	 */
	fun generateNewTransferCode(): String {
		val code = CharArray(TRANSFER_CODE_LEN)

		// Generate the first 8 chars at random
		for (i in 0 until TRANSFER_CODE_LEN - 1) {
			code[i] = ALPHABET.random()
		}

		// Append the check character
		val range = 0 until TRANSFER_CODE_LEN - 1 // inclusive to inclusive!
		code[TRANSFER_CODE_LEN - 1] = computeCheckCharacter(code.sliceArray(range))

		return code.concatToString()
	}
}