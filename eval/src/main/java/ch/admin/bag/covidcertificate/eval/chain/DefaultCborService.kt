/**
 * Adapted from https://github.com/ehn-digital-green-development/hcert-kotlin
 * published under Apache-2.0 License.
 */
package ch.admin.bag.covidcertificate.eval.chain

import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper
import com.upokecenter.cbor.CBORObject
import ch.admin.bag.covidcertificate.eval.data.Eudgc
import ch.admin.bag.covidcertificate.eval.models.CertType
import java.time.Instant

class DefaultCborService {

    private val keyEuDgcV1 = CBORObject.FromObject(1)

    fun decode(input: ByteArray, verificationResult: VerificationResult): Eudgc {
        verificationResult.cborDecoded = false
        try {
            val map = CBORObject.DecodeFromBytes(input)

            map[CwtHeaderKeys.ISSUER.AsCBOR()]?.let {
                verificationResult.issuer = it.AsString()
            }
            map[CwtHeaderKeys.ISSUED_AT.AsCBOR()]?.let {
                verificationResult.issuedAt = Instant.ofEpochSecond(it.AsInt64())
            }
            map[CwtHeaderKeys.EXPIRATION.AsCBOR()]?.let {
                verificationResult.expirationTime = Instant.ofEpochSecond(it.AsInt64())
            }

            map[CwtHeaderKeys.HCERT.AsCBOR()]?.let { hcert -> // SPEC
                hcert[keyEuDgcV1]?.let {
                    val eudgc = CBORMapper()
                        .readValue(getContents(it), Eudgc::class.java)
                        .also { verificationResult.cborDecoded = true }

                    if (eudgc.t?.filterNotNull()?.isNotEmpty() == true)
                        verificationResult.content.add(CertType.TEST)
                    if (eudgc.v?.filterNotNull()?.isNotEmpty() == true)
                        verificationResult.content.add(CertType.VACCINATION)
                    if (eudgc.r?.filterNotNull()?.isNotEmpty() == true)
                        verificationResult.content.add(CertType.RECOVERY)
                    return eudgc
                }
            }
            return Eudgc()
        } catch (e: Throwable) {
            return Eudgc()
        }
    }

    private fun getContents(it: CBORObject) = try {
        it.GetByteString()
    } catch (e: Throwable) {
        it.EncodeToBytes()
    }

}