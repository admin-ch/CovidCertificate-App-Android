/**
 * Adapted from https://github.com/ehn-digital-green-development/hcert-kotlin
 * published under Apache-2.0 License.
 */
package ch.admin.bag.covidcertificate.eval.chain

import ch.admin.bag.covidcertificate.eval.utils.Base45

/**
 * Encodes/decodes input in/from Base45
 */
open class BagBase45Service {

    fun decode(input: String, verificationResult: VerificationResult): ByteArray {
        verificationResult.base45Decoded = false
        return try {
            Base45.getDecoder().decode(input).also {
                verificationResult.base45Decoded = true
            }
        } catch (e: Throwable) {
            // Assume that the data might not have been base45, and continue the chain
            input.toByteArray()
        }
    }

}