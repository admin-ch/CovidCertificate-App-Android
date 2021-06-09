/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.util

import android.content.Context
import android.text.SpannableString
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import ch.admin.bag.covidcertificate.common.util.*
import ch.admin.bag.covidcertificate.common.verification.VerificationState
import ch.admin.bag.covidcertificate.eval.CheckNationalRulesState
import ch.admin.bag.covidcertificate.eval.CheckRevocationState
import ch.admin.bag.covidcertificate.eval.CheckSignatureState
import ch.admin.bag.covidcertificate.eval.models.CertType
import ch.admin.bag.covidcertificate.eval.utils.DEFAULT_DISPLAY_DATE_FORMATTER
import ch.admin.bag.covidcertificate.eval.utils.DEFAULT_DISPLAY_DATE_TIME_FORMATTER
import ch.admin.bag.covidcertificate.wallet.R

const val DATE_REPLACEMENT_STRING = "{DATE}"

fun VerificationState.getStatusString(context: Context): SpannableString {
	return when (this) {
		is VerificationState.SUCCESS -> SpannableString(context.getString(R.string.verifier_verify_success_info))
		else -> getValidationStatusString(context)
	}
}

fun VerificationState.getValidationStatusString(context: Context): SpannableString {
	return when (this) {
		is VerificationState.ERROR -> SpannableString("${context.getString(R.string.verifier_verify_error_list_title)}\n(${this.error.code})"
		)
		is VerificationState.INVALID -> {
			when {
				this.signatureState is CheckSignatureState.INVALID -> {
					context.getString(R.string.wallet_error_invalid_signature)
						.makeSubStringBold(context.getString(R.string.wallet_error_invalid_signature_bold))
				}
				this.revocationState == CheckRevocationState.INVALID -> {
					context.getString(R.string.wallet_error_revocation)
						.makeSubStringBold(context.getString(R.string.wallet_error_revocation_bold))
				}
				this.nationalRulesState is CheckNationalRulesState.NOT_VALID_ANYMORE -> {
					context.getString(R.string.wallet_error_expired)
						.makeSubStringBold(context.getString(R.string.wallet_error_expired_bold))
				}
				this.nationalRulesState is CheckNationalRulesState.NOT_YET_VALID -> {
					context.getString(R.string.wallet_error_valid_from).addBoldDate(
						DATE_REPLACEMENT_STRING,
						(this.nationalRulesState as CheckNationalRulesState.NOT_YET_VALID).validityRange.validFrom!!
					)
				}
				this.nationalRulesState is CheckNationalRulesState.INVALID -> {
					SpannableString(context.getString(R.string.wallet_error_national_rules))
				}
				else -> {
					SpannableString(context.getString(R.string.unknown_error))
				}
			}
		}
		VerificationState.LOADING -> SpannableString(context.getString(R.string.wallet_certificate_verifying))
		is VerificationState.SUCCESS -> context.getString(R.string.wallet_certificate_verify_success).makeBold()
	}
}

fun VerificationState.getValidUntilDateString(certificateType: CertType): String? {
	val dateUntil = when (this) {
		is VerificationState.ERROR -> validityRange?.validUntil
		is VerificationState.INVALID -> validityRange?.validUntil
		is VerificationState.SUCCESS -> validityRange.validUntil
		VerificationState.LOADING -> null
	} ?: return null

	return when (certificateType) {
		CertType.TEST -> DEFAULT_DISPLAY_DATE_TIME_FORMATTER
		else -> DEFAULT_DISPLAY_DATE_FORMATTER
	}.format(dateUntil)
}

@DrawableRes
fun VerificationState.getStatusIcon(): Int {
	return when(this) {
		is VerificationState.ERROR -> R.drawable.ic_process_error
		is VerificationState.INVALID -> {
			when (this.nationalRulesState) {
				is CheckNationalRulesState.NOT_VALID_ANYMORE -> R.drawable.ic_invalid_grey
				is CheckNationalRulesState.NOT_YET_VALID -> R.drawable.ic_timelapse
				else -> R.drawable.ic_error_grey
			}
		}
		VerificationState.LOADING -> 0
		is VerificationState.SUCCESS -> R.drawable.ic_info_blue
	}
}

@DrawableRes
fun VerificationState.getValidationStatusIcon(): Int {
	return when(this) {
		is VerificationState.ERROR -> R.drawable.ic_process_error
		is VerificationState.INVALID -> {
			when (this.nationalRulesState) {
				is CheckNationalRulesState.NOT_VALID_ANYMORE -> R.drawable.ic_invalid_red
				is CheckNationalRulesState.NOT_YET_VALID -> R.drawable.ic_timelapse_red
				else -> R.drawable.ic_error
			}
		}
		VerificationState.LOADING -> 0
		is VerificationState.SUCCESS -> R.drawable.ic_check_green
	}
}

@DrawableRes
fun VerificationState.getValidationStatusIconLarge(): Int {
	return when(this) {
		is VerificationState.ERROR -> R.drawable.ic_process_error_large
		is VerificationState.INVALID -> R.drawable.ic_error_large
		VerificationState.LOADING -> 0
		is VerificationState.SUCCESS -> R.drawable.ic_check_large
	}
}

@ColorRes
fun VerificationState.getInfoBubbleColor(): Int {
	return when(this) {
		is VerificationState.ERROR -> R.color.orangeish
		is VerificationState.INVALID -> {
			if (this.nationalRulesState is CheckNationalRulesState.NOT_VALID_ANYMORE ||
			this.nationalRulesState is CheckNationalRulesState.NOT_YET_VALID) {
				R.color.blueish
			} else {
				R.color.greyish
			}
		}
		VerificationState.LOADING -> R.color.greyish
		is VerificationState.SUCCESS -> R.color.blueish
	}
}

@ColorRes
fun VerificationState.getInfoBubbleValidationColor(): Int {
	return when(this) {
		is VerificationState.ERROR -> R.color.orangeish
		is VerificationState.INVALID -> R.color.redish
		VerificationState.LOADING -> R.color.greyish
		is VerificationState.SUCCESS -> R.color.greenish
	}
}

@ColorRes
fun VerificationState.getSolidValidationColor(): Int {
	return when(this) {
		is VerificationState.ERROR -> R.color.orange
		is VerificationState.INVALID -> R.color.red
		VerificationState.LOADING -> R.color.grey
		is VerificationState.SUCCESS -> R.color.green
	}
}

@ColorRes
fun VerificationState.getNameDobColor(): Int {
	return when (this) {
		is VerificationState.INVALID -> R.color.grey
		else -> R.color.black
	}
}

fun VerificationState.getQrAlpha(): Float {
	return when(this) {
		is VerificationState.INVALID -> 0.55f
		else -> 1f
	}
}
