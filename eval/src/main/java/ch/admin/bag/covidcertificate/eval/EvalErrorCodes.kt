/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval

object EvalErrorCodes {
	val DECODE_BASE_45 = "D|B45"
	val DECODE_Z_LIB = "D|ZLB"
	val DECODE_UNKNOWN = "D|UNK"
	val SIGNATURE_NETWORK = "S|NWN"
	val SIGNATURE_UNKNOWN= "S|UNK"
	val REVOCATION_NETWORK = "R|NWN"
	val REVOCATION_UNKNOWN = "R|UNK"
	val RULESET_NETWORK = "N|NWN"
	val RULESET_UNKNOWN = "N|UNK"
	val LIST_LOAD_UNK = "L|UNK"

	val SIGNATURE_TIMESTAMP_INVALID = "S|TSI"
	val SIGNATURE_BAGDGC_TYPE_INVALID = "S|BTI"
	val SIGNATURE_COSE_INVALID = "S|CSI"

	val NO_VALID_DATE = "N|NVD"
	val NO_VALID_PRODUCT = "N|NVP"
	val WRONG_DISEASE_TARGET = "N|WDT"
	val WRONG_TEST_TYPE = "N|WTT"
	val POSITIVE_RESULT = "N|PR"
	val NOT_FULLY_PROTECTED = "N|NFP"

}