package ch.admin.bag.covidcertificate.common.config

import android.os.Parcel
import android.os.Parcelable
import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
data class InfoCovidCertificateNews(
	val title: String?,
	val newsItems: List<CovidCertificateNewsItem>
) : Serializable