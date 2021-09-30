package ch.admin.bag.covidcertificate.common.qr

import ch.admin.bag.covidcertificate.sdk.core.models.state.StateError

sealed class DecodeCertificateState {
	data class SUCCESS(val qrCode: String?) : DecodeCertificateState()
	object SCANNING : DecodeCertificateState()
	data class ERROR(val error: StateError) : DecodeCertificateState()
}