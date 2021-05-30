/**
 * Adapted from https://github.com/ehn-digital-green-development/hcert-kotlin
 * published under Apache-2.0 License.
 */
package ch.admin.bag.covidcertificate.eval.chain

import COSE.HeaderKeys
import COSE.MessageTag
import COSE.OneKey
import COSE.Sign1Message
import ch.admin.bag.covidcertificate.eval.models.CertType
import ch.admin.bag.covidcertificate.eval.models.Jwk
import ch.admin.bag.covidcertificate.eval.utils.toBase64

class VerificationCoseService(private val keys: List<Jwk>) {
	private val TAG = VerificationCoseService::class.java.simpleName

	fun decode(input: ByteArray, verificationResult: VerificationResult, type: CertType): ByteArray {
		verificationResult.coseVerified = false

		return try {
			(Sign1Message.DecodeFromBytes(input, MessageTag.Sign1) as Sign1Message).also { signature ->
				try {
					keys
						// "use": keys could be valid for signing a vaccination, but not a test certificate.
						.filter { k -> k.isAllowedToSign(type) }
						.filter { k -> k.getPublicKey() != null }
						.forEach { k ->
							val pubKey = OneKey(k.getPublicKey(), null)
							if (signature.validate(pubKey)) {
								verificationResult.coseVerified = true
								return signature.GetContent()
							}
						}
				} catch (e: Throwable) {
					e.printStackTrace()
					signature.GetContent()
				}
			}.GetContent()
		} catch (e: Throwable) {
			e.printStackTrace()
			input
		}
	}

}