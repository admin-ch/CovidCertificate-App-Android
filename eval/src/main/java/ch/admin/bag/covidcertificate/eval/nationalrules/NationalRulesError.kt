/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.nationalrules

import ch.admin.bag.covidcertificate.eval.EvalErrorCodes

enum class NationalRulesError(val message: String, val errorCode: String) {
	NO_VALID_DATE("Not a valid Date format", EvalErrorCodes.NO_VALID_DATE),
	NO_VALID_PRODUCT("Product is not registered", EvalErrorCodes.NO_VALID_PRODUCT),
	WRONG_DISEASE_TARGET("Only SarsCov2 is a valid disease target", EvalErrorCodes.WRONG_DISEASE_TARGET),
	WRONG_TEST_TYPE("Test type invalid", EvalErrorCodes.WRONG_TEST_TYPE),
	POSITIVE_RESULT("Test result was positive", EvalErrorCodes.POSITIVE_RESULT),
	NOT_FULLY_PROTECTED("Missing vaccine shots, only partially protected", EvalErrorCodes.NOT_FULLY_PROTECTED)
}