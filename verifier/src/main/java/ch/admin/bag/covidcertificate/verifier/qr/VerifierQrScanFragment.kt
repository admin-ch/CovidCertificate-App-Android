package ch.admin.bag.covidcertificate.verifier.qr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ch.admin.bag.covidcertificate.common.qr.QrScanFragment
import ch.admin.bag.covidcertificate.eval.models.Bagdgc
import ch.admin.bag.covidcertificate.verifier.R
import ch.admin.bag.covidcertificate.verifier.databinding.FragmentQrScanBinding
import ch.admin.bag.covidcertificate.verifier.verification.VerificationFragment


class VerifierQrScanFragment : QrScanFragment() {

	companion object {
		val TAG = VerifierQrScanFragment::class.java.canonicalName

		fun newInstance(): VerifierQrScanFragment {
			return VerifierQrScanFragment()
		}

	}

	private lateinit var binding: FragmentQrScanBinding

	override val viewFinderErrorColor: Int = R.color.red_error_qr_verifier
	override val viewFinderColor: Int = R.color.white
	override val torchOnDrawable: Int = R.drawable.ic_light_on_black
	override val torchOffDrawable: Int = R.drawable.ic_light_off

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		super.onCreateView(inflater, container, savedInstanceState)

		binding = FragmentQrScanBinding.inflate(inflater, container, false)

		toolbar = binding.fragmentQrScannerToolbar
		barcodeScanner = binding.barcodeScanner
		flashButton = binding.fragmentQrScannerFlashButton
		errorView = binding.fragmentQrScannerErrorView

		invalidCodeText = binding.qrCodeScannerInvalidCodeText
		viewFinderTopLeftIndicator = binding.qrCodeScannerTopLeftIndicator
		viewFinderTopRightIndicator = binding.qrCodeScannerTopRightIndicator
		viewFinderBottomLeftIndicator = binding.qrCodeScannerBottomLeftIndicator
		viewFinderBottomRightIndicator = binding.qrCodeScannerBottomRightIndicator

		return binding.root
	}

	override fun onDecodeSuccess(dgc: Bagdgc) = showVerificationFragment(dgc)

	private fun showVerificationFragment(bagdgc: Bagdgc) {
		parentFragmentManager.beginTransaction()
			.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
			.replace(R.id.fragment_container, VerificationFragment.newInstance(bagdgc))
			.addToBackStack(VerificationFragment::class.java.canonicalName)
			.commit()
	}

}

