/**
 * Adapted from https://github.com/ehn-digital-green-development/hcert-kotlin
 * published under Apache-2.0 License.
 */
package ch.admin.bag.covidcertificate.eval.chain

import COSE.MessageTag
import COSE.Sign1Message

/**
 * A no-op COSE verification services.
 * Does not acutally check the signature, but simply extracts the message and returns it.
 */
class NoopVerificationCoseService {

	fun decode(input: ByteArray, verificationResult: VerificationResult): ByteArray {
		verificationResult.coseVerified = false
		return try {
			val cose: Sign1Message = Sign1Message.DecodeFromBytes(input, MessageTag.Sign1) as Sign1Message
			cose.GetContent()
		} catch (e: Throwable) {
			input
		}
	}

}