package ch.admin.bag.covidcertificate.eval.chain

import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.admin.bag.covidcertificate.eval.HC1_A
import ch.admin.bag.covidcertificate.eval.getInvalidSigningKeys
import ch.admin.bag.covidcertificate.eval.models.CertType
import ch.admin.bag.covidcertificate.eval.utils.getHardcodedBagSigningKeys
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VerificationCoseServiceTest {

	private val contextIdentifierService = DefaultContextIdentifierService()
	private val base45Service = BagBase45Service()
	private val compressorService = DecompressionService()

	@Test
	fun decode_success() {
		val bagKeys = getHardcodedBagSigningKeys()
		val coseService = VerificationCoseService(bagKeys)

		val vr = VerificationResult()
		val encoded = contextIdentifierService.decode(HC1_A, vr)
		val compressed = base45Service.decode(encoded, vr)
		val cose = compressorService.decode(compressed, vr)
		coseService.decode(cose, vr, CertType.VACCINATION)

		assertTrue(vr.coseVerified)
	}

	@Test
	fun decode_invalidSigningKey() {
		val invalidKeys = getInvalidSigningKeys()
		val coseService = VerificationCoseService(invalidKeys)

		val vr = VerificationResult()
		val encoded = contextIdentifierService.decode(HC1_A, vr)
		val compressed = base45Service.decode(encoded, vr)
		val cose = compressorService.decode(compressed, vr)
		coseService.decode(cose, vr, CertType.VACCINATION)

		assertFalse(vr.coseVerified)
	}

}