package ch.admin.bag.covidcertificate.verifier

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.core.os.bundleOf
import ch.admin.bag.covidcertificate.common.data.ConfigSecureStorage
import ch.admin.bag.covidcertificate.common.debug.DebugFragment
import ch.admin.bag.covidcertificate.common.util.EnvironmentUtil
import ch.admin.bag.covidcertificate.sdk.android.CovidCertificateSdk
import ch.admin.bag.covidcertificate.sdk.android.data.Config
import ch.admin.bag.covidcertificate.sdk.android.net.interceptor.UserAgentInterceptor
import ch.admin.bag.covidcertificate.verifier.data.VerifierSecureStorage
import ch.admin.bag.covidcertificate.verifier.extensions.getApplicationSignature

class MainApplication : Application() {

	override fun onCreate() {
		super.onCreate()

		if (DebugFragment.EXISTS) {
			DebugFragment.initDebug(this)
		}

		// If this is a fresh install, don't show the certificate light updateboarding
		val isFreshInstall = ConfigSecureStorage.getInstance(this).getConfig() == null
		if (isFreshInstall) {
			VerifierSecureStorage.getInstance(this).setCertificateLightUpdateboardingCompleted(true)
		}

		Config.appToken = BuildConfig.SDK_APP_TOKEN
		Config.userAgent =
			UserAgentInterceptor.UserAgentGenerator { "${this.packageName};${BuildConfig.VERSION_NAME};${BuildConfig.BUILD_TIME};Android;${Build.VERSION.SDK_INT}" }

		CovidCertificateSdk.init(this, EnvironmentUtil.getSdkEnvironment())

		initializeZebraDataWedgeApi()
	}

	/**
	 * Send a broadcast to configure Zebra phones for Intent Output mode
	 * @see <a href="https://techdocs.zebra.com/datawedge/11-0/guide/output/intent/">https://techdocs.zebra.com/datawedge/11-0/guide/output/intent/</a>
	 */
	private fun initializeZebraDataWedgeApi() {
		// Send a broadcast to the Data Wedge app to create a new profile
		val createProfileIntent = Intent().apply {
			action = "com.symbol.datawedge.api.ACTION"
			putExtra("com.symbol.datawedge.api.CREATE_PROFILE", "CovidCheck-${BuildConfig.FLAVOR}")
		}
		sendBroadcast(createProfileIntent)

		// Set the Intent Output config in the Data Wedge app
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
						"SIGNATURE" to getApplicationSignature(),
					),
					"intent_output_enabled" to true,
				)
			)
		)

		val configIntent = Intent().apply {
			action = "com.symbol.datawedge.api.ACTION"
			putExtra("com.symbol.datawedge.api.SET_CONFIG", configExtras)
			putExtra("SEND_RESULT", "LAST_RESULT")
		}

		sendBroadcast(configIntent)
	}

}