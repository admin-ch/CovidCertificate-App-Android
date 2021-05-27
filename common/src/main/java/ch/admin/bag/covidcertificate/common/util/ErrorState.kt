package ch.admin.bag.covidcertificate.common.util

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ch.admin.bag.covidcertificate.common.R

enum class ErrorState(
	@field:StringRes @param:StringRes val titleResId: Int,
	@field:StringRes @param:StringRes val textResId: Int,
	@field:StringRes @param:StringRes val actionResId: Int,
	@field:DrawableRes @param:DrawableRes val imageResId: Int
) {
	NETWORK(
		R.string.error_network_title,
		R.string.error_network_text,
		R.string.error_action_retry,
		R.drawable.ic_error_triangle
	),
	CAMERA_ACCESS_DENIED(
		R.string.error_camera_permission_title,
		R.string.error_camera_permission_text,
		R.string.error_action_change_settings,
		R.drawable.ic_cam_off
	),
	NO_VALID_QR_CODE(
		R.string.error_title,
		R.string.qr_scanner_error,
		R.string.ok_button,
		R.drawable.ic_error_triangle
	);
}