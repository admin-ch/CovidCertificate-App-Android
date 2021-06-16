/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.common.faq.model

import java.io.Serializable

interface Faq

data class Header(val iconName: String?, val title: String, val subtitle: String?) : Faq, Serializable

data class Question(
	val question: String,
	val answer: String,
	var isSelected: Boolean = false,
	val linkTitle: String? = null,
	val linkUrl: String? = null,
) : Faq, Serializable

data class IntroSection(val iconName: String, val text: String) : Faq, Serializable