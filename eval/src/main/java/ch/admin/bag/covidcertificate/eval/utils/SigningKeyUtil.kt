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

import ch.admin.bag.covidcertificate.eval.models.Jwk
import ch.admin.bag.covidcertificate.verifier.eval.BuildConfig

internal fun getHardcodedBagSigningKeys(): List<Jwk> {
	val kid = BuildConfig.SIGNING_KEY_KID
	val n = BuildConfig.SIGNING_KEY_N
	val e = BuildConfig.SIGNING_KEY_E

	return listOf(
		Jwk.fromNE(kid, n, e, use = "")
	)
}