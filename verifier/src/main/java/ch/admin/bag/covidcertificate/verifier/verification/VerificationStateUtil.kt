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
import ch.admin.bag.covidcertificate.sdk.core.data.ErrorCodes
import ch.admin.bag.covidcertificate.sdk.core.models.state.*

/**
 * The verification state indicates an offline mode if it is an ERROR and the error code is set to GENERAL_OFFLINE (G|OFF)
 */
fun VerificationState.isOfflineMode() = this is VerificationState.ERROR && this.error.code == ErrorCodes.GENERAL_OFFLINE

/**
 * @return A list of 0-n [StatusItem] and optionally one or none [InfoItem]
 */
fun VerificationState.getVerificationStateItems(context: Context, modeTitle: String): List<VerificationItem> {
	val items = mutableListOf<VerificationItem>()

	val isLoading = this == VerificationState.LOADING

	// Get a list of status bubbles based on the verification state
	val statusString = getValidationStatusStrings(context, modeTitle)
	val statusIcons = getValidationStatusIcons()
	val statusBubbleColors = getStatusBubbleColors()
	val numStatus = listOf(statusString.size, statusIcons.size, statusBubbleColors.size).minOrNull() ?: 0

	for (i in 0 until numStatus) {
		items.add(StatusItem(statusString[i], statusIcons[i], statusBubbleColors[i], isLoading))
	}

	if (!isLoading) {
		// Add an additional info status bubble if it's not a loading state
		val statusInfoText = getStatusInformationString(context)
		if (statusInfoText != null) {
			val showRetry = this is VerificationState.ERROR
			items.add(InfoItem(statusInfoText, getInfoIconColor(), getStatusInformationBubbleColor(), showRetry))
		}
	}

	return items
}

/**
 * @return A list of spannable strings shown in the status bubbles (each list entry is shown in a separate bubble)
 */
fun VerificationState.getValidationStatusStrings(context: Context, modeTitle:String): List<SpannableString> {
	return when (this) {
		is VerificationState.SUCCESS -> when (getModeValidity()) {
			ModeValidityState.SUCCESS -> listOf(context.getString(R.string.verifier_verify_success_title).makeBold())
			ModeValidityState.IS_LIGHT -> listOf(context.getString(R.string.verifier_verify_light_not_supported_by_mode_title).replace("{MODUS}", modeTitle).makeBold())
			ModeValidityState.INVALID -> listOf(
				SpannableString(context.getString(R.string.verifier_verify_success_info_for_certificate_valid)),
				SpannableString(context.getString(R.string.verifier_verify_success_info_for_blacklist)),
				context.getString(R.string.verifier_verify_error_info_for_national_rules).replace("{MODUS}", modeTitle).makeBold()
			)
			ModeValidityState.TWO_G -> listOf(
				"Gültiges Covid-Zertifikat nach 2G-Regelung".makeBold(),
				SpannableString("Für 2G+ nur in Kombination mit einem gültigen PCR- oder Antigentest zugelassen.")
			) // TODO Use string resources
			ModeValidityState.PLUS -> listOf(
				"Gültiges Covid-Zertifikat für Getestete.".makeBold(),
				SpannableString("Für 2G+ nur in Kombination mit einem gültigen Covid-Zertifikat für Geimpfte oder Genesene zugelassen.")
			) // TODO Use string resources
			else -> listOf(context.getString(R.string.verifier_verify_error_list_title).makeBold())
		}
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
			val signatureState = this.signatureState
			if (signatureState is CheckSignatureState.SUCCESS) {
				stateStrings.add(SpannableString(context.getString(R.string.verifier_verify_success_info_for_certificate_valid)))

				if (this.revocationState is CheckRevocationState.SUCCESS) {
					stateStrings.add(SpannableString(context.getString(R.string.verifier_verify_success_info_for_blacklist)))

					when (this.nationalRulesState) {
						is CheckNationalRulesState.INVALID,
						is CheckNationalRulesState.NOT_VALID_ANYMORE,
						is CheckNationalRulesState.NOT_YET_VALID -> {
							stateStrings.add(context.getString(R.string.verifier_verify_error_info_for_national_rules).replace("{MODUS}", modeTitle).makeBold())
						}
					}
				} else {
					stateStrings.add(context.getString(R.string.verifier_verify_error_info_for_blacklist).makeBold())
				}
			} else {
				if (signatureState is CheckSignatureState.INVALID
					&& signatureState.signatureErrorCode == ErrorCodes.SIGNATURE_TIMESTAMP_EXPIRED
				) {
					stateStrings.add(context.getString(R.string.verifier_certificate_light_error_expired).makeBold())
				} else {
					stateStrings.add(context.getString(R.string.verifier_verify_error_info_for_certificate_invalid).makeBold())
				}
			}

			stateStrings
		}
		VerificationState.LOADING -> listOf(SpannableString(context.getString(R.string.wallet_certificate_verifying)))
	}
}

/**
 * @return The string to be shown in the info status bubble
 */
fun VerificationState.getStatusInformationString(context: Context): String? {
	return when (this) {
		is VerificationState.ERROR -> {
			if (isOfflineMode()) {
				context.getString(R.string.verifier_offline_error_text)
			} else {
				context.getString(R.string.verifier_verify_error_list_info_text)
			}
		}
		is VerificationState.INVALID -> null
		VerificationState.LOADING -> context.getString(R.string.wallet_certificate_verifying)
		is VerificationState.SUCCESS -> when (getModeValidity()) {
			ModeValidityState.SUCCESS -> if (this.isLightCertificate) {
				context.getString(R.string.verifier_verify_success_certificate_light_info)
			} else {
				context.getString(R.string.verifier_verify_success_info)
			}
			ModeValidityState.IS_LIGHT -> context.getString(R.string.verifier_verify_light_not_supported_by_mode_text)
			ModeValidityState.INVALID -> null
			ModeValidityState.TWO_G -> null
			ModeValidityState.PLUS -> null
			else -> context.getString(R.string.verifier_verify_error_list_info_text)
		}
	}
}

/**
 * @return A list of drawable resource IDs shown in the status bubbles (each list entry is shown in a separate bubble)
 */
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
		is VerificationState.SUCCESS -> when (getModeValidity()) {
			ModeValidityState.SUCCESS -> listOf(R.drawable.ic_check_green)
			ModeValidityState.IS_LIGHT -> listOf(R.drawable.ic_process_error)
			ModeValidityState.INVALID -> listOf(
				R.drawable.ic_privacy_grey,
				R.drawable.ic_check_grey,
				R.drawable.ic_error
			)
			ModeValidityState.TWO_G -> listOf(
				R.drawable.ic_2g_green,
				R.drawable.ic_plus
			)
			ModeValidityState.PLUS -> listOf(
				R.drawable.ic_plus_green,
				R.drawable.ic_2g_grey
			)
			else -> listOf(R.drawable.ic_error)
		}
		VerificationState.LOADING -> listOf(0)
	}
}

/**
 * @return A list of drawable resource IDs that correspond to the large verification state icon in the header
 */
fun VerificationState.getValidationStatusIconsLarge(): List<Int> {
	return when (this) {
		is VerificationState.ERROR -> {
			if (isOfflineMode()) {
				listOf(R.drawable.ic_no_connection_large)
			} else {
				listOf(R.drawable.ic_process_error_large)
			}
		}
		is VerificationState.INVALID -> listOf(R.drawable.ic_error_large)
		is VerificationState.LOADING -> emptyList()
		is VerificationState.SUCCESS -> when (getModeValidity()) {
			ModeValidityState.SUCCESS -> listOf(R.drawable.ic_check_large)
			ModeValidityState.IS_LIGHT -> listOf(R.drawable.ic_process_error_large)
			ModeValidityState.TWO_G -> listOf(
				ch.admin.bag.covidcertificate.verifier.R.drawable.ic_header_2g_on,
				ch.admin.bag.covidcertificate.verifier.R.drawable.ic_header_plus_off,
			)
			ModeValidityState.PLUS -> listOf(
				ch.admin.bag.covidcertificate.verifier.R.drawable.ic_header_2g_off,
				ch.admin.bag.covidcertificate.verifier.R.drawable.ic_header_plus_on,
			)
			else -> listOf(R.drawable.ic_error_large)
		}
	}
}

/**
 * @return A list of color resource IDs for the status bubble backgrounds
 */
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
		is VerificationState.SUCCESS -> when (getModeValidity()) {
			ModeValidityState.SUCCESS -> listOf(R.color.greenish)
			ModeValidityState.IS_LIGHT -> listOf(R.color.orangeish)
			ModeValidityState.INVALID -> listOf(R.color.greyish, R.color.greyish, R.color.redish)
			ModeValidityState.TWO_G, ModeValidityState.PLUS -> listOf(R.color.greenish, R.color.greyish)
			else -> listOf(R.color.redish)
		}
	}
}

/**
 * @return The background color resource ID for the info status bubble
 */
@ColorRes
fun VerificationState.getStatusInformationBubbleColor(): Int {
	return when (this) {
		is VerificationState.ERROR -> R.color.orangeish
		is VerificationState.INVALID -> R.color.redish
		VerificationState.LOADING -> R.color.greyish
		is VerificationState.SUCCESS -> when (getModeValidity()) {
			ModeValidityState.SUCCESS -> R.color.blueish
			ModeValidityState.IS_LIGHT -> R.color.orangeish
			else -> R.color.redish
		}
	}
}

/**
 * @return The color resource ID for the info status bubble icon tint
 */
@ColorRes
fun VerificationState.getInfoIconColor(): Int {
	return when (this) {
		is VerificationState.ERROR -> R.color.orange
		is VerificationState.INVALID -> R.color.red
		VerificationState.LOADING -> R.color.grey
		is VerificationState.SUCCESS -> when (getModeValidity()) {
			ModeValidityState.SUCCESS -> R.color.blue
			ModeValidityState.IS_LIGHT -> R.color.orange
			else -> R.color.red
		}
	}
}

/**
 * @return The header background color
 */
@ColorRes
fun VerificationState.getHeaderColor(): Int {
	return when (this) {
		is VerificationState.ERROR -> R.color.orange
		is VerificationState.INVALID -> R.color.red
		VerificationState.LOADING -> R.color.grey
		is VerificationState.SUCCESS -> {
			when (getModeValidity()) {
				ModeValidityState.SUCCESS, ModeValidityState.TWO_G, ModeValidityState.PLUS -> R.color.green
				ModeValidityState.INVALID -> R.color.red
				ModeValidityState.IS_LIGHT -> R.color.orange
				ModeValidityState.UNKNOWN -> R.color.red_error
				ModeValidityState.UNKNOWN_MODE -> R.color.red_error
			}
		}
	}
}

private fun VerificationState.SUCCESS.getModeValidity() =
	(this.successState as SuccessState.VerifierSuccessState).modeValidity.modeValidityState