package ch.admin.bag.covidcertificate.common.config

import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
data class CovidCertificateNewsItem(
	val iconAndroid: String?,
	val iconIos: String?,
	val text: String?,
) : Serializable
