package ch.admin.bag.covidcertificate.verifier.faq

import androidx.fragment.app.activityViewModels
import ch.admin.bag.covidcertificate.common.faq.FaqFragment
import ch.admin.bag.covidcertificate.verifier.R
import ch.admin.bag.covidcertificate.verifier.VerifierViewModel

class VerifierFaqFragment : FaqFragment() {

	companion object {
		fun newInstance(): FaqFragment = VerifierFaqFragment()
	}

	private val verifierViewModel by activityViewModels<VerifierViewModel>()

	override fun setupFaqProvider() {
		verifierViewModel.configLiveData.observe(viewLifecycleOwner, { config ->
			val languageKey = getString(R.string.language_key)
			setupFaqList(config.generateFaqItems(languageKey))
		})
		verifierViewModel.loadConfig()
	}
}