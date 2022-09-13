/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.common.config

import ch.admin.bag.covidcertificate.common.faq.model.Faq
import ch.admin.bag.covidcertificate.common.faq.model.Header
import ch.admin.bag.covidcertificate.common.faq.model.Question
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ConfigModel(
	val forceUpdate: Boolean,
	val infoBox: Map<String, InfoBoxModel>?,
	val questions: Map<String, FaqModel>?,
	val works: Map<String, FaqModel>?,
	val transferQuestions: Map<String, FaqModel>?,
	val transferWorks: Map<String, FaqModel>?,
	val androidTransferCheckIntervalMs: Long?,
	val androidTransferCheckBackoffMs: Long?,
	val lightCertificateActive: Boolean?,
	val pdfGenerationActive: Boolean?,
	val vaccinationHints: Map<String, List<VaccinationHintModel>>?,
	val vaccinationBookingInfo: Map<String, VaccinationBookingInfoModel>?,
	val showVaccinationHintHomescreen: Boolean?,
	val showVaccinationHintDetail: Boolean?,
	val showVaccinationHintTransfer: Boolean?,
	val timeshiftDetectionEnabled: Boolean?,
	val checkModesInfos: Map<String, CheckModesInfosModel>?,
	val checkModesInfo: Map<String, CheckModesInfoModel>?,
	val checkModeReselectAfterHours: Int?,
	val lightCertDurationInHours: Int?,
	val refreshButtonDisabled: Boolean?,
	val refreshButtonInfo: Map<String, RefreshButtonInfoModel>?,
	val eolBannerInfo: Map<String, Map<String, EolBannerInfoModel>>?,
	val foreignRulesCheckEnabled: Boolean?,
	val foreignRulesLinkText: Map<String, String>?,
	val foreignRulesLinkUrl: Map<String, String>?,
	val foreignRulesHints: Map<String, List<ForeignRulesHintModel>>?,
	val showRatConversionForm: Boolean?,
	val ratConversionFormUrl: String?,
	val certRenewalInfo: Map<String, Map<CertificateRenewalType, CertificateRenewalInfoModel>>?,
	val showValidityState: Boolean?,
	val covidCertificateNewsText: Map<String, String>?,
	val infoCovidCertificateNews: Map<String, InfoCovidCertificateNews>?,
) {
	fun getInfoBox(languageKey: String?): InfoBoxModel? = infoBox?.get(languageKey)
	fun getQuestionsFaqs(languageKey: String): FaqModel? = questions?.get(languageKey)
	fun getWorksFaqs(languageKey: String): FaqModel? = works?.get(languageKey)
	fun getTransferQuestionsFaqs(languageKey: String): FaqModel? = transferQuestions?.get(languageKey)
	fun getTransferWorksFaqs(languageKey: String): FaqModel? = transferWorks?.get(languageKey)
	fun getVaccinationHints(languageKey: String): List<VaccinationHintModel>? = vaccinationHints?.get(languageKey)
	fun getCheckModesInfos(languageKey: String): Map<String, CheckModeInfoModel>? = checkModesInfos?.get(languageKey)?.infos
	fun getUnselectedModesInfos(languageKey: String): VerifierInfos? = checkModesInfos?.get(languageKey)?.unselected
	fun getRefreshButtonInfo(languageKey: String): RefreshButtonInfoModel? = refreshButtonInfo?.get(languageKey)
	fun getEolBannerInfo(languageKey: String): Map<String, EolBannerInfoModel>? = eolBannerInfo?.get(languageKey)
	fun getForeignRulesLinkText(languageKey: String): String? = foreignRulesLinkText?.get(languageKey)
	fun getForeignRulesLinkUrl(languageKey: String): String? = foreignRulesLinkUrl?.get(languageKey)
	fun getForeignRulesHints(languageKey: String): List<ForeignRulesHintModel>? = foreignRulesHints?.get(languageKey)
	fun getCertRenewalInfo(languageKey: String): Map<CertificateRenewalType, CertificateRenewalInfoModel>? =
		certRenewalInfo?.get(languageKey)

	fun getCheckModes(languageKey: String): Map<String, WalletModeModel>? = checkModesInfo?.get(languageKey)?.modes
	fun getInfoModeTitle(languageKey: String): String? = checkModesInfo?.get(languageKey)?.title

	fun getVaccinationBookingInfo(languageKey: String): VaccinationBookingInfoModel? = vaccinationBookingInfo?.get(languageKey)

	fun getCovidCertificateNewsText(languageKey: String): String? = covidCertificateNewsText?.get(languageKey)

	fun getInfoCovidCertificateNews(languageKey: String): InfoCovidCertificateNews? = infoCovidCertificateNews?.get(languageKey)

	fun generateFaqItems(languageKey: String): List<Faq> {
		val itemsList = mutableListOf<Faq>()
		getQuestionsFaqs(languageKey)?.let { questionModel ->
			val questionItems = questionModel.faqEntries
			itemsList.add(Header(questionModel.faqIconAndroid, questionModel.faqTitle, questionModel.faqSubTitle))
			questionItems?.let {
				itemsList.addAll(it.map { faqEntry ->
					Question(
						faqEntry.title,
						faqEntry.text,
						linkTitle = faqEntry.linkTitle,
						linkUrl = faqEntry.linkUrl
					)
				})
			}
		}
		getWorksFaqs(languageKey)?.let { worksModel ->
			val questionItems = worksModel.faqEntries
			itemsList.add(Header(worksModel.faqIconAndroid, worksModel.faqTitle, worksModel.faqSubTitle))
			questionItems?.let {
				itemsList.addAll(it.map { faqEntry ->
					Question(
						faqEntry.title,
						faqEntry.text,
						linkTitle = faqEntry.linkTitle,
						linkUrl = faqEntry.linkUrl
					)
				})
			}
		}
		return itemsList
	}
}