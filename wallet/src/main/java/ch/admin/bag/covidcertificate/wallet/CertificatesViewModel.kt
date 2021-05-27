package ch.admin.bag.covidcertificate.wallet

import android.app.Application
import androidx.lifecycle.*
import ch.admin.bag.covidcertificate.common.config.ConfigModel
import ch.admin.bag.covidcertificate.common.util.SingleLiveEvent
import ch.admin.bag.covidcertificate.common.verification.CertificateVerifier
import ch.admin.bag.covidcertificate.common.verification.VerificationState
import ch.admin.bag.covidcertificate.eval.DecodeState
import ch.admin.bag.covidcertificate.eval.Eval
import ch.admin.bag.covidcertificate.eval.models.Bagdgc
import ch.admin.bag.covidcertificate.wallet.data.CertificateStorage
import ch.admin.bag.covidcertificate.wallet.networking.ConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CertificatesViewModel(application: Application) : AndroidViewModel(application) {

	private val certificatesCollectionMutableLiveData = MutableLiveData<List<Bagdgc>>()
	val certificatesCollectionLiveData: LiveData<List<Bagdgc>> = certificatesCollectionMutableLiveData

	private val certificateStorage: CertificateStorage by lazy { CertificateStorage.getInstance(getApplication()) }

	val onQrCodeClickedSingleLiveEvent = SingleLiveEvent<Bagdgc>()

	fun loadCertificates() {
		viewModelScope.launch(Dispatchers.Default) {
			certificatesCollectionMutableLiveData.postValue(
				certificateStorage.getCertificateList().mapNotNull { (Eval.decode(it) as? DecodeState.SUCCESS)?.dgc }
			)
		}
	}

	fun onQrCodeClicked(certificate: Bagdgc) {
		onQrCodeClickedSingleLiveEvent.postValue(certificate)
	}

	fun containsCertificate(certificate: String): Boolean {
		return certificateStorage.containsCertificate(certificate)
	}

	fun addCertificate(certificate: String) {
		certificateStorage.saveCertificate(certificate)
	}

	fun moveCertificate(from: Int, to: Int) {
		certificateStorage.changeCertificatePosition(from, to)
	}

	fun removeCertificate(certificate: String) {
		certificateStorage.deleteCertificate(certificate)
		loadCertificates()
	}

	private val configMutableLiveData = MutableLiveData<ConfigModel>()
	val configLiveData: LiveData<ConfigModel> = configMutableLiveData

	fun loadConfig() {
		val configRepository = ConfigRepository.getInstance(getApplication())
		viewModelScope.launch {
			configRepository.loadConfig(getApplication())?.let { config -> configMutableLiveData.postValue(config) }
		}
	}

	private val certificateVerifierMapMutableLiveData = MutableLiveData<Map<String, CertificateVerifier>>(HashMap())
	val certificateVerifierMapLiveData: LiveData<Map<String, CertificateVerifier>> = certificateVerifierMapMutableLiveData

	private val verifiedCertificatesMediatorLiveData = MediatorLiveData<List<VerifiedCertificate>>().apply {
		addSource(certificatesCollectionLiveData) { certificates ->
			value =
				certificates.map { certificate ->
					val verifier = certificateVerifierMapLiveData.value?.get(certificate.qrCodeData)
					val state = verifier?.liveData?.value
					return@map VerifiedCertificate(certificate, state ?: VerificationState.LOADING)
				}
		}
	}
	val verifiedCertificatesLiveData: LiveData<List<VerifiedCertificate>> = verifiedCertificatesMediatorLiveData

	init {
		certificatesCollectionLiveData.observeForever { certificates -> updateCertificateVerifiers(certificates) }
	}

	private fun updateCertificateVerifiers(certificates: List<Bagdgc>) {
		val certificateVerifierMap = certificateVerifierMapLiveData.value!!.toMutableMap()
		val newCertificateSet = certificates.map { bagdgc -> bagdgc.qrCodeData }.toSet()

		certificateVerifierMap.keys
			.filter { !newCertificateSet.contains(it) }
			.forEach { removedCertificate ->
				certificateVerifierMap[removedCertificate]?.liveData?.let { verifiedCertificatesMediatorLiveData.removeSource(it) }
				certificateVerifierMap.remove(removedCertificate)
			}

		certificates
			.filter { !certificateVerifierMap.containsKey(it.qrCodeData) }
			.forEach { addedCertificate ->
				val newVerifier = CertificateVerifier(getApplication(), viewModelScope, addedCertificate)
				certificateVerifierMap[addedCertificate.qrCodeData] = newVerifier
				verifiedCertificatesMediatorLiveData.addSource(newVerifier.liveData) { state ->
					val currentStates = verifiedCertificatesMediatorLiveData.value ?: return@addSource
					verifiedCertificatesMediatorLiveData.value = currentStates.map { verifiedCertificate ->
						return@map if (verifiedCertificate.certificate.qrCodeData == addedCertificate.qrCodeData) {
							VerifiedCertificate(addedCertificate, state)
						} else {
							verifiedCertificate
						}
					}
				}
			}
		certificateVerifierMapMutableLiveData.value = certificateVerifierMap
	}

	data class VerifiedCertificate(val certificate: Bagdgc, val state: VerificationState)
}