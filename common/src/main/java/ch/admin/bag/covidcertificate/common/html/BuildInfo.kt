package ch.admin.bag.covidcertificate.common.html

import java.io.Serializable

data class BuildInfo(
	val appName: String,
	val versionName: String,
	val buildTime: Long,
	val flavor: String,
	val agbUrl: String
) : Serializable
