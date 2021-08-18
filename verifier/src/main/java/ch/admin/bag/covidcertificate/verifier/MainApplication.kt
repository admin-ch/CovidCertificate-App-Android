package ch.admin.bag.covidcertificate.verifier

import android.app.Application
import android.content.Context
import android.os.Build
import ch.admin.bag.covidcertificate.common.data.ConfigSecureStorage
import ch.admin.bag.covidcertificate.common.util.EnvironmentUtil
import ch.admin.bag.covidcertificate.sdk.android.CovidCertificateSdk
import ch.admin.bag.covidcertificate.sdk.android.data.Config
import ch.admin.bag.covidcertificate.sdk.android.net.interceptor.UserAgentInterceptor
import ch.admin.bag.covidcertificate.sdk.android.utils.CorruptedEncryptedSharedPreferencesException
import ch.admin.bag.covidcertificate.sdk.android.utils.SingletonHolder
import ch.admin.bag.covidcertificate.verifier.data.VerifierSecureStorage

class MainApplication : Application() {

	private val mutableFailedSharedPrefs = mutableListOf<String>("SecureStorage")
	val failedSharedPrefs: List<String> = mutableFailedSharedPrefs

	override fun onCreate() {
		super.onCreate()

		loadAllEncryptedSharedPrefs(this)

		// If this is a fresh install, don't show the certificate light updateboarding
		val isFreshInstall = ConfigSecureStorage.getInstance(this).getConfig() == null
		if (isFreshInstall) {
			VerifierSecureStorage.getInstance(this).setCertificateLightUpdateboardingCompleted(true)
		}

		Config.appToken = BuildConfig.SDK_APP_TOKEN
		Config.userAgent =
			UserAgentInterceptor.UserAgentGenerator { "${this.packageName};${BuildConfig.VERSION_NAME};${BuildConfig.BUILD_TIME};Android;${Build.VERSION.SDK_INT}" }

		try {
			CovidCertificateSdk.init(this, EnvironmentUtil.getSdkEnvironment())
		} catch (e: CorruptedEncryptedSharedPreferencesException) {
			mutableFailedSharedPrefs.add(e.preferencesName)
		}

		if (failedSharedPrefs.isNotEmpty()) return
		// Add anything that uses the EncryptedSharedPrefs below this line
	}

	private fun loadAllEncryptedSharedPrefs(context: Context) {
		val prefs = listOf<SingletonHolder<Any, Context>>(
			ConfigSecureStorage,
			VerifierSecureStorage,
		)

		for (pref in prefs) {
			try {
				pref.getInstance(context)
			} catch (e: CorruptedEncryptedSharedPreferencesException) {
				mutableFailedSharedPrefs.add(e.preferencesName)
			}
		}
	}
}