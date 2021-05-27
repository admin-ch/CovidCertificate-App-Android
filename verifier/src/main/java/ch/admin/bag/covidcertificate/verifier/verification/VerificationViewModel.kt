package ch.admin.bag.covidcertificate.verifier.verification

import android.app.Application
import androidx.lifecycle.*
import ch.admin.bag.covidcertificate.common.verification.CertificateVerifier
import ch.admin.bag.covidcertificate.common.verification.VerificationState
import ch.admin.bag.covidcertificate.eval.models.Bagdgc
import ch.admin.bag.covidcertificate.verifier.BuildConfig

class VerificationViewModel(application: Application) : AndroidViewModel(application) {

	private val certificateVerifier = CertificateVerifier(getApplication(), viewModelScope)
	val verificationLiveData: LiveData<VerificationState> = certificateVerifier.liveData

	fun startVerification(bagdgc: Bagdgc) {
		certificateVerifier.startVerification(bagdgc)
	}

}