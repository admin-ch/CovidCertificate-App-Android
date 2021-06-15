/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.transfercode

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import ch.admin.bag.covidcertificate.wallet.databinding.ViewTransferCodeBinding

class TransferCodeView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

	companion object {
		private const val TRANSFER_CODE_SECTION_COUNT = 3
		private const val TRANSFER_CODE_SECTION_LENGTH = 3
	}

	private val binding = ViewTransferCodeBinding.inflate(LayoutInflater.from(context), this)

	init {
		orientation = HORIZONTAL
		gravity = Gravity.CENTER

		if (isInEditMode) {
			setTransferCode("A2X56K7WP")
		}
	}

	fun setTransferCode(code: String) {
		val sections = code.chunked(TRANSFER_CODE_SECTION_LENGTH)
			.take(TRANSFER_CODE_SECTION_COUNT)

		binding.transferCodeSection1.text = sections[0]
		binding.transferCodeSection2.text = sections[1]
		binding.transferCodeSection3.text = sections[2]
	}

}