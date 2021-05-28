/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.common.config

import android.os.Parcel
import android.os.Parcelable
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class InfoBoxModel(
	val title: String,
	val msg: String,
	val url: String?,
	val urlTitle: String?,
	val isDismissible: Boolean,
	val infoId: Long
) : Parcelable {

	constructor(parcel: Parcel) : this(
		parcel.readString() ?: throw IllegalArgumentException("No title specified for InfoDialog"),
		parcel.readString() ?: throw IllegalArgumentException("No message specified for InfoDialog"),
		parcel.readString(),
		parcel.readString(),
		parcel.readByte() != 0.toByte(),
		parcel.readLong()
	) {
	}

	override fun writeToParcel(parcel: Parcel, flags: Int) {
		parcel.writeString(title)
		parcel.writeString(msg)
		parcel.writeString(url)
		parcel.writeString(urlTitle)
		parcel.writeByte(if (isDismissible) 1 else 0)
		parcel.writeLong(infoId)
	}

	override fun describeContents(): Int {
		return 0
	}

	companion object CREATOR : Parcelable.Creator<InfoBoxModel> {
		override fun createFromParcel(parcel: Parcel): InfoBoxModel {
			return InfoBoxModel(parcel)
		}

		override fun newArray(size: Int): Array<InfoBoxModel?> {
			return arrayOfNulls(size)
		}
	}
}