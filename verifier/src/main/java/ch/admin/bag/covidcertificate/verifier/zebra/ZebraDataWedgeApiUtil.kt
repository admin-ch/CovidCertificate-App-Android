package ch.admin.bag.covidcertificate.verifier.zebra

import android.content.Context
import android.content.Intent
import androidx.core.os.bundleOf
import ch.admin.bag.covidcertificate.verifier.BuildConfig
import ch.admin.bag.covidcertificate.verifier.extensions.getApplicationSignature

/**
 * A utility class to work with the Data Wedge API of Zebra scanner phones
 * @see <a href="https://techdocs.zebra.com/datawedge/11-0/guide/output/intent/">https://techdocs.zebra.com/datawedge/11-0/guide/output/intent/</a>
 */
object ZebraDataWedgeApiUtil {

	const val DATA_WEDGE_API_RESULT_ACTION = "com.symbol.datawedge.api.RESULT_ACTION"
	const val DATA_WEDGE_API_ACTION = "com.symbol.datawedge.api.ACTION"

	fun getEnumerateScannersIntent() = Intent().apply {
		action = DATA_WEDGE_API_ACTION
		putExtra("com.symbol.datawedge.api.ENUMERATE_SCANNERS", "")
	}

	fun getCreateProfileIntent() = Intent().apply {
		action = DATA_WEDGE_API_ACTION
		putExtra("com.symbol.datawedge.api.CREATE_PROFILE", "CovidCheck-${BuildConfig.FLAVOR}")
	}

	fun getSetConfigIntent(context: Context): Intent {
		val configExtras = bundleOf(
			"PROFILE_NAME" to "CovidCheck-${BuildConfig.FLAVOR}",
			"PROFILE_ENABLED" to true,
			"CONFIG_MODE" to "OVERWRITE",
			"APP_LIST" to arrayOf(
				bundleOf(
					"PACKAGE_NAME" to BuildConfig.APPLICATION_ID,
					"ACTIVITY_LIST" to arrayOf("*"),
				)
			),
			"PLUGIN_CONFIG" to bundleOf(
				"PLUGIN_NAME" to "INTENT",
				"RESET_CONFIG" to false,
				"PARAM_LIST" to bundleOf(
					"intent_action" to "ch.admin.bag.covidcertificate.verifier.qr.zebra",
					"intent_category" to Intent.CATEGORY_DEFAULT,
					"intent_delivery" to 2, // 2 = Deliver via Broadcast
					"intent_component_info" to bundleOf(
						"PACKAGE_NAME" to BuildConfig.APPLICATION_ID,
						"SIGNATURE" to context.getApplicationSignature(),
					),
					"intent_output_enabled" to true,
				)
			)
		)

		val configIntent = Intent().apply {
			action = "com.symbol.datawedge.api.ACTION"
			putExtra("com.symbol.datawedge.api.SET_CONFIG", configExtras)
		}

		return configIntent
	}

}