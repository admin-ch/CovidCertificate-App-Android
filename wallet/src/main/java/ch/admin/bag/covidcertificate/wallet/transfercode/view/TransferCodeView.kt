/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.transfercode.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import ch.admin.bag.covidcertificate.wallet.R
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
	private var transferCode: String? = null

	init {
		orientation = HORIZONTAL
		gravity = Gravity.CENTER

		if (isInEditMode) {
			setTransferCode("A2X56K7WP")
		}
	}

	override fun onFinishInflate() {
		super.onFinishInflate()
		setupClickToCopy()
	}

	fun setTransferCode(code: String) {
		this.transferCode = code
		val sections = code.chunked(TRANSFER_CODE_SECTION_LENGTH)
			.take(TRANSFER_CODE_SECTION_COUNT)

		binding.transferCodeSection1.text = sections[0]
		binding.transferCodeSection2.text = sections[1]
		binding.transferCodeSection3.text = sections[2]
	}

	private fun setupClickToCopy() {
		// Set the selectable item ripple drawable as this views foreground
		val outValue = TypedValue()
		context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
		val selectableItemDrawable = ContextCompat.getDrawable(context, outValue.resourceId)
		foreground = selectableItemDrawable

		setOnClickListener {
			transferCode?.let {
				val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
				val clip = ClipData.newPlainText(context.getString(R.string.wallet_transfer_code_title), it)
				clipboardManager.setPrimaryClip(clip)
				Toast.makeText(context, R.string.wallet_transfer_code_copied, Toast.LENGTH_SHORT).show()
			}
		}
	}

}