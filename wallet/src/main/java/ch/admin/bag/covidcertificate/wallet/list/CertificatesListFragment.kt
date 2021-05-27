package ch.admin.bag.covidcertificate.wallet.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import ch.admin.bag.covidcertificate.common.views.hideAnimated
import ch.admin.bag.covidcertificate.wallet.CertificatesViewModel
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentCertificatesListBinding
import ch.admin.bag.covidcertificate.wallet.detail.CertificateDetailFragment

class CertificatesListFragment : Fragment() {

	companion object {
		fun newInstance(): CertificatesListFragment = CertificatesListFragment()
	}

	private val certificatesViewModel by activityViewModels<CertificatesViewModel>()

	private lateinit var binding: FragmentCertificatesListBinding

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		binding = FragmentCertificatesListBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		binding.certificatesOverviewToolbar.setNavigationOnClickListener { v: View? ->
			parentFragmentManager.popBackStack()
		}

		setupRecyclerView()
	}

	private fun setupRecyclerView() {
		val recyclerView = binding.certificatesOverviewRecyclerView
		recyclerView.layoutManager = LinearLayoutManager(recyclerView.context, LinearLayoutManager.VERTICAL, false)
		val itemTouchHelper = CertificatesListTouchHelper()
		itemTouchHelper.attachToRecyclerView(recyclerView)
		val adapter = CertificatesListAdapter({ certificate ->
			parentFragmentManager.beginTransaction()
				.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
				.replace(R.id.fragment_container, CertificateDetailFragment.newInstance(certificate))
				.addToBackStack(CertificateDetailFragment::class.java.canonicalName)
				.commit()
		}, { from, to ->
			certificatesViewModel.moveCertificate(from, to)
		}, { viewHolder ->
			itemTouchHelper.startDrag(viewHolder)
		})
		recyclerView.adapter = adapter

		binding.certificatesOverviewLoadingGroup.isVisible = true

		certificatesViewModel.verifiedCertificatesLiveData.observe(viewLifecycleOwner) { verifiedCertificates ->
			if (verifiedCertificates.isEmpty()) {
				parentFragmentManager.popBackStack()
			}
			binding.certificatesOverviewLoadingGroup.hideAnimated()
			adapter.setItems(verifiedCertificates.map { VerifiedCeritificateItem(it) })
		}
		certificatesViewModel.certificateVerifierMapLiveData.observe(viewLifecycleOwner) {
			it.forEach { (certificate, verifier) -> verifier.startVerification() }
		}

		certificatesViewModel.loadCertificates()
	}

}