package ch.admin.bag.covidcertificate.wallet.faq

import androidx.fragment.app.activityViewModels
import ch.admin.bag.covidcertificate.common.faq.FaqFragment
import ch.admin.bag.covidcertificate.wallet.CertificatesViewModel
import ch.admin.bag.covidcertificate.wallet.R


class WalletFaqFragment : FaqFragment() {

	companion object {
		fun newInstance(): WalletFaqFragment = WalletFaqFragment()
	}

	private val certificatesViewModel by activityViewModels<CertificatesViewModel>()

	override fun setupFaqProvider() {
		certificatesViewModel.configLiveData.observe(viewLifecycleOwner, { config ->
			val languageKey = getString(R.string.language_key)
			setupFaqList(config.generateFaqItems(languageKey))
		})
		certificatesViewModel.loadConfig()
	}

}