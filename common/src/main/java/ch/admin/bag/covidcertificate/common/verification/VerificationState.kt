package ch.admin.bag.covidcertificate.common.verification

import ch.admin.bag.covidcertificate.eval.CheckNationalRulesState
import ch.admin.bag.covidcertificate.eval.CheckRevocationState
import ch.admin.bag.covidcertificate.eval.CheckSignatureState
import ch.admin.bag.covidcertificate.eval.Error
import ch.admin.bag.covidcertificate.eval.nationalrules.ValidityRange

sealed class VerificationState {
	data class SUCCESS(val validityRange: ValidityRange) : VerificationState()
	data class INVALID(
		val signatureState: CheckSignatureState?,
		val revocationState: CheckRevocationState?,
		val nationalRulesState: CheckNationalRulesState?,
		val validityRange: ValidityRange?
	) : VerificationState()

	object LOADING : VerificationState()
	data class ERROR(val error: Error, val retry: Runnable, val validityRange: ValidityRange?) : VerificationState()
}