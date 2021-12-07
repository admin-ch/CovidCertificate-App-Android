package ch.admin.bag.covidcertificate.common.config

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CheckModesInfoModel(
	val title: String,
	val infos: Map<String, CheckModeInfoModel>,
	val modes: Map<String, ModeModel>
)

@JsonClass(generateAdapter = true)
data class ModeModel(
	val ok: ModeDetails,
	val notOk: ModeDetails
)

@JsonClass(generateAdapter = true)
data class ModeDetails(
	val iconAndroid: String,
	val iconIos: String,
	val text: String
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