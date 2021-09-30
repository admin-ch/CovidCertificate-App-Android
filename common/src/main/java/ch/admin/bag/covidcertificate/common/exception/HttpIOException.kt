/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.common.exception

import retrofit2.Response
import java.io.IOException

/**
 * A replacement for the retrofit2.HttpException that inherits from IOException instead of RuntimeException.
 * Since other request related exceptions (e.g. UnknownHost or SocketTimeout) extend IOException, this makes it easier to catch all
 * networking related exceptions in a single catch-clause.
 */
class HttpIOException(val response: Response<*>) : IOException(response.message()) {
	val code = response.code()
}