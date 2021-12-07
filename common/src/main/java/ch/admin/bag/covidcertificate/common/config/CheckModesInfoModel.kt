package ch.admin.bag.covidcertificate.common.config

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CheckModesInfoModel(
	val infos: Map<String, CheckModeInfoModel>
)

@JsonClass(generateAdapter = true)
data class CheckModeInfoModel(
	val title: String,
	val hexColor: String,
	val infos: List<CheckModeInfoEntry>
)

data class CheckModeInfoModelWithId(
	val id: String,
	val title: String,
	val hexColor: String,
	val infos: List<CheckModeInfoEntry>
)

@JsonClass(generateAdapter = true)
data class CheckModeInfoEntry(
	val iconAndroid: String,
	val text: String
)