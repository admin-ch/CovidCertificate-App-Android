package ch.admin.bag.covidcertificate.common.aws

import android.graphics.Bitmap
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AWSBody(
	val image: String
)
