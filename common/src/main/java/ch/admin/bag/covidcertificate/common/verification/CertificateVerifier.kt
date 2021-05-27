package ch.admin.bag.covidcertificate.common.verification

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import ch.admin.bag.covidcertificate.eval.*
import ch.admin.bag.covidcertificate.eval.models.Bagdgc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CertificateVerifier(
	private val context: Context,
	private val coroutineScope: CoroutineScope,
	private val bagdgc: Bagdgc? = null,
) {

	private val checkSignatureStateMutableLiveData = MutableLiveData<CheckSignatureState>()
	private val checkRevocationStateMutableLiveData = MutableLiveData<CheckRevocationState>()
	private val checkNationalRulesStateMutableLiveData = MutableLiveData<CheckNationalRulesState>()

	private val stateMediator = MediatorLiveData<VerificationState>().apply {
		addSource(checkSignatureStateMutableLiveData) {
			val state =
				checkVerificationState(it, checkRevocationStateMutableLiveData.value, checkNationalRulesStateMutableLiveData.value)
			if (value != state) postValue(state)
		}
		addSource(checkRevocationStateMutableLiveData) {
			val state =
				checkVerificationState(checkSignatureStateMutableLiveData.value, it, checkNationalRulesStateMutableLiveData.value)
			if (value != state) postValue(state)
		}
		addSource(checkNationalRulesStateMutableLiveData) {
			val state =
				checkVerificationState(checkSignatureStateMutableLiveData.value, checkRevocationStateMutableLiveData.value, it)
			if (value != state) postValue(state)
		}
	}
	val liveData: LiveData<VerificationState> = stateMediator

	private fun checkVerificationState(
		checkSignatureState: CheckSignatureState?,
		checkRevocationState: CheckRevocationState?,
		checkNationalRulesState: CheckNationalRulesState?,
	): VerificationState {
		return if (checkSignatureState is CheckSignatureState.ERROR) {
			VerificationState.ERROR(
				checkSignatureState.error,
				{ checkSignatureState.error.bagdgc?.let { checkSignature(it) } },
				checkNationalRulesState?.validityRange()
			)
		} else if (checkRevocationState is CheckRevocationState.ERROR) {
			VerificationState.ERROR(
				checkRevocationState.error,
				{ checkRevocationState.error.bagdgc?.let { checkRevocationStatus(it) } },
				checkNationalRulesState?.validityRange()
			)
		} else if (checkNationalRulesState is CheckNationalRulesState.ERROR) {
			VerificationState.ERROR(
				checkNationalRulesState.error,
				{ checkNationalRulesState.error.bagdgc?.let { checkNationalRules(it) } },
				null
			)
		} else if (checkSignatureState == CheckSignatureState.SUCCESS &&
			checkRevocationState == CheckRevocationState.SUCCESS &&
			checkNationalRulesState is CheckNationalRulesState.SUCCESS
		)
			VerificationState.SUCCESS(checkNationalRulesState.validityRange)
		else if (checkSignatureState is CheckSignatureState.INVALID ||
			checkRevocationState is CheckRevocationState.INVALID ||
			checkNationalRulesState is CheckNationalRulesState.INVALID ||
			checkNationalRulesState is CheckNationalRulesState.NOT_YET_VALID ||
			checkNationalRulesState is CheckNationalRulesState.NOT_VALID_ANYMORE
		) {
			VerificationState.INVALID(
				checkSignatureState, checkRevocationState, checkNationalRulesState,
				checkNationalRulesState?.validityRange()
			)
		} else {
			VerificationState.LOADING
		}
	}

	private fun checkSignature(bagdgc: Bagdgc) {
		coroutineScope.launch(Dispatchers.IO) {
			try {
				val checkSignatureState = Eval.checkSignature(bagdgc, context)
				checkSignatureStateMutableLiveData.postValue(checkSignatureState)
			} catch (e: Exception) {
				checkSignatureStateMutableLiveData.postValue(
					CheckSignatureState.ERROR(Error(EvalErrorCodes.SIGNATURE_UNKNOWN, e.message.toString(), bagdgc))
				)
			}
		}
	}

	private fun checkRevocationStatus(bagdgc: Bagdgc) {
		coroutineScope.launch(Dispatchers.IO) {
			try {
				val checkRevocationState = Eval.checkRevocationStatus(bagdgc, context)
				checkRevocationStateMutableLiveData.postValue(checkRevocationState)
			} catch (e: Exception) {
				checkRevocationStateMutableLiveData.postValue(
					CheckRevocationState.ERROR(Error(EvalErrorCodes.REVOCATION_UNKNOWN, e.message.toString(), bagdgc))
				)
			}
		}
	}

	private fun checkNationalRules(bagdgc: Bagdgc) {
		coroutineScope.launch(Dispatchers.IO) {
			try {
				val checkNationalRulesState = Eval.checkNationalRules(bagdgc, context)
				checkNationalRulesStateMutableLiveData.postValue(checkNationalRulesState)
			} catch (e: Exception) {
				checkNationalRulesStateMutableLiveData.postValue(
					CheckNationalRulesState.ERROR(Error(EvalErrorCodes.RULESET_UNKNOWN, e.message.toString(), bagdgc))
				)
			}
		}
	}

	fun startVerification(bagdgc: Bagdgc? = this.bagdgc) {
		val bagdgc = bagdgc ?: return
		stateMediator.value = VerificationState.LOADING
		checkSignature(bagdgc)
		checkRevocationStatus(bagdgc)
		checkNationalRules(bagdgc)
	}
}