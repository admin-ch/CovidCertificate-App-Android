package ch.admin.bag.covidcertificate.eval.nationalrules

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ch.admin.bag.covidcertificate.eval.CheckNationalRulesState
import ch.admin.bag.covidcertificate.eval.TestDataGenerator
import ch.admin.bag.covidcertificate.eval.data.AcceptedVaccineProvider
import ch.admin.bag.covidcertificate.eval.utils.AcceptanceCriterias
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId


@RunWith(AndroidJUnit4::class)
class NationalRulesVerifierTest {

	lateinit var instrumentationContext: Context
	lateinit var nationalRulesVerifier: NationalRulesVerifier

	@Before
	fun setup() {
		instrumentationContext = InstrumentationRegistry.getInstrumentation().context
		nationalRulesVerifier = NationalRulesVerifier(instrumentationContext)
	}

	/// VACCINE TESTS

	@Test
	fun testVaccineDiseaseTargetedHasToBeSarsCoV2() {
		val clock = Clock.fixed(Instant.parse("2021-05-25T12:00:00Z"), ZoneId.systemDefault())
		val vaccinationDate = LocalDate.now(clock).minusDays(10)

		val cert = TestDataGenerator.generateVaccineCert(
			2,
			2,
			"ORG-100001699",
			"EU/1/21/1529",
			AcceptanceCriterias.TARGET_DISEASE,
			"J07BX03",
			vaccinationDate,
		)

		val result = nationalRulesVerifier.verifyVaccine(cert.v.first(), clock)
		assertTrue(result is CheckNationalRulesState.SUCCESS)
	}

	@Test
	fun testVaccineMustBeInWhitelist() {
		val clock = Clock.fixed(Instant.parse("2021-05-25T12:00:00Z"), ZoneId.systemDefault())
		val vaccinationDate = LocalDate.now(clock).minusDays(10)

		val valicCert = TestDataGenerator.generateVaccineCert(
			2,
			2,
			"ORG-100001699",
			"EU/1/21/1529",
			AcceptanceCriterias.TARGET_DISEASE,
			"1119349007",
			vaccinationDate,
		)
		val invalidCert = TestDataGenerator.generateVaccineCert(
			2,
			2,
			"ORG-100001699",
			"EU/1/21/1529/INVALID",
			AcceptanceCriterias.TARGET_DISEASE,
			"1119349007",
			vaccinationDate,
		)

		var result = nationalRulesVerifier.verifyVaccine(valicCert.v.first(), clock)
		assertTrue(result is CheckNationalRulesState.SUCCESS)

		result = nationalRulesVerifier.verifyVaccine(invalidCert.v.first(), clock)
		assertTrue(result is CheckNationalRulesState.INVALID && result.nationalRulesError == NationalRulesError.NO_VALID_PRODUCT)
	}

	@Test
	fun test2of2VaccineIsValidToday() {
		val clock = Clock.fixed(Instant.parse("2021-05-25T12:00:00Z"), ZoneId.systemDefault())
		val vaccinationDate = LocalDate.now(clock)

		val cert = TestDataGenerator.generateVaccineCert(
			2,
			2,
			"ORG-100001699",
			"EU/1/21/1529",
			AcceptanceCriterias.TARGET_DISEASE,
			"J07BX03",
			vaccinationDate,
		)

		val result = nationalRulesVerifier.verifyVaccine(cert.v.first(), clock)
		assertTrue(result is CheckNationalRulesState.SUCCESS)
	}

	@Test
	fun testVaccine1of1WithPreviousInfectionsIsValidToday() {
		val clock = Clock.fixed(Instant.parse("2021-05-25T12:00:00Z"), ZoneId.systemDefault())
		val vaccinationDate = LocalDate.now(clock)

		val cert = TestDataGenerator.generateVaccineCert(
			1,
			1,
			"ORG-100001699",
			"EU/1/21/1529",
			AcceptanceCriterias.TARGET_DISEASE,
			"J07BX03",
			vaccinationDate,
		)
		// Sanity check - this vaccine usually needs 2 shots
		val acp = AcceptedVaccineProvider.getInstance(instrumentationContext)
		assertEquals(acp.getVaccineDataFromList(cert.v.first())!!.total_dosis_number, 2)

		val result = nationalRulesVerifier.verifyVaccine(cert.v.first(), clock)
		assertTrue(result is CheckNationalRulesState.SUCCESS)
	}

	@Test
	fun testVaccine1of1IsValidAfter15Days() {
		val clock = Clock.fixed(Instant.parse("2021-05-25T12:00:00Z"), ZoneId.systemDefault())
		val nowDate = LocalDate.now(clock)

		val validCert = TestDataGenerator.generateVaccineCert(
			1,
			1,
			"ORG-100001417",
			"EU/1/20/1525",
			AcceptanceCriterias.TARGET_DISEASE,
			"J07BX03",
			nowDate.minusDays(15),
		)

		var result = nationalRulesVerifier.verifyVaccine(validCert.v.first(), clock)
		assertTrue(result is CheckNationalRulesState.SUCCESS)

		val invalidCert = TestDataGenerator.generateVaccineCert(
			1,
			1,
			"ORG-100001417",
			"EU/1/20/1525",
			AcceptanceCriterias.TARGET_DISEASE,
			"J07BX03",
			nowDate.minusDays(14),
		)
		val expectedValidFrom = LocalDate.now(clock).plusDays(1).atStartOfDay()

		result = nationalRulesVerifier.verifyVaccine(invalidCert.v.first(), clock)
		assertTrue(result is CheckNationalRulesState.NOT_YET_VALID)

		result = result as CheckNationalRulesState.NOT_YET_VALID
		assertEquals(result.validityRange.validFrom!!, expectedValidFrom)
	}

	@Test
	fun testWeNeedAllShots() {
		val clock = Clock.fixed(Instant.parse("2021-05-25T12:00:00Z"), ZoneId.systemDefault())
		val nowDate = LocalDate.now(clock)

		val validCert = TestDataGenerator.generateVaccineCert(
			2,
			2,
			"ORG-100001699",
			"EU/1/21/1529",
			AcceptanceCriterias.TARGET_DISEASE,
			"J07BX03",
			nowDate,
		)
		var result = nationalRulesVerifier.verifyVaccine(validCert.v.first(), clock)
		assertTrue(result is CheckNationalRulesState.SUCCESS)

		val invalidCert = TestDataGenerator.generateVaccineCert(
			1,
			2,
			"ORG-100001699",
			"EU/1/21/1529",
			AcceptanceCriterias.TARGET_DISEASE,
			"J07BX03",
			nowDate,
		)
		result = nationalRulesVerifier.verifyVaccine(invalidCert.v.first(), clock)
		assertTrue(result is CheckNationalRulesState.INVALID)

		result = result as CheckNationalRulesState.INVALID
		assertEquals(result.nationalRulesError, NationalRulesError.NOT_FULLY_PROTECTED)
	}

	/// TEST TESTS

	@Test
	fun testTestDiseaseTargetedHasToBeSarsCoV2() {
	}

	@Test
	fun testTypeHasToBePcrOrRat() {
	}

	@Test
	fun testTestHasToBeInWhitelist() {
	}

	@Test
	fun testPcrIsValidFor72h() {
	}

	@Test
	fun testRatIsValidFor24h() {
	}

	@Test
	fun testTestResultHasToBeNegative() {
	}

	/// RECOVERY TESTS

	@Test
	fun testRecoveryDiseaseTargetedHasToBeSarsCoV2() {
	}

	@Test
	fun testCertificateIsValidFor180DaysAfter() {
	}

	@Test
	fun testTestIsOnlyValid10DaysAfterTestResult() {
	}
}