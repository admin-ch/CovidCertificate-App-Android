/**
 * Adapted from https://github.com/ehn-digital-green-development/hcert-kotlin
 * published under Apache-2.0 License.
 */
package ch.admin.bag.covidcertificate.eval.chain

import java.util.zip.InflaterInputStream

class DecompressionService {

	fun decode(input: ByteArray, verificationResult: VerificationResult): ByteArray {
		verificationResult.zlibDecoded = false

		return try {
			InflaterInputStream(input.inputStream()).readBytes().also {
				verificationResult.zlibDecoded = true
			}
		} catch (e: Throwable) {
			input
		}
	}

}