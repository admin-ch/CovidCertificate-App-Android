package ch.admin.bag.covidcertificate.verifier

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ch.admin.bag.covidcertificate.common.config.ConfigModel
import ch.admin.bag.covidcertificate.verifier.networking.ConfigRepository
import kotlinx.coroutines.launch

class VerifierViewModel(application: Application) : AndroidViewModel(application) {

	private val configMutableLiveData = MutableLiveData<ConfigModel>()
	val configLiveData: LiveData<ConfigModel> = configMutableLiveData

	fun loadConfig() {
		val configRepository = ConfigRepository.getInstance(getApplication())
		viewModelScope.launch {
			configRepository.loadConfig(getApplication())?.let { config -> configMutableLiveData.postValue(config) }
		}
	}

}