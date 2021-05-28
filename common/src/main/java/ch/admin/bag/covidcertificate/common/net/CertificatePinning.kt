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

import ch.admin.bag.covidcertificate.common.BuildConfig
import okhttp3.CertificatePinner

object CertificatePinning {

	private val CERTIFICATE_PINNER_DISABLED = CertificatePinner.DEFAULT
	private val CERTIFICATE_PINNER_LIVE = CertificatePinner.Builder()
		.add("www.cc-a.bit.admin.ch", "sha256/++MBgDH5WGvL9Bcn5Be30cRcL0f5O+NyoXuWtQdX1aI=") // root
		.add("www.cc-d.bit.admin.ch", "sha256/++MBgDH5WGvL9Bcn5Be30cRcL0f5O+NyoXuWtQdX1aI=") // root
		.add("www.cc.bit.admin.ch", "sha256/++MBgDH5WGvL9Bcn5Be30cRcL0f5O+NyoXuWtQdX1aI=") // root
		.build()

	val pinner: CertificatePinner
		get() = if (BuildConfig.DEBUG) CERTIFICATE_PINNER_DISABLED else CERTIFICATE_PINNER_LIVE

}