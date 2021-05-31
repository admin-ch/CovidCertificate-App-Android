/**
 * Adapted from https://github.com/ehn-digital-green-development/hcert-kotlin
 * published under Apache-2.0 License.
 */
package ch.admin.bag.covidcertificate.eval.chain

import COSE.MessageTag
import COSE.OneKey
import COSE.Sign1Message
import ch.admin.bag.covidcertificate.eval.models.CertType
import ch.admin.bag.covidcertificate.eval.models.Jwk

class VerificationCoseService(private val keys: List<Jwk>) {
	private val TAG = VerificationCoseService::class.java.simpleName

	fun decode(input: ByteArray, verificationResult: VerificationResult, type: CertType): ByteArray {
		verificationResult.coseVerified = false

		val signature: Sign1Message = try {
			(Sign1Message.DecodeFromBytes(input, MessageTag.Sign1) as Sign1Message)
		} catch (e: Throwable) {
			null
		} ?: return input

		for (k in keys) {
			val pk = k.getPublicKey() ?: continue

			try {
				val pubKey = OneKey(pk, null)
				if (signature.validate(pubKey)) {
					verificationResult.coseVerified = true
					return signature.GetContent()
				}
			} catch (e: Throwable) {
				e.printStackTrace()
			}
		}

		return input
	}

}