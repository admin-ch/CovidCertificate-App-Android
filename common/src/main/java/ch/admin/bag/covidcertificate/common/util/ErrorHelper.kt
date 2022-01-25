/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.common.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import ch.admin.bag.covidcertificate.common.R

object ErrorHelper {
	@JvmOverloads
	fun updateErrorView(
		errorView: View,
		errorState: ErrorState,
		customButtonClickAction: Runnable?,
		context: Context?,
		showButton: Boolean = true
	) {
		errorView.findViewById<TextView>(R.id.error_status_title).setText(errorState.titleResId)
		errorView.findViewById<TextView>(R.id.error_status_text).setText(errorState.textResId)
		errorView.findViewById<ImageView>(R.id.error_status_image)
			.setImageDrawable(ContextCompat.getDrawable(errorView.context, errorState.imageResId))

		val buttonView = errorView.findViewById<TextView>(R.id.error_status_button)
		if (showButton) {
			buttonView.visibility = View.VISIBLE
			buttonView.setText(errorState.actionResId)
			buttonView.paintFlags = buttonView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
			buttonView.setOnClickListener { executeErrorAction(errorState, customButtonClickAction, context) }
		} else {
			buttonView.visibility = View.GONE
		}
	}

	private fun executeErrorAction(errorState: ErrorState, customButtonClickAction: Runnable?, context: Context?) {
		customButtonClickAction?.run()
		when (errorState) {
			ErrorState.CAMERA_ACCESS_DENIED -> openApplicationSettings(context)
			else -> {}
		}
	}

	private fun openApplicationSettings(context: Context?) {
		val context = context ?: return
		val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
		val uri = Uri.fromParts("package", context.packageName, null)
		intent.data = uri
		try {
			context.startActivity(intent)
		} catch (e: ActivityNotFoundException) {
			Toast.makeText(context, "Could not open settings", Toast.LENGTH_LONG).show()
		}
	}
}