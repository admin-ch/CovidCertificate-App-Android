package ch.admin.bag.covidcertificate.wallet.howto

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import ch.admin.bag.covidcertificate.common.R
import ch.admin.bag.covidcertificate.common.databinding.ItemFaqQuestionBinding
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentHomeBinding
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentHowToScanBinding

class HowToScanFragment : Fragment() {

	companion object {
		fun newInstance(): HowToScanFragment = HowToScanFragment()
	}

	private var _binding: FragmentHowToScanBinding? = null
	private val binding get() = _binding!!

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentHowToScanBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		binding.howToScanToolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }
		setupExpandableItem(
			binding.howToScanQuestionBubble,
			R.string.wallet_scanner_howitworks_question1,
			R.string.wallet_scanner_howitworks_answer1
		)
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun setupExpandableItem(
		view: ItemFaqQuestionBinding,
		@StringRes question: Int,
		@StringRes answer: Int,
		isSelected: Boolean = false
	) {
		view.root.setOnClickListener {
			setupExpandableItem(view, question, answer, !isSelected)
			view.root.doOnLayout {
				binding.howToScanScrollView.smoothScrollTo(0, view.root.top)
			}
		}
		view.itemFaqQuestionTitle.setText(question)
		view.itemFaqQuestionAnswer.apply {
			setText(answer)
			isVisible = isSelected
		}
		view.itemFaqQuestionChevron.setImageResource(if (isSelected) R.drawable.ic_arrow_contract else R.drawable.ic_arrow_expand)
	}
}