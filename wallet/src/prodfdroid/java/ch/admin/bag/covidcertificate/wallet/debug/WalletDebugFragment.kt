package ch.admin.bag.covidcertificate.wallet.debug
/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */


import ch.admin.bag.covidcertificate.common.debug.DebugFragment

class WalletDebugFragment : DebugFragment() {
	companion object {
		fun newInstance(): DebugFragment = DebugFragment()
	}
}