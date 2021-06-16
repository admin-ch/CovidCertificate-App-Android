package ch.admin.bag.covidcertificate.common.util

import ch.admin.bag.covidcertificate.eval.data.state.CheckNationalRulesState
import ch.admin.bag.covidcertificate.eval.data.state.CheckSignatureState
import ch.admin.bag.covidcertificate.eval.data.state.VerificationState

fun VerificationState.getInvalidErrorCode(errorDelimiter: String = ", ", showNationalErrors: Boolean = false): String {
	val errorCodes = mutableListOf<String>()
	if (this !is VerificationState.INVALID) return ""

	val signatureState = signatureState
	if (signatureState is CheckSignatureState.INVALID) {
		errorCodes.add(signatureState.signatureErrorCode)
	}

	val nationalRulesState = nationalRulesState
	if (showNationalErrors && nationalRulesState is CheckNationalRulesState.INVALID) {
		errorCodes.add(nationalRulesState.nationalRulesError.errorCode)
	}
	return errorCodes.joinToString(errorDelimiter)
}