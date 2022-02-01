/*
 * Copyright (c) 2022 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.common.extensions

import android.content.Context
import android.content.res.Configuration
import java.util.*

fun Context.getDrawableIdentifier(drawableName: String) = resources.getIdentifier(drawableName, "drawable", packageName)

fun Context.updateLocale(languageKey: String? = null): Context {
	val config = Configuration()

	val locale = languageKey?.let {
		Locale(languageKey, "CH")
	} ?: Locale.getDefault()

	config.setLocale(locale)
	return createConfigurationContext(config)
}