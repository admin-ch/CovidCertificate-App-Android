/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.nationalrules

import android.content.Context
import ch.admin.bag.covidcertificate.eval.CheckNationalRulesState
import ch.admin.bag.covidcertificate.eval.data.*
import ch.admin.bag.covidcertificate.eval.products.Vaccine
import ch.admin.bag.covidcertificate.eval.utils.*
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

class NationalRulesVerifier(context: Context) {

	private val acceptedVaccineProvider: AcceptedVaccineProvider = AcceptedVaccineProvider.getInstance(context)
	private val acceptedTestProvider: AcceptedTestProvider = AcceptedTestProvider.getInstance(context)

	fun verifyVaccine(
		vaccinationEntry: VaccinationEntry,
		clock: Clock = Clock.systemDefaultZone(),
	): CheckNationalRulesState {

		// tg must be sars-cov2
		if (!vaccinationEntry.isTargetDiseaseCorrect()) {
			return CheckNationalRulesState.INVALID(NationalRulesError.WRONG_DISEASE_TARGET)
		}

		// dosis number must be greater or equal to total number of dosis
		if (vaccinationEntry.doseNumber() < vaccinationEntry.totalDoses()) {
			return CheckNationalRulesState.INVALID(NationalRulesError.NOT_FULLY_PROTECTED)
		}

		// check if vaccine is in accepted product. We only check the mp now, since the same product held by different license holder should work the same -- right?
		val foundEntry: Vaccine = acceptedVaccineProvider.getVaccineDataFromList(vaccinationEntry)
			?: return CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_PRODUCT)

		val today = LocalDate.now(clock).atStartOfDay()
		val validFromDate = vaccinationEntry.validFromDate(foundEntry)
		val validUntilDate = vaccinationEntry.validUntilDate()

		if (validFromDate == null || validUntilDate == null) {
			return CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE)
		}

		if (validFromDate.isAfter(today)) {
			return CheckNationalRulesState.NOT_YET_VALID(ValidityRange(validFromDate, validUntilDate))
		}

		if (validUntilDate.isBefore(today)) {
			return CheckNationalRulesState.NOT_VALID_ANYMORE(ValidityRange(validFromDate, validUntilDate))
		}
		return CheckNationalRulesState.SUCCESS(ValidityRange(validFromDate, validUntilDate))
	}


	fun verifyTest(
		testEntry: TestEntry,
		clock: Clock = Clock.systemDefaultZone(),
	): CheckNationalRulesState {
		// tg must be sars-cov2
		if (!testEntry.isTargetDiseaseCorrect()) {
			return CheckNationalRulesState.INVALID(NationalRulesError.WRONG_DISEASE_TARGET)
		}

		if (!testEntry.isNegative()) {
			return CheckNationalRulesState.INVALID(NationalRulesError.POSITIVE_RESULT)
		}

		// test type must be RAT or PCR
		if (!acceptedTestProvider.testIsPCRorRAT(testEntry)) {
			return CheckNationalRulesState.INVALID(NationalRulesError.WRONG_TEST_TYPE)
		}

		if (!acceptedTestProvider.testIsAcceptedInEuAndCH(testEntry)) {
			return CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_PRODUCT)
		}

		val today = LocalDateTime.now(clock)
		val validFromDate = testEntry.validFromDate()
		val validUntilDate = testEntry.validUntilDate(testEntry)

		if (validFromDate == null || validUntilDate == null) {
			return CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE)
		}

		if (validFromDate.isAfter(today)) {
			return CheckNationalRulesState.NOT_YET_VALID(ValidityRange(validFromDate, validUntilDate))
		}
		if (validUntilDate.isBefore(today)) {
			return CheckNationalRulesState.NOT_VALID_ANYMORE(ValidityRange(validFromDate, validUntilDate))
		}
		return CheckNationalRulesState.SUCCESS(ValidityRange(validFromDate, validUntilDate))
	}

	fun verifyRecovery(
		recoveryEntry: RecoveryEntry,
		clock: Clock = Clock.systemDefaultZone(),
	): CheckNationalRulesState {

		// tg must be sars-cov2
		if (!recoveryEntry.isTargetDiseaseCorrect()) {
			return CheckNationalRulesState.INVALID(NationalRulesError.WRONG_DISEASE_TARGET)
		}

		val today = LocalDate.now(clock).atStartOfDay()
		val validFromDate = recoveryEntry.validFromDate()
		val validUntilDate = recoveryEntry.validUntilDate()

		if (validFromDate == null || validUntilDate == null) {
			return CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE)
		}

		if (validFromDate.isAfter(today)) {
			return CheckNationalRulesState.NOT_YET_VALID(ValidityRange(validFromDate, validUntilDate))
		}

		if (validUntilDate.isBefore(today)) {
			return CheckNationalRulesState.NOT_VALID_ANYMORE(ValidityRange(validFromDate, validUntilDate))
		}
		return CheckNationalRulesState.SUCCESS(ValidityRange(validFromDate, validUntilDate))
	}
}

