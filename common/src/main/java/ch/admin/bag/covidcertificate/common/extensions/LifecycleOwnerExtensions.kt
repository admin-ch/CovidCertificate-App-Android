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

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Collect a flow, starting and cancelling the collection automatically when the lifecycle changes from or to STARTED
 */
fun <T> LifecycleOwner.collectWhenStarted(flow: Flow<T>, block: suspend (T) -> Unit) {
	lifecycleScope.launch {
		flow.flowWithLifecycle(lifecycle).collect {
			block(it)
		}
	}
}