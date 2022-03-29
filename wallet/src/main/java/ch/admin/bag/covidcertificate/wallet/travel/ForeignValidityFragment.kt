package ch.admin.bag.covidcertificate.wallet.travel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentForeignValidityBinding

class ForeignValidityFragment : Fragment(R.layout.fragment_foreign_validity) {

	companion object {

		fun newInstance() = ForeignValidityFragment()
	}

	private var _binding: FragmentForeignValidityBinding? = null
	private val binding get() = _binding!!

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentForeignValidityBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		binding.toolbar.setNavigationOnClickListener {
			parentFragmentManager.popBackStack()
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}
}