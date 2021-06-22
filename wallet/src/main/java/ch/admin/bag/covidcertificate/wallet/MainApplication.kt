package ch.admin.bag.covidcertificate.wallet

import android.app.Application
import android.os.Build
import ch.admin.bag.covidcertificate.eval.data.Config
import ch.admin.bag.covidcertificate.eval.net.UserAgentInterceptor
import ch.admin.bag.covidcertificate.eval.CovidCertificateSdk
import ch.admin.bag.covidcertificate.wallet.data.CertificateStorage
import ch.admin.bag.covidcertificate.wallet.data.WalletDataItem
import ch.admin.bag.covidcertificate.wallet.data.WalletDataSecureStorage
import ch.admin.bag.covidcertificate.wallet.data.WalletSecureStorage

class MainApplication : Application() {

	override fun onCreate() {
		super.onCreate()
		Config.appToken = BuildConfig.SDK_APP_TOKEN
		Config.userAgent =
			UserAgentInterceptor.UserAgentGenerator { "${this.packageName};${BuildConfig.VERSION_NAME};${BuildConfig.BUILD_TIME};Android;${Build.VERSION.SDK_INT}" }

		CovidCertificateSdk.init(this)

		migrateCertificatesToWalletData()
	}

	@Suppress("DEPRECATION")
	private fun migrateCertificatesToWalletData() {
		val walletStorage = WalletSecureStorage.getInstance(this)
		if (!walletStorage.getMigratedCertificatesToWalletData()) {
			val certificateStorage = CertificateStorage.getInstance(this)
			val walletDataStorage = WalletDataSecureStorage.getInstance(this)

			val certificates = certificateStorage.getCertificateList()
			certificates.reversed().map {
				val walletData = WalletDataItem.CertificateWalletData(it)
				walletDataStorage.saveWalletDataItem(walletData)
			}

			walletStorage.setMigratedCertificatesToWalletData(true)
		}
	}
}