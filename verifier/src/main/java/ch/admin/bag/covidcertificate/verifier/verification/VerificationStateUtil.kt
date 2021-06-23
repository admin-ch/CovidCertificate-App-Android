/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.verifier.verification

import android.content.Context
import android.text.SpannableString
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import ch.admin.bag.covidcertificate.common.R
import ch.admin.bag.covidcertificate.common.util.makeBold
import ch.admin.bag.covidcertificate.common.util.makeSubStringBold
import ch.admin.bag.covidcertificate.eval.data.ErrorCodes
import ch.admin.bag.covidcertificate.eval.data.state.CheckNationalRulesState
import ch.admin.bag.covidcertificate.eval.data.state.CheckRevocationState
import ch.admin.bag.covidcertificate.eval.data.state.CheckSignatureState
import ch.admin.bag.covidcertificate.eval.data.state.VerificationState

/**
 * The verification state indicates an offline mode if it is an ERROR and the error code is set to GENERAL_OFFLINE (G|OFF)
 */
fun VerificationState.isOfflineMode() = this is VerificationState.ERROR && this.error.code == ErrorCodes.GENERAL_OFFLINE

fun VerificationState.getVerificationStateItems(context: Context) : List<VerificationItem> {
	val items = mutableListOf<VerificationItem>()

	val isLoading = this == VerificationState.LOADING

	val statusString = getValidationStatusStrings(context)
	val statusIcons = getValidationStatusIcons()
	val statusBubbleColors = getStatusBubbleColors()
	val numStatus = listOf(statusString.size, statusIcons.size, statusBubbleColors.size).minOrNull() ?: 0

	for (i in 0 until numStatus) {
		items.add(StatusItem(statusString[i], statusIcons[i], statusBubbleColors[i], isLoading))
	}

	if (!isLoading && this !is VerificationState.INVALID) {
		val showRetry = this is VerificationState.ERROR
		items.add(InfoItem(getStatusInformationString(context), getInfoIconColor(), getStatusInformationBubbleColor(), showRetry))
	}

	return items;
}

fun VerificationState.getValidationStatusStrings(context: Context): List<SpannableString> {
	return when (this) {
		is VerificationState.SUCCESS -> listOf(context.getString(R.string.verifier_verify_success_title).makeBold())
		is VerificationState.ERROR -> {
			listOf(
				if (isOfflineMode()) {
					context.getString(R.string.verifier_offline_error_title).makeBold()
				} else {
					context.getString(R.string.verifier_verify_error_list_title).makeBold()
				}
			)
		}
		is VerificationState.INVALID -> {
			val stateStrings = mutableListOf<SpannableString>()
			if (this.signatureState is CheckSignatureState.SUCCESS) {
				stateStrings.add(SpannableString(context.getString(R.string.verifier_verify_success_info_for_certificate_valid)))

				if (this.revocationState is CheckRevocationState.SUCCESS) {
					stateStrings.add(SpannableString(context.getString(R.string.verifier_verify_success_info_for_blacklist)))

					when (this.nationalRulesState) {
						is CheckNationalRulesState.INVALID -> {
							stateStrings.add(context.getString(R.string.verifier_verify_error_info_for_national_rules).makeBold())
						}
						is CheckNationalRulesState.NOT_VALID_ANYMORE -> {
							stateStrings.add(context.getString(R.string.verifier_verifiy_error_expired).makeSubStringBold(context.getString(R.string.verifier_verify_error_validity_range_bold)))
						}
						is CheckNationalRulesState.NOT_YET_VALID -> {
							stateStrings.add(context.getString(R.string.verifier_verifiy_error_notyetvalid).makeSubStringBold(context.getString(R.string.verifier_verify_error_validity_range_bold)))
						}
					}
				} else {
					stateStrings.add(context.getString(R.string.verifier_verify_error_info_for_blacklist).makeBold())
				}
			} else {
				stateStrings.add(context.getString(R.string.verifier_verify_error_info_for_certificate_invalid).makeBold())
			}

			stateStrings
		}
		VerificationState.LOADING -> listOf(SpannableString(context.getString(R.string.wallet_certificate_verifying)))
	}
}

fun VerificationState.getStatusInformationString(context: Context): String {
	return when (this) {
		is VerificationState.ERROR -> {
			if (isOfflineMode()) {
				context.getString(R.string.verifier_offline_error_text)
			} else {
				context.getString(R.string.verifier_verify_error_list_info_text)
			}
		}
		is VerificationState.INVALID -> ""
		VerificationState.LOADING -> context.getString(R.string.wallet_certificate_verifying)
		is VerificationState.SUCCESS -> context.getString(R.string.verifier_verify_success_info)
	}
}

@DrawableRes
fun VerificationState.getValidationStatusIcons(): List<Int> {
	return when (this) {
		is VerificationState.ERROR -> {
			listOf(
				if (isOfflineMode()) {
					R.drawable.ic_no_connection
				} else {
					R.drawable.ic_process_error
				}
			)
		}
		is VerificationState.INVALID -> {
			val stateIcons = mutableListOf<Int>()
			if (this.signatureState is CheckSignatureState.SUCCESS) {
				stateIcons.add(R.drawable.ic_privacy_grey)
				if (this.revocationState is CheckRevocationState.SUCCESS) {
					stateIcons.add(R.drawable.ic_check_grey)
					stateIcons.add(R.drawable.ic_error)
				} else {
					stateIcons.add(R.drawable.ic_error)
				}
			} else {
				stateIcons.add(R.drawable.ic_error)
			}
			stateIcons
		}
		is VerificationState.SUCCESS -> listOf(R.drawable.ic_check_green)
		VerificationState.LOADING -> listOf(0)
	}
}

@DrawableRes
fun VerificationState.getValidationStatusIconLarge(): Int {
	return when (this) {
		is VerificationState.ERROR -> {
			if (isOfflineMode()) {
				R.drawable.ic_no_connection_large
			} else {
				R.drawable.ic_process_error_large
			}
		}
		is VerificationState.INVALID -> R.drawable.ic_error_large
		VerificationState.LOADING -> 0
		is VerificationState.SUCCESS -> R.drawable.ic_check_large
	}
}

@ColorRes
fun VerificationState.getStatusBubbleColors(): List<Int> {
	return when (this) {
		is VerificationState.ERROR -> listOf(R.color.orangeish)
		is VerificationState.INVALID -> {
			val stateColors = mutableListOf<Int>()
			if (this.signatureState is CheckSignatureState.SUCCESS) {
				stateColors.add(R.color.greyish)
				if (this.revocationState is CheckRevocationState.SUCCESS) {
					stateColors.add(R.color.greyish)
					stateColors.add(R.color.redish)
				} else {
					stateColors.add(R.color.redish)
				}
			} else {
				stateColors.add(R.color.redish)
			}
			stateColors
		}
		VerificationState.LOADING -> listOf(R.color.greyish)
		is VerificationState.SUCCESS -> listOf(R.color.greenish)
	}
}

@ColorRes
fun VerificationState.getStatusInformationBubbleColor(): Int {
	return when (this) {
		is VerificationState.ERROR -> R.color.orangeish
		is VerificationState.INVALID -> R.color.redish
		VerificationState.LOADING -> R.color.greyish
		is VerificationState.SUCCESS -> R.color.blueish
	}
}

@ColorRes
fun VerificationState.getInfoIconColor(): Int {
	return when (this) {
		is VerificationState.ERROR -> R.color.orange
		is VerificationState.INVALID -> R.color.red
		VerificationState.LOADING -> R.color.grey
		is VerificationState.SUCCESS -> R.color.blue
	}
}

@ColorRes
fun VerificationState.getHeaderColor(): Int {
	return when (this) {
		is VerificationState.ERROR -> R.color.orange
		is VerificationState.INVALID -> R.color.red
		VerificationState.LOADING -> R.color.grey
		is VerificationState.SUCCESS -> R.color.green
	}
}