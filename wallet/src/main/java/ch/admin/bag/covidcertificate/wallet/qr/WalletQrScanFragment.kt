package ch.admin.bag.covidcertificate.wallet.qr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ch.admin.bag.covidcertificate.common.qr.QrScanFragment
import ch.admin.bag.covidcertificate.eval.models.Bagdgc
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.add.CertificateAddFragment
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentQrScanBinding
import ch.admin.bag.covidcertificate.wallet.howto.HowToScanFragment


class WalletQrScanFragment : QrScanFragment() {

	companion object {
		val TAG = WalletQrScanFragment::class.java.canonicalName

		fun newInstance(): WalletQrScanFragment {
			return WalletQrScanFragment()
		}
	}

	private var _binding: FragmentQrScanBinding? = null
	private val binding get() = _binding!!

	override val viewFinderErrorColor: Int = R.color.red_error_qr_wallet
	override val viewFinderColor: Int = R.color.blue
	override val torchOnDrawable: Int = R.drawable.ic_light_on
	override val torchOffDrawable: Int = R.drawable.ic_light_off_blue

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		super.onCreateView(inflater, container, savedInstanceState)

		_binding = FragmentQrScanBinding.inflate(inflater, container, false)

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

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		binding.qrCodeScannerButtonHow.setOnClickListener { showHowToScanFragment() }
	}

	override fun onDecodeSuccess(dgc: Bagdgc) = showCertificationAddFragment(dgc)

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun showCertificationAddFragment(certificate: Bagdgc) {
		parentFragmentManager.beginTransaction()
			.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
			.replace(R.id.fragment_container, CertificateAddFragment.newInstance(certificate))
			.addToBackStack(CertificateAddFragment::class.java.canonicalName)
			.commit()
	}

	private fun showHowToScanFragment() {
		parentFragmentManager.beginTransaction()
			.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
			.replace(R.id.fragment_container, HowToScanFragment.newInstance())
			.addToBackStack(HowToScanFragment::class.java.canonicalName)
			.commit()
	}

}