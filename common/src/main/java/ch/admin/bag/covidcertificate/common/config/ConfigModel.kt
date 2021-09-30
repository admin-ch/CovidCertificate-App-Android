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
	val vaccinationBookingCantons: Map<String, List<VaccinationBookingCantonModel>>?,
	val vaccinationBookingInfo: Map<String, VaccinationBookingInfoModel>?,
	val showVaccinationHintHomescreen: Boolean?,
	val showVaccinationHintDetail: Boolean?,
	val showVaccinationHintTransfer: Boolean?,

) {
	fun getInfoBox(languageKey: String?): InfoBoxModel? = infoBox?.get(languageKey)
	fun getQuestionsFaqs(languageKey: String): FaqModel? = questions?.get(languageKey)
	fun getWorksFaqs(languageKey: String): FaqModel? = works?.get(languageKey)
	fun getTransferQuestionsFaqs(languageKey: String): FaqModel? = transferQuestions?.get(languageKey)
	fun getTransferWorksFaqs(languageKey: String): FaqModel? = transferWorks?.get(languageKey)
	fun getVaccinationHints(languageKey: String): List<VaccinationHintModel>? = vaccinationHints?.get(languageKey)
	fun getVaccinationBookingCantons(languageKey: String): List<VaccinationBookingCantonModel>? = vaccinationBookingCantons?.get(languageKey)
	fun getVaccinationBookingInfo(languageKey: String): VaccinationBookingInfoModel? = vaccinationBookingInfo?.get(languageKey)

	fun generateFaqItems(languageKey: String) : List<Faq> {
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