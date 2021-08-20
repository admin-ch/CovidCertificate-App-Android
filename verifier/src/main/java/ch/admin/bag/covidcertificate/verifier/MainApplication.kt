package ch.admin.bag.covidcertificate.verifier

import android.app.Application
import android.os.Build
import ch.admin.bag.covidcertificate.common.data.ConfigSecureStorage
import ch.admin.bag.covidcertificate.common.debug.DebugFragment
import ch.admin.bag.covidcertificate.common.util.EnvironmentUtil
import ch.admin.bag.covidcertificate.sdk.android.CovidCertificateSdk
import ch.admin.bag.covidcertificate.sdk.android.data.Config
import ch.admin.bag.covidcertificate.sdk.android.net.interceptor.UserAgentInterceptor
import ch.admin.bag.covidcertificate.verifier.data.VerifierSecureStorage

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
	}
}