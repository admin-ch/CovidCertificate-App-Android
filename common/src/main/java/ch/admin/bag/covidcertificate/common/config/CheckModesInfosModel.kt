package ch.admin.bag.covidcertificate.common.config

import com.squareup.moshi.JsonClass
//for verifier
@JsonClass(generateAdapter = true)
data class CheckModesInfosModel(
	val infos: Map<String, CheckModeInfoModel>?
)
//for wallet
@JsonClass(generateAdapter = true)
data class CheckModesInfoModel(
	val title: String?,
	val modes: Map<String, WalletModeModel>?,
)


@JsonClass(generateAdapter = true)
data class WalletModeModel(
	val ok: WalletModeDetails,
	val notOk: WalletModeDetails
)

@JsonClass(generateAdapter = true)
data class WalletModeDetails(
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