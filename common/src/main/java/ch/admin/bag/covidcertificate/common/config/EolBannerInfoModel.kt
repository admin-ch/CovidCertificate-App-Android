package ch.admin.bag.covidcertificate.common.config

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EolBannerInfoModel(
	val homescreenHexColor: String,
	val homescreenTitle: String,
	val detailHexColor: String,
	val detailTitle: String,
	val detailText: String,
	val detailMoreInfo: String,
	val popupTitle: String,
	val popupText1: String,
	val popupBoldText: String,
	val popupText2: String,
	val popupLinkText: String,
	val popupLinkUrl: String,
)

