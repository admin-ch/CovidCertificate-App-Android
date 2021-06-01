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

object AcceptanceCriterias {

	const val NEGATIVE_CODE: String = "260415000"
	const val TARGET_DISEASE = "840539006"
	const val PCR_TEST_VALIDITY_IN_HOURS = 72L
	const val RAT_TEST_VALIDITY_IN_HOURS = 24L
	const val SINGLE_VACCINE_VALIDITY_OFFSET_IN_DAYS = 15L
	const val VACCINE_IMMUNITY_DURATION_IN_DAYS = 179L
	const val RECOVERY_OFFSET_VALID_UNTIL_DAYS = 179L
	const val RECOVERY_OFFSET_VALID_FROM_DAYS = 10L
}

enum class TestType(val code: String) {
	RAT("LP217198-3"),
	PCR("LP6464-4")
}
