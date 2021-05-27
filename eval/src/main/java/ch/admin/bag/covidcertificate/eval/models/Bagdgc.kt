package ch.admin.bag.covidcertificate.eval.models

import ch.admin.bag.covidcertificate.eval.chain.VerificationResult
import ch.admin.bag.covidcertificate.eval.data.Eudgc
import java.io.Serializable

data class Bagdgc(
	val dgc: Eudgc,
	val qrCodeData: String,
	val cose: ByteArray,
	val verificationResult: VerificationResult,
) : Serializable {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as Bagdgc

		if (dgc != other.dgc) return false
		if (!cose.contentEquals(other.cose)) return false
		if (qrCodeData != other.qrCodeData) return false
		if (verificationResult != other.verificationResult) return false

		return true
	}

	override fun hashCode(): Int {
		var result = dgc.hashCode()
		result = 31 * result + cose.contentHashCode()
		result = 31 * result + qrCodeData.hashCode()
		result = 31 * result + verificationResult.hashCode()
		return result
	}

	fun getType(): CertType? {
		// Certificate must not have two types => if it has more then it is invalid
		return if (verificationResult.content.size == 1) {
			verificationResult.content.first()
		} else {
			null
		}
	}
}



