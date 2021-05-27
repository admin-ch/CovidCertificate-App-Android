/**
 * Adapted from https://github.com/ehn-digital-green-development/hcert-kotlin
 * published under Apache-2.0 License.
 */
package ch.admin.bag.covidcertificate.eval.chain


class DefaultContextIdentifierService(private val prefix: String = "HC1:") {

    fun decode(input: String, verificationResult: VerificationResult): String = when {
        input.startsWith(prefix) -> input.drop(prefix.length).also { verificationResult.contextIdentifier = prefix }
        else -> input.also { verificationResult.contextIdentifier = null }
    }

}