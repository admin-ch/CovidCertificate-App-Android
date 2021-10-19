package ch.admin.bag.covidcertificate.wallet.vaccination.hint

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ch.admin.bag.covidcertificate.common.net.ConfigRepository
import ch.admin.bag.covidcertificate.wallet.data.WalletSecureStorage
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class VaccinationHintViewModel(application: Application) : AndroidViewModel(application) {

	companion object {
		private val VACCINATION_HINT_DISMISS_THRESHOLD = TimeUnit.DAYS.toMillis(30)
	}

	private val walletSecureStorage = WalletSecureStorage.getInstance(application.applicationContext)

	private val displayVaccinationHintMutable = MutableLiveData(false)
	val displayVaccinationHint = displayVaccinationHintMutable as LiveData<Boolean>

	init {
		val now = Instant.now()
		val lastDismiss = Instant.ofEpochMilli(walletSecureStorage.getVaccinationHintDismissTimestamp())
		val displayHint = ConfigRepository.getCurrentConfig(application.applicationContext)?.showVaccinationHintHomescreen ?: false

		// Show the vaccination hints if the last dismiss was more than 7 days ago
		if (displayHint && lastDismiss.until(now, ChronoUnit.MILLIS) > VACCINATION_HINT_DISMISS_THRESHOLD) {
			displayVaccinationHintMutable.value = true
		}
	}

	fun dismissVaccinationHint() {
		walletSecureStorage.setVaccinationHintDismissTimestamp(Instant.now().toEpochMilli())
		displayVaccinationHintMutable.value = false
	}

}