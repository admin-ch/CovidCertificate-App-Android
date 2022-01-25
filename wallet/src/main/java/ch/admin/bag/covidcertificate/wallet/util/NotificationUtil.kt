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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ch.admin.bag.covidcertificate.wallet.MainActivity
import ch.admin.bag.covidcertificate.wallet.R

object NotificationUtil {

	private const val WALLET_TRANSFER_NOTIFICATION_ID = 0
	private const val WALLET_TRANSFER_NOTIFICATION_CHANNEL_ID = "wallet_transfer_notification_channel"

	fun createTransferNotificationChannel(context: Context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val name = context.getString(R.string.wallet_transfer_code_title)
			val importance = NotificationManager.IMPORTANCE_DEFAULT
			val channel = NotificationChannel(WALLET_TRANSFER_NOTIFICATION_CHANNEL_ID, name, importance)
			val notificationManager: NotificationManager =
				context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
			notificationManager.createNotificationChannel(channel)
		}
	}

	fun showTransferSuccessNotification(context: Context) {
		val intent = Intent(context, MainActivity::class.java).apply {
			flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
		}
		val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

		val notificationText = context.getString(R.string.wallet_notification_transfer_text)
		val notificationBuilder = NotificationCompat.Builder(context, WALLET_TRANSFER_NOTIFICATION_CHANNEL_ID)
			.setSmallIcon(R.drawable.ic_transfer_notification)
			.setColor(ContextCompat.getColor(context, R.color.blue))
			.setContentTitle(context.getString(R.string.wallet_notification_transfer_title))
			.setContentText(notificationText)
			.setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
			.setContentIntent(pendingIntent)
			.setAutoCancel(true)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)

		with(NotificationManagerCompat.from(context)) {
			notify(WALLET_TRANSFER_NOTIFICATION_ID, notificationBuilder.build())
		}
	}

}