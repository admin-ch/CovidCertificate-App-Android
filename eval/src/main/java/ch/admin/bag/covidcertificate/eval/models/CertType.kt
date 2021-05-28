/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.models

/**
 * The type of health certificate.
 *
 * These are explicitly mutually exclusive: a certificate must not contain both a vaccination and a test statement.
 */
enum class CertType(val use: String) {
	VACCINATION("v"),
	TEST("t"),
	RECOVERY("r"),
}