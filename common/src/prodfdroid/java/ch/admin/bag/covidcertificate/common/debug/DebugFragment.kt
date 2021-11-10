package ch.admin.bag.covidcertificate.common.debug
/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

import android.content.Context
import androidx.fragment.app.Fragment

open class DebugFragment : Fragment() {

	companion object {
		fun newInstance(): DebugFragment = DebugFragment()

		const val EXISTS = false

		fun initDebug(context: Context) {}
	}

}