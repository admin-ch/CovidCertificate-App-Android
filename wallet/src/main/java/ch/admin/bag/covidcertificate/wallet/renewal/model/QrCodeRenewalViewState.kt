package ch.admin.bag.covidcertificate.wallet.renewal.model

import ch.admin.bag.covidcertificate.sdk.core.models.state.StateError

sealed class QrCodeRenewalViewState {
	object RenewalRequired : QrCodeRenewalViewState()
	object RenewalInProgress : QrCodeRenewalViewState()
	object RenewalSuccessful : QrCodeRenewalViewState()
	data class RenewalFailed(val error: StateError) : QrCodeRenewalViewState()
}
