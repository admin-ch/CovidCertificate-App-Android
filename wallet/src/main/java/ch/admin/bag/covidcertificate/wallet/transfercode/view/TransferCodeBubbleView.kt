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

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.ColorRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import ch.admin.bag.covidcertificate.common.util.makeSubStringBold
import ch.admin.bag.covidcertificate.sdk.android.extensions.DEFAULT_DISPLAY_DATE_TIME_FORMATTER
import ch.admin.bag.covidcertificate.sdk.android.extensions.prettyPrint
import ch.admin.bag.covidcertificate.sdk.core.data.ErrorCodes
import ch.admin.bag.covidcertificate.sdk.core.models.state.StateError
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.databinding.ViewTransferCodeBubbleBinding
import ch.admin.bag.covidcertificate.wallet.transfercode.model.TransferCodeModel
import java.time.Instant

class TransferCodeBubbleView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

	companion object {
		private const val DATE_REPLACEMENT_STRING = "{DATE}"
		private const val DAYS_REPLACEMENT_STRING = "{DAYS}"
	}

	private val binding = ViewTransferCodeBubbleBinding.inflate(LayoutInflater.from(context), this)
	private val validityIcons = arrayOf(
		R.drawable.ic_expire_1,
		R.drawable.ic_expire_2,
		R.drawable.ic_expire_3,
		R.drawable.ic_expire_4,
		R.drawable.ic_expire_5,
		R.drawable.ic_expire_6,
		R.drawable.ic_expire_7,
	)

	private var viewState: TransferCodeBubbleState? = null
	private var transferCode: TransferCodeModel? = null

	init {
		clipToPadding = false
		clipToOutline = false
		setPaddingRelative(0, context.resources.getDimensionPixelSize(R.dimen.spacing_medium_large), 0, 0)

		if (isInEditMode) {
			setTransferCode(TransferCodeModel("A2X56K7WP", Instant.now(), Instant.now()))
			setState(TransferCodeBubbleState.Valid(false))
		}
	}

	fun setTransferCode(transferCode: TransferCodeModel) {
		this.transferCode = transferCode
		updateView()
	}

	fun setState(newState: TransferCodeBubbleState) {
		viewState = newState
		updateView()
	}

	private fun updateView() {
		when (val viewState = viewState) {
			is TransferCodeBubbleState.Created -> showCreatedState()
			is TransferCodeBubbleState.Valid -> showValidState(viewState)
			is TransferCodeBubbleState.Expired -> showExpiredState(viewState)
			is TransferCodeBubbleState.Error -> showErrorState(viewState)
		}
	}

	private fun showCreatedState() {
		val transferCode = transferCode ?: return

		binding.transferCodeErrorIcon.isVisible = false
		binding.transferCodeStatusIcon.isVisible = true
		binding.transferCodeLoadingIndicator.isVisible = false
		binding.transferCodeValidity.isVisible = false
		binding.transferCodeTitle.isVisible = true
		binding.transferCodeValue.isVisible = true
		binding.transferCodeExpired.isVisible = false
		binding.transferCodeCreationDatetime.isVisible = true
		binding.transferCodeErrorTitle.isVisible = false
		binding.transferCodeErrorDescription.isVisible = false

		binding.transferCodeStatusIcon.setImageResource(R.drawable.ic_check_mark)
		binding.transferCodeValue.setTransferCode(transferCode.code)

		setIconAndTextColor(R.color.blue)
		setBubbleBackgroundColor(R.color.blueish)
		showCreationTimestamp()
	}

	private fun showValidState(state: TransferCodeBubbleState.Valid) {
		val transferCode = transferCode ?: return

		binding.transferCodeStatusIcon.isVisible = !state.isRefreshing
		binding.transferCodeLoadingIndicator.isVisible = state.isRefreshing
		binding.transferCodeValidity.isVisible = true
		binding.transferCodeTitle.isVisible = false
		binding.transferCodeValue.isVisible = true
		binding.transferCodeExpired.isVisible = false
		binding.transferCodeCreationDatetime.isVisible = true
		binding.transferCodeErrorTitle.isVisible = false
		binding.transferCodeErrorDescription.isVisible = false

		val daysUntilExpiration = transferCode.getDaysUntilExpiration()
		if (daysUntilExpiration > 1) {
			val boldPart = context.getString(R.string.wallet_transfer_code_expire_plural_bold)
				.replace(DAYS_REPLACEMENT_STRING, daysUntilExpiration.toString())
			binding.transferCodeValidity.text = context.getString(R.string.wallet_transfer_code_expire_plural)
				.replace(DAYS_REPLACEMENT_STRING, daysUntilExpiration.toString())
				.makeSubStringBold(boldPart)
		} else {
			binding.transferCodeValidity.text = context.getString(R.string.wallet_transfer_code_expire_singular)
				.makeSubStringBold(context.getString(R.string.wallet_transfer_code_expire_singular_bold))
		}

		if (!state.isRefreshing) {
			val imageIndex = (daysUntilExpiration - 1).coerceAtLeast(0)
			binding.transferCodeStatusIcon.setImageResource(validityIcons[imageIndex])
		}

		state.error?.let {
			if (it.code == ErrorCodes.GENERAL_OFFLINE) {
				binding.transferCodeErrorIcon.setImageResource(R.drawable.ic_corner_offline)
			} else {
				binding.transferCodeErrorIcon.setImageResource(R.drawable.ic_corner_process_error)
			}
		}
		binding.transferCodeErrorIcon.isVisible = state.error != null

		binding.transferCodeValue.setTransferCode(transferCode.code)

		setIconAndTextColor(R.color.blue)
		setBubbleBackgroundColor(R.color.blueish)
		showCreationTimestamp()
	}

	private fun showExpiredState(state: TransferCodeBubbleState.Expired) {
		binding.transferCodeErrorIcon.isVisible = false
		binding.transferCodeStatusIcon.isVisible = true
		binding.transferCodeLoadingIndicator.isVisible = false
		binding.transferCodeValidity.isVisible = false
		binding.transferCodeTitle.isVisible = false
		binding.transferCodeValue.isVisible = false
		binding.transferCodeExpired.isVisible = true
		binding.transferCodeCreationDatetime.isVisible = true
		binding.transferCodeErrorTitle.isVisible = false
		binding.transferCodeErrorDescription.isVisible = false

		binding.transferCodeStatusIcon.setImageResource(R.drawable.ic_info_outline)

		if (!state.isHighlighted) {
			// Show errors if the transfer code is not highlighted (failed)
			state.error?.let {
				binding.transferCodeErrorIcon.isVisible = true
				if (it.code == ErrorCodes.GENERAL_OFFLINE) {
					binding.transferCodeErrorIcon.setImageResource(R.drawable.ic_corner_offline)
				} else {
					binding.transferCodeErrorIcon.setImageResource(R.drawable.ic_corner_process_error)
				}
			}
		}

		setIconAndTextColor(if (state.isHighlighted) R.color.red else R.color.blue)
		setBubbleBackgroundColor(if (state.isHighlighted) R.color.redish else R.color.blueish)
		showCreationTimestamp()
	}

	private fun showErrorState(state: TransferCodeBubbleState.Error) {
		binding.transferCodeErrorIcon.isVisible = false
		binding.transferCodeStatusIcon.isVisible = true
		binding.transferCodeLoadingIndicator.isVisible = false
		binding.transferCodeValidity.isVisible = false
		binding.transferCodeTitle.isVisible = false
		binding.transferCodeValue.isVisible = false
		binding.transferCodeExpired.isVisible = false
		binding.transferCodeCreationDatetime.isVisible = false
		binding.transferCodeErrorTitle.isVisible = true
		binding.transferCodeErrorDescription.isVisible = true
		binding.transferCodeTransferCode.isVisible = false
		binding.transferBubbleCodeErrorCode.isVisible = false

		if (state.error.code == ErrorCodes.GENERAL_OFFLINE) {
			binding.transferCodeStatusIcon.setImageResource(R.drawable.ic_no_connection)
			binding.transferCodeErrorTitle.setText(R.string.wallet_transfer_code_no_internet_title)
			binding.transferCodeErrorDescription.setText(R.string.wallet_transfer_code_generate_no_internet_error_text)
			setIconAndTextColor(R.color.orange)
			setBubbleBackgroundColor(R.color.orangeish)
		} else if (state.error.code == ErrorCodes.INAPP_DELIVERY_KEYPAIR_GENERATION_FAILED) {
			binding.transferCodeStatusIcon.setImageResource(R.drawable.ic_scanner_alert)
			binding.transferCodeErrorTitle.setText(R.string.wallet_transfer_code_unexpected_error_text)
			binding.transferCodeErrorDescription.setText(R.string.wallet_transfer_code_unexpected_error_phone_number)
			binding.transferCodeErrorDescription.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_call, 0, 0, 0)
			binding.transferCodeErrorDescription.setTypeface(binding.transferCodeErrorDescription.typeface,Typeface.BOLD)
			binding.transferCodeErrorDescription.setTextColor(context.getColor(R.color.red))
			transferCode?.let { code ->
				val text = context.getString(R.string.wallet_transfer_code_title) + ": ${code.code}"
				binding.transferCodeTransferCode.text = text
				binding.transferCodeTransferCode.isVisible = true
			}
			binding.transferBubbleCodeErrorCode.text = state.error.code
			binding.transferBubbleCodeErrorCode.isVisible = true
			setIconAndTextColor(R.color.red)
			setBubbleBackgroundColor(R.color.redish)

		} else {
			binding.transferCodeStatusIcon.setImageResource(R.drawable.ic_process_error)
			binding.transferCodeErrorTitle.setText(R.string.verifier_network_error_text)
			binding.transferCodeErrorDescription.setText(R.string.wallet_detail_network_error_text)
			setIconAndTextColor(R.color.orange)
			setBubbleBackgroundColor(R.color.orangeish)
		}


	}

	private fun setIconAndTextColor(@ColorRes colorId: Int) {
		val color = ContextCompat.getColor(context, colorId)
		binding.transferCodeStatusIcon.imageTintList = ColorStateList.valueOf(color)
		binding.transferCodeExpired.setTextColor(color)
	}

	private fun setBubbleBackgroundColor(@ColorRes colorId: Int) {
		val color = ContextCompat.getColor(context, colorId)
		binding.background.backgroundTintList = ColorStateList.valueOf(color)
	}

	private fun showCreationTimestamp() {
		transferCode?.let {
			binding.transferCodeCreationDatetime.text = context.getString(R.string.wallet_transfer_code_createdat)
				.replace(DATE_REPLACEMENT_STRING, it.creationTimestamp.prettyPrint(DEFAULT_DISPLAY_DATE_TIME_FORMATTER))
		}
	}

	sealed class TransferCodeBubbleState {
		object Created : TransferCodeBubbleState()
		data class Valid(
			val isRefreshing: Boolean,
			val error: StateError? = null,
		) : TransferCodeBubbleState()

		data class Expired(
			val isHighlighted: Boolean,
			val error: StateError? = null,
		) : TransferCodeBubbleState()

		data class Error(val error: StateError) : TransferCodeBubbleState()
	}

}