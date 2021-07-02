package ch.admin.bag.covidcertificate.common.util

import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckNationalRulesState
import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckRevocationState
import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckSignatureState
import ch.admin.bag.covidcertificate.sdk.core.models.state.VerificationState

fun VerificationState.getInvalidErrorCode(errorDelimiter: String = ", ", showNationalErrors: Boolean = false): String {
	val errorCodes = mutableListOf<String>()
	if (this !is VerificationState.INVALID) return ""

	val signatureState = signatureState
	if (signatureState is CheckSignatureState.INVALID) {
		errorCodes.add(signatureState.signatureErrorCode)
	}

	val revocationState = revocationState
	if (revocationState is CheckRevocationState.INVALID) {
		errorCodes.add(revocationState.revocationErrorCode)
	}

	val nationalRulesState = nationalRulesState
	if (showNationalErrors && nationalRulesState is CheckNationalRulesState.INVALID) {
		nationalRulesState.nationalRulesError?.errorCode?.let { errorCodes.add(it) }
	}
	return errorCodes.joinToString(errorDelimiter)
}