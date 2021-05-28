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

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class RuleSet(
	val rules: List<Rule>
)

@Serializable
@JsonClass(generateAdapter = true)
data class Rule(
	val name: String,
	val description: String,
	val expression: Expression
)

@Serializable
@JsonClass(generateAdapter = true)
data class Expression(
	val version: String,
	val type: String,
	val value: String
)


