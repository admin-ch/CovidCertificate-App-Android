/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.common.util

import ch.admin.bag.covidcertificate.common.BuildConfig
import ch.admin.bag.covidcertificate.sdk.android.SdkEnvironment

object EnvironmentUtil {

	fun getSdkEnvironment(flavor: String = BuildConfig.FLAVOR) = when (flavor) {
		"dev" -> SdkEnvironment.DEV
		"abn" -> SdkEnvironment.ABN
		"prod" -> SdkEnvironment.PROD
		"prodfdroid" -> SdkEnvironment.PROD
		else -> throw IllegalArgumentException("Unknown environment $flavor")
	}

}