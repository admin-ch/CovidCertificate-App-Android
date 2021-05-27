package ch.admin.bag.covidcertificate.verifier.pager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import ch.admin.bag.covidcertificate.verifier.R
import ch.admin.bag.covidcertificate.verifier.databinding.FragmentHomeScreenPagerBinding

class HomescreenPagerFragment : Fragment() {

	companion object {
		private const val ARG_POSITION = "ARG_POSITION"

		fun getInstance(position: Int) = HomescreenPagerFragment().apply {
			arguments = bundleOf(ARG_POSITION to position)
		}

		fun getDescriptions(): List<Int> {
			val strings = mutableListOf<Int>()
			strings.add(R.string.verifier_homescreen_pager_description_1)
			strings.add(R.string.verifier_homescreen_pager_description_2)
			return strings
		}
	}

	private lateinit var binding: FragmentHomeScreenPagerBinding

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		binding = FragmentHomeScreenPagerBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		val position = requireArguments().getInt(ARG_POSITION)
		val homescreenPagerDescriptions: List<Int> = getDescriptions()
		val homescreenImages = getImageAssets()
		with(binding) {
			homescreenImage.setImageResource(homescreenImages[position])
			homescreenDescription.setText(homescreenPagerDescriptions[position])
		}
	}

	private fun getImageAssets(): List<Int> {
		val images = mutableListOf<Int>()
		images.add(R.drawable.ic_illu_home_1)
		images.add(R.drawable.ic_illu_home_2)
		return images
	}
}