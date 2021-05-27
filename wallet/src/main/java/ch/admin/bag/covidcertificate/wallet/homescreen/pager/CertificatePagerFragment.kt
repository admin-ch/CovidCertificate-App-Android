package ch.admin.bag.covidcertificate.wallet.homescreen.pager

import android.content.res.ColorStateList
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import ch.admin.bag.covidcertificate.common.util.DEFAULT_DISPLAY_DATE_FORMATTER
import ch.admin.bag.covidcertificate.common.util.parseIsoTimeAndFormat
import ch.admin.bag.covidcertificate.common.verification.CertificateVerifier
import ch.admin.bag.covidcertificate.common.verification.VerificationState
import ch.admin.bag.covidcertificate.eval.models.Bagdgc
import ch.admin.bag.covidcertificate.wallet.CertificatesViewModel
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentCertificatePagerBinding
import ch.admin.bag.covidcertificate.wallet.util.*

class CertificatePagerFragment : Fragment() {

	companion object {
		private const val ARG_CERTIFICATE = "ARG_CERTIFICATE"

		fun newInstance(certificate: Bagdgc) = CertificatePagerFragment().apply {
			arguments = bundleOf(ARG_CERTIFICATE to certificate)
		}
	}

	private val certificatesViewModel by activityViewModels<CertificatesViewModel>()

	private lateinit var binding: FragmentCertificatePagerBinding

	private lateinit var certificate: Bagdgc
	private lateinit var verifier: CertificateVerifier

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		certificate = arguments?.getSerializable(ARG_CERTIFICATE) as? Bagdgc
			?: throw IllegalStateException("Certificate pager fragment created without QrCode!")
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		binding = FragmentCertificatePagerBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		val qrCodeBitmap = QrCode.renderToBitmap(certificate.qrCodeData)
		val qrCodeDrawable = BitmapDrawable(resources, qrCodeBitmap).apply { isFilterBitmap = false }
		binding.certificatePageQrCode.setImageDrawable(qrCodeDrawable)

		val name = "${certificate.dgc.nam.fn} ${certificate.dgc.nam.gn}"
		binding.certificatePageName.text = name
		val dateOfBirth = certificate.dgc.dob.parseIsoTimeAndFormat(DEFAULT_DISPLAY_DATE_FORMATTER)
		binding.certificatePageBirthdate.text = dateOfBirth

		setupStatusInfo()

		binding.certificatePageMainGroup.setOnClickListener { certificatesViewModel.onQrCodeClicked(certificate) }
	}

	private fun setupStatusInfo() {
		certificatesViewModel.certificateVerifierMapLiveData.observe(
			viewLifecycleOwner,
			object : Observer<Map<String, CertificateVerifier>> {
				override fun onChanged(verifierMap: Map<String, CertificateVerifier>) {
					val verifier = verifierMap[certificate.qrCodeData] ?: return
					certificatesViewModel.certificateVerifierMapLiveData.removeObserver(this)
					this@CertificatePagerFragment.verifier = verifier
					verifier.liveData.observe(viewLifecycleOwner) { updateStatusInfo(it) }
					verifier.startVerification()
				}
			})
	}

	private fun updateStatusInfo(verificationState: VerificationState?) {
		val state = verificationState ?: return
		val context = binding.root.context

		binding.certificatePageName.setTextColor(ContextCompat.getColor(context, state.getNameDobColor()))
		binding.certificatePageBirthdate.setTextColor(ContextCompat.getColor(context, state.getNameDobColor()))
		binding.certificatePageQrCode.alpha = state.getQrAlpha()
		binding.certificatePageInfo.backgroundTintList =
			ColorStateList.valueOf(ContextCompat.getColor(context, state.getInfoBubbleColor()))
		binding.certificatePageStatusIcon.setImageResource(state.getStatusIcon())

		binding.certificatePageInfo.text = state.getStatusString(context)

		when (state) {
			is VerificationState.INVALID, is VerificationState.SUCCESS, is VerificationState.ERROR -> {
				binding.certificatePageStatusLoading.isVisible = false
				binding.certificatePageStatusIcon.isVisible = true
			}
			VerificationState.LOADING -> {
				binding.certificatePageStatusLoading.isVisible = true
				binding.certificatePageStatusIcon.isVisible = false
			}
		}
	}
}