/**
 * Adapted from https://github.com/ehn-digital-green-development/hcert-kotlin
 * published under Apache-2.0 License.
 */
package ch.admin.bag.covidcertificate.eval.chain

import java.time.Instant

class TimestampVerificationService {
	private val TAG = TimestampVerificationService::class.java.simpleName

	fun validate(verificationResult: VerificationResult) {
		verificationResult.timestampVerified = true
		val now = Instant.now()

		verificationResult.expirationTime?.also { et ->
			if (et.isBefore(now)) {
				verificationResult.timestampVerified = false
			}
		}

		verificationResult.issuedAt?.also { ia ->
			if (ia.isAfter(now)) {
				verificationResult.timestampVerified = false
			}
		}
	}

}