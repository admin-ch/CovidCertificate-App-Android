package ch.admin.bag.covidcertificate.wallet

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import ch.admin.bag.covidcertificate.common.debug.DebugFragment
import ch.admin.bag.covidcertificate.common.net.ConfigRepository
import ch.admin.bag.covidcertificate.common.util.EnvironmentUtil
import ch.admin.bag.covidcertificate.sdk.android.CovidCertificateSdk
import ch.admin.bag.covidcertificate.sdk.android.data.Config
import ch.admin.bag.covidcertificate.sdk.android.net.interceptor.UserAgentInterceptor
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.wallet.data.CertificateStorage
import ch.admin.bag.covidcertificate.wallet.data.WalletDataItem
import ch.admin.bag.covidcertificate.wallet.data.WalletDataSecureStorage
import ch.admin.bag.covidcertificate.wallet.data.WalletSecureStorage
import ch.admin.bag.covidcertificate.wallet.transfercode.worker.TransferWorker
import ch.admin.bag.covidcertificate.wallet.util.NotificationUtil

class MainApplication : Application() {

	companion object {

		fun getTransferCodeConversionMapping(context: Context): HashMap<String, CertificateHolder>? {
			val applicationContext = context.applicationContext
			if (applicationContext is MainApplication) {
				return applicationContext.getTransferCodeConversionMappingInternal()
			} else {
				return null
			}
		}

	}

	val transferCodeConversionMapping = HashMap<String, CertificateHolder>()

	override fun onCreate() {
		super.onCreate()

		if (DebugFragment.EXISTS) {
			DebugFragment.initDebug(this)
		}

		Config.appToken = BuildConfig.SDK_APP_TOKEN
		Config.userAgent =
			UserAgentInterceptor.UserAgentGenerator { "${this.packageName};${BuildConfig.VERSION_NAME};${BuildConfig.BUILD_TIME};Android;${Build.VERSION.SDK_INT}" }

		CovidCertificateSdk.init(this, EnvironmentUtil.getSdkEnvironment())

		migrateCertificatesToWalletData()
		migrateTransferCodeValidity()

		setupTransferWorker()
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

	private fun migrateTransferCodeValidity() {
		val walletStorage = WalletSecureStorage.getInstance(this)
		if (!walletStorage.getMigratedTransferCodeValidity()) {
			// Reading the wallet data once from the storage and writing it again immediately is enough to migrate the validity.
			// The data class constructor defines the new fields with a default value, so it is automatically set when deserializing
			val walletDataStorage = WalletDataSecureStorage.getInstance(this)
			val walletDataItems = walletDataStorage.getWalletData()
			walletDataStorage.updateWalletData(walletDataItems)

			walletStorage.setMigratedTransferCodeValidity(true)
		}
	}

	private fun setupTransferWorker() {
		ProcessLifecycleOwner.get().lifecycle.addObserver(object : LifecycleObserver {
			@OnLifecycleEvent(Lifecycle.Event.ON_START)
			fun onAppStart() {
				TransferWorker.cancelScheduledTransferWorker(this@MainApplication)
			}

			@OnLifecycleEvent(Lifecycle.Event.ON_STOP)
			fun onAppStop() {
				val hasTransferCodes = WalletDataSecureStorage.getInstance(this@MainApplication).getWalletData()
					.filterIsInstance<WalletDataItem.TransferCodeWalletData>().isNotEmpty()
				if (hasTransferCodes) {
					TransferWorker.scheduleTransferWorker(
						this@MainApplication, ConfigRepository.getCurrentConfig(this@MainApplication)
					)
				}
			}
		})
		NotificationUtil.createTransferNotificationChannel(this)
	}

	private fun getTransferCodeConversionMappingInternal() = transferCodeConversionMapping

}