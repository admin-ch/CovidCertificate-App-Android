/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.detail

import android.content.Context
import ch.admin.bag.covidcertificate.common.util.LocaleUtil
import ch.admin.bag.covidcertificate.sdk.android.data.AcceptedTestProvider
import ch.admin.bag.covidcertificate.sdk.android.data.AcceptedVaccineProvider
import ch.admin.bag.covidcertificate.sdk.android.extensions.DEFAULT_DISPLAY_DATE_FORMATTER
import ch.admin.bag.covidcertificate.sdk.android.extensions.DEFAULT_DISPLAY_DATE_TIME_FORMATTER
import ch.admin.bag.covidcertificate.sdk.android.extensions.prettyPrint
import ch.admin.bag.covidcertificate.sdk.android.extensions.prettyPrintIsoDateTime
import ch.admin.bag.covidcertificate.sdk.core.extensions.getCertificateIdentifier
import ch.admin.bag.covidcertificate.sdk.core.extensions.getFormattedResultDate
import ch.admin.bag.covidcertificate.sdk.core.extensions.getFormattedSampleDate
import ch.admin.bag.covidcertificate.sdk.core.extensions.getIssuer
import ch.admin.bag.covidcertificate.sdk.core.extensions.getNumberOverTotalDose
import ch.admin.bag.covidcertificate.sdk.core.extensions.getRecoveryCountry
import ch.admin.bag.covidcertificate.sdk.core.extensions.getTestCenter
import ch.admin.bag.covidcertificate.sdk.core.extensions.getTestCountry
import ch.admin.bag.covidcertificate.sdk.core.extensions.getVaccinationCountry
import ch.admin.bag.covidcertificate.sdk.core.extensions.isNegative
import ch.admin.bag.covidcertificate.sdk.core.extensions.isNotFullyProtected
import ch.admin.bag.covidcertificate.sdk.core.extensions.isTargetDiseaseCorrect
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.DccCert
import ch.admin.bag.covidcertificate.wallet.R

class CertificateDetailItemListBuilder(val context: Context, val certificateHolder: CertificateHolder, val showEnglishVersion: Boolean = true) {
	private val showEnglishVersionForLabels = showEnglishVersion && LocaleUtil.isSystemLangNotEnglish(context)

	fun buildAll(): List<CertificateDetailItem> {
		val detailItems = ArrayList<CertificateDetailItem>()
		detailItems.addAll(buildVaccinationEntries())
		detailItems.addAll(buildRecoveryEntries())
		detailItems.addAll(buildTestEntries())
		return detailItems
	}

	private fun buildVaccinationEntries(): List<CertificateDetailItem> {
		val dccCert = certificateHolder.certificate as? DccCert ?: return emptyList()

		val detailItems = ArrayList<CertificateDetailItem>()
		val vaccinations = dccCert.vaccinations

		if (vaccinations.isNullOrEmpty()) {
			return detailItems
		}

		detailItems.add(DividerItem)
		val firstVaccine = vaccinations.first()
		if (firstVaccine.isNotFullyProtected()) {
			detailItems.add(TitleItem(R.string.wallet_certificate_type_incomplete_vaccine, showEnglishVersionForLabels))
		} else {
			detailItems.add(TitleItem(R.string.covid_certificate_vaccination_title, showEnglishVersionForLabels))
		}

		for (vaccinationEntry in vaccinations) {
			detailItems.add(DividerItem)

			detailItems.add(ValueItem(R.string.wallet_certificate_impfdosis_title, vaccinationEntry.getNumberOverTotalDose(),
				showEnglishVersionForLabels))

			// Vaccine data
			if (vaccinationEntry.isTargetDiseaseCorrect()) {
				detailItems.add(
					ValueItem(R.string.wallet_certificate_target_disease_title,
						context.getString(R.string.target_disease_name),
						showEnglishVersionForLabels)
				)
			}
			val acceptedTestProvider = AcceptedVaccineProvider.getInstance(context)
			detailItems.add(ValueItem(R.string.wallet_certificate_vaccine_prophylaxis,
				acceptedTestProvider.getProphylaxis(vaccinationEntry), showEnglishVersionForLabels))
			detailItems.add(ValueItem(R.string.wallet_certificate_impfstoff_product_name_title,
				acceptedTestProvider.getVaccineName(vaccinationEntry), showEnglishVersionForLabels))
			detailItems.add(ValueItem(R.string.wallet_certificate_impfstoff_holder,
				acceptedTestProvider.getAuthHolder(vaccinationEntry), showEnglishVersionForLabels))

			// Vaccination date + country
			detailItems.add(DividerItem)
			detailItems.add(
				ValueItem(
					R.string.wallet_certificate_vaccination_date_title,
					vaccinationEntry.vaccinationDate.prettyPrintIsoDateTime(DEFAULT_DISPLAY_DATE_FORMATTER),
					showEnglishVersionForLabels
				)
			)

			detailItems.add(
				ValueItem(R.string.wallet_certificate_vaccination_country_title,
					vaccinationEntry.getVaccinationCountry(showEnglishVersionForLabels),
					showEnglishVersionForLabels
				))

			// Issuer
			detailItems.add(DividerItem)
			detailItems.add(ValueItem(R.string.wallet_certificate_vaccination_issuer_title,
				vaccinationEntry.getIssuer(), showEnglishVersionForLabels))
			detailItems.add(ValueItem(R.string.wallet_certificate_identifier,
				vaccinationEntry.getCertificateIdentifier(),
				false))
			var issuerText: Int = R.string.wallet_certificate_date
			if (vaccinationEntry.isNotFullyProtected()) {
				issuerText = R.string.wallet_certificate_evidence_creation_date
			}
			certificateHolder.issuedAt?.prettyPrint(DEFAULT_DISPLAY_DATE_TIME_FORMATTER)?.let { dateString ->
				val dateText = context.getString(issuerText).replace("{DATE}", dateString)
				detailItems.add(ValueItemWithoutLabel(dateText))
				if (showEnglishVersionForLabels) {
					val dateTextEnglish =
						getEnglishTranslation(context, issuerText).replace("{DATE}", dateString)
					detailItems.add(ValueItemWithoutLabel(dateTextEnglish, true))
				}

			}
		}
		return detailItems
	}

	private fun buildRecoveryEntries(): List<CertificateDetailItem> {
		val dccCert = certificateHolder.certificate as? DccCert ?: return emptyList()

		val detailItems = ArrayList<CertificateDetailItem>()
		val recoveries = dccCert.pastInfections

		if (recoveries.isNullOrEmpty()) {
			return detailItems
		}

		detailItems.add(DividerItem)
		detailItems.add(TitleItem(R.string.covid_certificate_recovery_title, showEnglishVersionForLabels))

		for (recoveryEntry in recoveries) {
			detailItems.add(DividerItem)
			if (recoveryEntry.isTargetDiseaseCorrect()) {
				detailItems.add(
					ValueItem(R.string.wallet_certificate_target_disease_title,
						context.getString(R.string.target_disease_name),
						showEnglishVersionForLabels)
				)
			}

			// Recovery dates + country
			detailItems.add(
				ValueItem(
					R.string.wallet_certificate_recovery_first_positiv_result,
					recoveryEntry.dateFirstPositiveTest.prettyPrintIsoDateTime(DEFAULT_DISPLAY_DATE_FORMATTER),
					showEnglishVersionForLabels
				)
			)

			detailItems.add(ValueItem(R.string.wallet_certificate_test_land,
				recoveryEntry.getRecoveryCountry(showEnglishVersionForLabels), showEnglishVersionForLabels))

			// Issuer
			detailItems.add(DividerItem)
			detailItems.add(ValueItem(R.string.wallet_certificate_vaccination_issuer_title,
				recoveryEntry.getIssuer(),
				showEnglishVersionForLabels))
			detailItems.add(ValueItem(R.string.wallet_certificate_identifier, recoveryEntry.getCertificateIdentifier(), false))

			certificateHolder.issuedAt?.prettyPrint(DEFAULT_DISPLAY_DATE_TIME_FORMATTER)?.let { dateString ->
				val dateText = context.getString(R.string.wallet_certificate_date).replace("{DATE}", dateString)
				detailItems.add(ValueItemWithoutLabel(dateText))
				if (showEnglishVersionForLabels) {
					val dateTextEnglish =
						getEnglishTranslation(context, R.string.wallet_certificate_date).replace("{DATE}", dateString)
					detailItems.add(ValueItemWithoutLabel(dateTextEnglish, true))
				}
			}
		}
		return detailItems
	}

	private fun buildTestEntries(): List<CertificateDetailItem> {
		val dccCert = certificateHolder.certificate as? DccCert ?: return emptyList()

		val detailItems = ArrayList<CertificateDetailItem>()
		val tests = dccCert.tests

		if (tests.isNullOrEmpty()) {
			return detailItems
		}

		detailItems.add(DividerItem)
		detailItems.add(TitleItem(R.string.covid_certificate_test_title, showEnglishVersionForLabels))

		for (testEntry in tests) {
			detailItems.add(DividerItem)

			// Test result
			if (testEntry.isTargetDiseaseCorrect()) {
				detailItems.add(
					ValueItem(R.string.wallet_certificate_target_disease_title,
						context.getString(R.string.target_disease_name),
						showEnglishVersionForLabels)
				)
			}

			val resultStringId =
				if (testEntry.isNegative()) R.string.wallet_certificate_test_result_negativ else R.string.wallet_certificate_test_result_positiv
			var value = context.getString(resultStringId)
			if (showEnglishVersionForLabels) {
				value = "$value\n${getEnglishTranslation(context, resultStringId)}"
			}
			detailItems.add(ValueItem(R.string.wallet_certificate_test_result_title, value, showEnglishVersionForLabels))

			// Test details
			val acceptedTestProvider = AcceptedTestProvider.getInstance(context)
			detailItems.add(ValueItem(R.string.wallet_certificate_test_type,
				acceptedTestProvider.getTestType(testEntry),
				showEnglishVersionForLabels))
			acceptedTestProvider.getTestName(testEntry)?.let {
				detailItems.add(ValueItem(R.string.wallet_certificate_test_name, it, showEnglishVersionForLabels))
			}
			acceptedTestProvider.getManufacturesIfExists(testEntry)?.let {
				detailItems.add(ValueItem(R.string.wallet_certificate_test_holder, it, showEnglishVersionForLabels))
			}

			// Test dates + country
			detailItems.add(DividerItem)
			testEntry.getFormattedSampleDate(DEFAULT_DISPLAY_DATE_TIME_FORMATTER)?.let { sampleDate ->
				detailItems.add(
					ValueItem(
						R.string.wallet_certificate_test_sample_date_title,
						sampleDate,
						showEnglishVersionForLabels
					)
				)
			}
			testEntry.getFormattedResultDate(DEFAULT_DISPLAY_DATE_TIME_FORMATTER)?.let { resultDate ->
				detailItems.add(
					ValueItem(
						R.string.wallet_certificate_test_result_date_title,
						resultDate,
						showEnglishVersionForLabels
					)
				)
			}

			testEntry.getTestCenter()?.let { testCenter ->
				detailItems.add(ValueItem(R.string.wallet_certificate_test_done_by, testCenter, showEnglishVersionForLabels))
			}
			detailItems.add(ValueItem(R.string.wallet_certificate_test_land,
				testEntry.getTestCountry(showEnglishVersionForLabels), showEnglishVersionForLabels))

			// Issuer
			detailItems.add(DividerItem)
			detailItems.add(ValueItem(R.string.wallet_certificate_vaccination_issuer_title,
				testEntry.getIssuer(),
				showEnglishVersionForLabels))
			detailItems.add(ValueItem(R.string.wallet_certificate_identifier, testEntry.getCertificateIdentifier(), false))

			certificateHolder.issuedAt?.prettyPrint(DEFAULT_DISPLAY_DATE_TIME_FORMATTER)?.let { dateString ->
				val dateText = context.getString(R.string.wallet_certificate_date).replace("{DATE}", dateString)
				detailItems.add(ValueItemWithoutLabel(dateText))
				if (showEnglishVersionForLabels) {
					val dateTextEnglish =
						getEnglishTranslation(context, R.string.wallet_certificate_date).replace("{DATE}", dateString)
					detailItems.add(ValueItemWithoutLabel(dateTextEnglish, true))
				}
			}
		}
		return detailItems
	}

}