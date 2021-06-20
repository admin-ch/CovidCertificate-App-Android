/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.transfercode.net

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface DeliveryService {

	@Headers("Accept: application/json")
	@POST("covidcert/register")
	suspend fun register(@Body deliveryRegistration: DeliveryRegistration): Response<Unit>

	@Headers("Accept: application/json+jws")
	@POST("covidcert")
	suspend fun get(@Body requestDeliveryPayload: RequestDeliveryPayload): Response<CovidCertDelivery>

	@Headers("Accept: application/json")
	@POST("covidcert/complete")
	suspend fun complete(@Body requestDeliveryPayload: RequestDeliveryPayload): Response<Unit>
}