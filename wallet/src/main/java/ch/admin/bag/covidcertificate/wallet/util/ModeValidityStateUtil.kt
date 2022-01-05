/*
 * Copyright (c) 2022 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.util

import ch.admin.bag.covidcertificate.sdk.core.models.state.ModeValidityState

fun ModeValidityState.isValid() = this == ModeValidityState.SUCCESS
fun ModeValidityState.isInvalid() = this == ModeValidityState.INVALID
fun ModeValidityState.isPartiallyValid() = this in setOf(ModeValidityState.SUCCESS_2G, ModeValidityState.SUCCESS_2G_PLUS)
fun ModeValidityState.isLight() = this == ModeValidityState.IS_LIGHT
fun ModeValidityState.isUnknown() = this in setOf(ModeValidityState.UNKNOWN_MODE, ModeValidityState.UNKNOWN)