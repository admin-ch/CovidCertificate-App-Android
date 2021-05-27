package ch.admin.bag.covidcertificate.verifier.faq

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import ch.admin.bag.covidcertificate.common.config.ConfigModel
import ch.admin.bag.covidcertificate.common.faq.FaqAdapter
import ch.admin.bag.covidcertificate.common.faq.OnUrlClickListener
import ch.admin.bag.covidcertificate.common.faq.model.Faq
import ch.admin.bag.covidcertificate.common.util.UrlUtil
import ch.admin.bag.covidcertificate.common.views.hideAnimated
import ch.admin.bag.covidcertificate.verifier.R
import ch.admin.bag.covidcertificate.verifier.VerifierViewModel
import ch.admin.bag.covidcertificate.verifier.databinding.FragmentFaqBinding


class FaqFragment : Fragment() {

	companion object {
		fun newInstance(): FaqFragment = FaqFragment()
	}

	private val verifierViewModel by activityViewModels<VerifierViewModel>()
	private val faqViewModel by viewModels<FaqViewModel>()

	private lateinit var binding: FragmentFaqBinding

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		binding = FragmentFaqBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		binding.certificatesOverviewToolbar.setNavigationOnClickListener {
			parentFragmentManager.popBackStack()
		}

		faqViewModel.faqItemsLiveData.observe(viewLifecycleOwner) { items -> setupFaqList(items) }

		if (savedInstanceState == null) {
			verifierViewModel.configLiveData.observe(viewLifecycleOwner, object : Observer<ConfigModel> {
				override fun onChanged(config: ConfigModel) {
					verifierViewModel.configLiveData.removeObserver(this)
					val languageKey = getString(R.string.language_key)
					faqViewModel.generateFaqItems(
						config.getQuestionsFaqs(languageKey),
						config.getWorksFaqs(languageKey)
					)
				}
			})
			verifierViewModel.loadConfig()
		}
	}

	private fun setupFaqList(items: List<Faq>) {
		binding.faqLoadingView.hideAnimated()

		val recyclerView = binding.faqRecyclerView
		(recyclerView.itemAnimator as SimpleItemAnimator?)?.supportsChangeAnimations = false
		val layoutManager = LinearLayoutManager(recyclerView.context, LinearLayoutManager.VERTICAL, false)
		recyclerView.layoutManager = layoutManager
		val adapter = FaqAdapter(onItemClickListener = object : OnUrlClickListener {
			override fun onLinkClicked(url: String) {
				context?.let { UrlUtil.openUrl(it, url) }
			}

		})
		recyclerView.adapter = adapter
		adapter.setItems(items)
	}
}