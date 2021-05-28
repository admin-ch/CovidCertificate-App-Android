package ch.admin.bag.covidcertificate.wallet.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentHomeBinding
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentOnboardingIntroBinding

class OnboardingIntroFragment : Fragment() {

	companion object {
		fun newInstance(): OnboardingIntroFragment {
			return OnboardingIntroFragment()
		}
	}

	private var _binding: FragmentOnboardingIntroBinding? = null
	private val binding get() = _binding!!

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		_binding = FragmentOnboardingIntroBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		binding.onboardingContinueButton.setOnClickListener(View.OnClickListener { v: View? -> (activity as OnboardingActivity?)!!.continueToNextPage() })
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

}
