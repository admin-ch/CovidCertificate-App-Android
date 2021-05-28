/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.products

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ValueSet(
	val valueSetId: String,
	val valueSetDate: String,
	val valueSetValues: Map<String, ValueEntry>,
)

@JsonClass(generateAdapter = true)
data class ValueEntry(
	val display: String,
	val lang: String,
	val active: Boolean,
	val system: String,
	val version: String,
)
