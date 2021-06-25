/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.common.html

import java.io.Serializable

data class BuildInfo(
	val appName: String,
	val versionName: String,
	val buildTime: Long,
	val flavor: String,
	val agbUrl: String,
	val appIdentifier: String,
) : Serializable
