/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.common.net

import ch.admin.bag.covidcertificate.common.config.ConfigModel
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface ConfigService {

	@Headers("Accept: application/json")
	@GET("config")
	suspend fun getConfig(
		@Query("appversion") appVersion: String,
		@Query("osversion") osVersion: String,
		@Query("buildnr") buildNumber: String
	): Response<ConfigModel>

}