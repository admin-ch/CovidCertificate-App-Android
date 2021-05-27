package ch.admin.bag.covidcertificate.wallet.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ch.admin.bag.covidcertificate.common.R
import ch.admin.bag.covidcertificate.common.util.UrlUtil
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentOnboardingAgbBinding

class OnboardingAgbFragment : Fragment() {

	companion object {
		fun newInstance(): OnboardingAgbFragment {
			return OnboardingAgbFragment()
		}
	}

	private lateinit var binding: FragmentOnboardingAgbBinding

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		binding = FragmentOnboardingAgbBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		binding.onboardingContinueButton.setOnClickListener {
			(requireActivity() as OnboardingActivity).continueToNextPage()
		}

		binding.itemAgbLink.setOnClickListener { v ->
			val url = v.context.getString(R.string.wallet_terms_privacy_link)
			UrlUtil.openUrl(v.context, url)
		}
	}

}
