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
import ch.admin.bag.covidcertificate.common.util.DEFAULT_DISPLAY_DATE_FORMATTER
import ch.admin.bag.covidcertificate.common.util.DEFAULT_DISPLAY_DATE_FORMAT_FULL_MONTH
import ch.admin.bag.covidcertificate.common.util.DEFAULT_DISPLAY_DATE_TIME_FORMATTER
import ch.admin.bag.covidcertificate.eval.data.AcceptedTestProvider
import ch.admin.bag.covidcertificate.eval.data.AcceptedVaccineProvider
import ch.admin.bag.covidcertificate.eval.models.DccHolder
import ch.admin.bag.covidcertificate.eval.utils.*
import ch.admin.bag.covidcertificate.wallet.R

class CertificateDetailItemListBuilder(val context: Context, val dccHolder: DccHolder) {

	fun buildAll(): List<CertificateDetailItem> {
		val detailItems = ArrayList<CertificateDetailItem>()
		detailItems.addAll(buildVaccinationEntries())
		detailItems.addAll(buildRecoveryEntries())
		detailItems.addAll(buildTestEntries())
		return detailItems
	}

	private fun buildVaccinationEntries(): List<CertificateDetailItem> {
		val detailItems = ArrayList<CertificateDetailItem>()
		val vaccinations = dccHolder.euDGC.v

		if (vaccinations.isNullOrEmpty()) {
			return detailItems
		}

		detailItems.add(DividerItem)
		detailItems.add(TitleItem(R.string.covid_certificate_vaccination_title))

		for (vaccinationEntry in vaccinations) {
			detailItems.add(DividerItem)

			detailItems.add(TitleStatusItem(R.string.wallet_certificate_impfdosis_title, vaccinationEntry.getNumberOverTotalDose()))

			// Vaccine data
			if (vaccinationEntry.isTargetDiseaseCorrect()) {
				detailItems.add(
					ValueItem(R.string.wallet_certificate_target_disease_title, context.getString(R.string.target_disease_name))
				)
			}
			val acceptedTestProvider = AcceptedVaccineProvider.getInstance(context)
			detailItems.add(ValueItem(R.string.wallet_certificate_vaccine_prophylaxis,
				acceptedTestProvider.getProphylaxis(vaccinationEntry)))
			detailItems.add(ValueItem(R.string.wallet_certificate_impfstoff_product_name_title,
				acceptedTestProvider.getVaccineName(vaccinationEntry)))
			detailItems.add(ValueItem(R.string.wallet_certificate_impfstoff_holder,
				acceptedTestProvider.getAuthHolder(vaccinationEntry)))

			// Vaccination date + country
			detailItems.add(DividerItem)
			vaccinationEntry.getFormattedVaccinationDate(DEFAULT_DISPLAY_DATE_FORMATTER)?.let { vaccineDate ->
				detailItems.add(
					ValueItem(
						R.string.wallet_certificate_vaccination_date_title,
						vaccineDate
					)
				)
			}

			detailItems.add(
				ValueItem(R.string.wallet_certificate_vaccination_country_title, vaccinationEntry.getVaccinationCountry())
			)

			// Issuer
			detailItems.add(DividerItem)
			detailItems.add(ValueItem(R.string.wallet_certificate_vaccination_issuer_title, vaccinationEntry.getIssuer()))
			detailItems.add(ValueItem(R.string.wallet_certificate_identifier, vaccinationEntry.getCertificateIdentifier()))
			dccHolder.issuedAt?.formatAsString(DEFAULT_DISPLAY_DATE_TIME_FORMATTER)?.let { dateString ->
				val dateText = context.getString(R.string.wallet_certificate_date).replace("{DATE}", dateString)
				detailItems.add(ValueItemWithoutLabel(dateText))
			}
		}
		return detailItems
	}

	private fun buildRecoveryEntries(): List<CertificateDetailItem> {
		val detailItems = ArrayList<CertificateDetailItem>()
		val recoveries = dccHolder.euDGC.r

		if (recoveries.isNullOrEmpty()) {
			return detailItems
		}

		detailItems.add(DividerItem)
		detailItems.add(TitleItem(R.string.covid_certificate_recovery_title))

		for (recoveryEntry in recoveries) {
			detailItems.add(DividerItem)
			if (recoveryEntry.isTargetDiseaseCorrect()) {
				detailItems.add(
					ValueItem(R.string.wallet_certificate_target_disease_title, context.getString(R.string.target_disease_name))
				)
			}

			// Recovery dates + country
			recoveryEntry.dateFormattedOfFirstPostiveResult(DEFAULT_DISPLAY_DATE_FORMAT_FULL_MONTH)?.let { firstPostiveResult ->
				detailItems.add(
					ValueItem(
						R.string.wallet_certificate_recovery_first_positiv_result,
						firstPostiveResult
					)
				)
			}

			detailItems.add(DividerItem)
			detailItems.add(ValueItem(R.string.wallet_certificate_test_land, recoveryEntry.getRecoveryCountry()))

			// Issuer
			detailItems.add(DividerItem)
			detailItems.add(ValueItem(R.string.wallet_certificate_vaccination_issuer_title, recoveryEntry.getIssuer()))
			detailItems.add(ValueItem(R.string.wallet_certificate_identifier, recoveryEntry.getCertificateIdentifier()))

			dccHolder.issuedAt?.formatAsString(DEFAULT_DISPLAY_DATE_TIME_FORMATTER)?.let { dateString ->
				val dateText = context.getString(R.string.wallet_certificate_date).replace("{DATE}", dateString)
				detailItems.add(ValueItemWithoutLabel(dateText))
			}
		}
		return detailItems
	}

	private fun buildTestEntries(): List<CertificateDetailItem> {
		val detailItems = ArrayList<CertificateDetailItem>()
		val tests = dccHolder.euDGC.t

		if (tests.isNullOrEmpty()) {
			return detailItems
		}

		detailItems.add(DividerItem)
		detailItems.add(TitleItem(R.string.covid_certificate_test_title))

		for (testEntry in tests) {
			detailItems.add(DividerItem)

			// Test result
			if (testEntry.isTargetDiseaseCorrect()) {
				detailItems.add(
					ValueItem(R.string.wallet_certificate_target_disease_title, context.getString(R.string.target_disease_name))
				)
			}

			val resultStringId =
				if (testEntry.isNegative()) R.string.wallet_certificate_test_result_negativ else R.string.wallet_certificate_test_result_positiv
			detailItems.add(ValueItem(R.string.wallet_certificate_test_result_title, context.getString(resultStringId)))

			// Test details
			val acceptedTestProvider = AcceptedTestProvider.getInstance(context)
			detailItems.add(ValueItem(R.string.wallet_certificate_test_type, acceptedTestProvider.getTestType(testEntry)))
			acceptedTestProvider.getTestName(testEntry)?.let {
				detailItems.add(ValueItem(R.string.wallet_certificate_test_name, it))
			}
			acceptedTestProvider.getManufacturesIfExists(testEntry)?.let {
				detailItems.add(ValueItem(R.string.wallet_certificate_test_holder, it))
			}

			// Test dates + country
			detailItems.add(DividerItem)
			testEntry.getFormattedSampleDate(DEFAULT_DISPLAY_DATE_TIME_FORMATTER)?.let { sampeDate ->
				detailItems.add(
					ValueItem(
						R.string.wallet_certificate_test_sample_date_title,
						sampeDate
					)
				)
			}
			testEntry.getFormattedResultDate(DEFAULT_DISPLAY_DATE_TIME_FORMATTER)?.let { resultDate ->
				detailItems.add(
					ValueItem(
						R.string.wallet_certificate_test_result_date_title,
						resultDate
					)
				)
			}

			testEntry.getTestCenter()?.let { testCenter ->
				detailItems.add(ValueItem(R.string.wallet_certificate_test_done_by, testCenter))
			}
			detailItems.add(ValueItem(R.string.wallet_certificate_test_land, testEntry.getTestCountry()))

			// Issuer
			detailItems.add(DividerItem)
			detailItems.add(ValueItem(R.string.wallet_certificate_vaccination_issuer_title, testEntry.getIssuer()))
			detailItems.add(ValueItem(R.string.wallet_certificate_identifier, testEntry.getCertificateIdentifier()))

			dccHolder.issuedAt?.formatAsString(DEFAULT_DISPLAY_DATE_TIME_FORMATTER)?.let { dateString ->
				val dateText = context.getString(R.string.wallet_certificate_date).replace("{DATE}", dateString)
				detailItems.add(ValueItemWithoutLabel(dateText))
			}
		}
		return detailItems
	}

}