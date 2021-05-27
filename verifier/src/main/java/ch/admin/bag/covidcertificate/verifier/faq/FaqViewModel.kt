package ch.admin.bag.covidcertificate.verifier.faq

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import ch.admin.bag.covidcertificate.common.config.FaqModel
import ch.admin.bag.covidcertificate.common.faq.model.Faq
import ch.admin.bag.covidcertificate.common.faq.model.Header
import ch.admin.bag.covidcertificate.common.faq.model.Question

class FaqViewModel(private val state: SavedStateHandle) : ViewModel() {

	private val STATE_KEY_FAQ = "STATE_KEY_FAQ"

	val faqItemsLiveData: LiveData<List<Faq>> = state.getLiveData(STATE_KEY_FAQ)

	fun generateFaqItems(
		faqQuestion: FaqModel?,
		faqWorks: FaqModel?,
	) {
		val itemsList = mutableListOf<Faq>()
		faqQuestion?.let { questionModel ->
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
		faqWorks?.let { worksModel ->
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
		state.set(STATE_KEY_FAQ, itemsList)
	}
}