/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.homescreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import ch.admin.bag.covidcertificate.common.config.InfoBoxModel
import ch.admin.bag.covidcertificate.common.data.ConfigSecureStorage
import ch.admin.bag.covidcertificate.common.dialog.InfoDialogFragment
import ch.admin.bag.covidcertificate.common.html.BuildInfo
import ch.admin.bag.covidcertificate.common.html.HtmlFragment
import ch.admin.bag.covidcertificate.common.util.AssetUtil
import ch.admin.bag.covidcertificate.common.util.HorizontalMarginItemDecoration
import ch.admin.bag.covidcertificate.common.util.LanguageDialog
import ch.admin.bag.covidcertificate.common.util.setSecureFlagToBlockScreenshots
import ch.admin.bag.covidcertificate.eval.data.state.DecodeState
import ch.admin.bag.covidcertificate.eval.models.DccHolder
import ch.admin.bag.covidcertificate.wallet.BuildConfig
import ch.admin.bag.covidcertificate.wallet.CertificatesViewModel
import ch.admin.bag.covidcertificate.wallet.MainActivity
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.add.CertificateAddFragment
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentHomeBinding
import ch.admin.bag.covidcertificate.wallet.debug.DebugFragment
import ch.admin.bag.covidcertificate.wallet.detail.CertificateDetailFragment
import ch.admin.bag.covidcertificate.wallet.faq.WalletFaqFragment
import ch.admin.bag.covidcertificate.wallet.homescreen.pager.CertificatesPagerAdapter
import ch.admin.bag.covidcertificate.wallet.list.CertificatesListFragment
import ch.admin.bag.covidcertificate.wallet.pdf.PdfViewModel
import ch.admin.bag.covidcertificate.wallet.qr.WalletQrScanFragment
import com.google.android.material.tabs.TabLayoutMediator
import java.util.concurrent.atomic.AtomicLong

class HomeFragment : Fragment() {

	companion object {

		fun newInstance(): HomeFragment {
			return HomeFragment()
		}
	}

	private val certificatesViewModel by activityViewModels<CertificatesViewModel>()
	private val pdfViewModel by activityViewModels<PdfViewModel>()

	private var _binding: FragmentHomeBinding? = null
	private val binding get() = _binding!!

	private lateinit var certificatesAdapter: CertificatesPagerAdapter

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentHomeBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		setupButtons()
		setupPager()
		setupInfoBox()
		setupLanguage()
	}

	private fun setupLanguage() {
		binding.homescreenHeaderEmpty.languageButton.setOnClickListener {
			LanguageDialog().show(childFragmentManager, LanguageDialog.TAG)
		}
		binding.homescreenHeaderNotEmpty.languageButton.setOnClickListener {
			LanguageDialog().show(childFragmentManager, LanguageDialog.TAG)
		}
	}

	override fun onResume() {
		super.onResume()
		reloadCertificates()
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun setupButtons() {
		binding.homescreenScanButtonBig.setOnClickListener {
			parentFragmentManager.beginTransaction()
				.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
				.replace(R.id.fragment_container, WalletQrScanFragment.newInstance())
				.addToBackStack(WalletQrScanFragment::class.java.canonicalName)
				.commit()
		}
		binding.homescreenScanButtonSmall.setOnClickListener {
			parentFragmentManager.beginTransaction()
				.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
				.replace(R.id.fragment_container, WalletQrScanFragment.newInstance())
				.addToBackStack(WalletQrScanFragment::class.java.canonicalName)
				.commit()
		}
		binding.homescreenSupportButton.setOnClickListener {
			parentFragmentManager.beginTransaction()
				.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
				.replace(R.id.fragment_container, WalletFaqFragment.newInstance())
				.addToBackStack(WalletFaqFragment::class.java.canonicalName)
				.commit()
		}
		binding.homescreenListButton.setOnClickListener {
			parentFragmentManager.beginTransaction()
				.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
				.replace(R.id.fragment_container, CertificatesListFragment.newInstance())
				.addToBackStack(CertificatesListFragment::class.java.canonicalName)
				.commit()
		}
		val impressumClickListener = View.OnClickListener {
			val buildInfo =
				BuildInfo(
					getString(R.string.wallet_onboarding_app_title),
					BuildConfig.VERSION_NAME,
					BuildConfig.BUILD_TIME,
					BuildConfig.FLAVOR,
					getString(R.string.wallet_terms_privacy_link)
				)
			parentFragmentManager.beginTransaction()
				.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
				.replace(
					R.id.fragment_container, HtmlFragment.newInstance(
						R.string.impressum_title,
						buildInfo,
						AssetUtil.getImpressumBaseUrl(it.context),
						AssetUtil.getImpressumHtml(it.context, buildInfo),
						R.id.fragment_container
					)
				)
				.addToBackStack(HtmlFragment::class.java.canonicalName)
				.commit()
		}
		binding.homescreenHeaderEmpty.headerImpressum.setOnClickListener(impressumClickListener)
		binding.homescreenHeaderNotEmpty.headerImpressum.setOnClickListener(impressumClickListener)

		if (DebugFragment.EXISTS) {
			val lastClick = AtomicLong(0)
			val debugButtonClickListener = View.OnClickListener {
				val now = System.currentTimeMillis()
				if (lastClick.get() > now - 1000L) {
					lastClick.set(0)
					parentFragmentManager.beginTransaction()
						.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
						.replace(
							R.id.fragment_container, DebugFragment.newInstance()
						)
						.addToBackStack(DebugFragment::class.java.canonicalName)
						.commit()
				} else {
					lastClick.set(now)
				}
			}
			binding.homescreenHeaderEmpty.schwiizerchruez.setOnClickListener(debugButtonClickListener)
			binding.homescreenHeaderNotEmpty.schwiizerchruez.setOnClickListener(debugButtonClickListener)
		}
	}

	private fun setupPager() {
		val viewPager = binding.homescreenCertificatesViewPager

		val marginPagerHorizontal = resources.getDimensionPixelSize(R.dimen.certificates_padding)
		val pageTransformer = ViewPager2.PageTransformer { page: View, position: Float ->
			page.translationX = -2 * marginPagerHorizontal * position
		}
		viewPager.setPageTransformer(pageTransformer)
		viewPager.addItemDecoration(HorizontalMarginItemDecoration(requireContext(), marginPagerHorizontal))
		viewPager.apply { (getChildAt(0) as? RecyclerView)?.overScrollMode = RecyclerView.OVER_SCROLL_NEVER }

		certificatesAdapter = CertificatesPagerAdapter(this)
		viewPager.offscreenPageLimit = 1
		viewPager.adapter = certificatesAdapter
		TabLayoutMediator(binding.homescreenCertificatesTabLayout, viewPager) { tab, position -> }.attach()

		certificatesViewModel.dccHolderCollectionLiveData.observe(viewLifecycleOwner) {
			it ?: return@observe
			binding.homescreenLoadingGroup.isVisible = false
			updateHomescreen(it)
		}

		certificatesViewModel.onQrCodeClickedSingleLiveEvent.observe(this) { certificate ->
			parentFragmentManager.beginTransaction()
				.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
				.replace(R.id.fragment_container, CertificateDetailFragment.newInstance(certificate))
				.addToBackStack(CertificateDetailFragment::class.java.canonicalName)
				.commit()
		}

		pdfViewModel.pdfImportLiveData.observe(viewLifecycleOwner) { decodeState ->
			when (decodeState) {
				is DecodeState.SUCCESS -> {
					showCertificationAddFragment(decodeState.dccHolder)
					pdfViewModel.clearPdf()
				}
				is DecodeState.ERROR -> {
					AlertDialog.Builder(requireContext(), R.style.CovidCertificate_AlertDialogStyle)
						.setTitle(R.string.error_title)
						.setMessage(R.string.verifier_error_invalid_format)
						.setPositiveButton(R.string.ok_button) { dialog, _ ->
							dialog.dismiss()
						}
						.setCancelable(true)
						.create()
						.apply { window?.setSecureFlagToBlockScreenshots(BuildConfig.FLAVOR) }
						.show()
				}
			}
		}

	}

	private fun showCertificationAddFragment(dccHolder: DccHolder) {
		parentFragmentManager.beginTransaction()
			.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
			.replace(R.id.fragment_container, CertificateAddFragment.newInstance(dccHolder))
			.addToBackStack(CertificateAddFragment::class.java.canonicalName)
			.commit()
	}

	private fun reloadCertificates() {
		binding.homescreenLoadingGroup.isVisible = true
		certificatesViewModel.loadCertificates()
	}

	private fun updateHomescreen(dccHolders: List<DccHolder>) {
		val hasCertificates = dccHolders.isNotEmpty()

		binding.homescreenContentEmptyScrollView.isVisible = !hasCertificates
		binding.homescreenScanButtonBig.isVisible = !hasCertificates
		binding.homescreenScanButtonSmall.isVisible = hasCertificates
		binding.homescreenListButton.isVisible = hasCertificates
		binding.homescreenCertificatesViewPager.isVisible = hasCertificates
		binding.homescreenCertificatesTabLayout.isVisible = dccHolders.size > 1
		binding.homescreenHeaderEmpty.root.isVisible = !hasCertificates
		binding.homescreenHeaderNotEmpty.root.isVisible = hasCertificates
		binding.homescreenListButton.isVisible = dccHolders.size > 1
		certificatesAdapter.setData(dccHolders)
		if (hasCertificates) {
			binding.homescreenCertificatesViewPager.postDelayed(250) {
				if (isAdded) {
					binding.homescreenCertificatesViewPager.setCurrentItem(0, true)
				}
			}
		}
	}

	private fun setupInfoBox() {
		certificatesViewModel.configLiveData.observe(viewLifecycleOwner) { config ->
			val buttonHeaderEmpty = binding.homescreenHeaderEmpty.headerNotification
			val buttonHeaderNotEmpty = binding.homescreenHeaderNotEmpty.headerNotification
			val localizedInfo = config.getInfoBox(getString(R.string.language_key))
			val hasInfoBox = localizedInfo != null

			val onClickListener = localizedInfo?.let { infoBox ->
				val secureStorage = ConfigSecureStorage.getInstance(buttonHeaderEmpty.context)
				if (secureStorage.getLastShownInfoBoxId() != infoBox.infoId) {
					closeCurrentInfoDialog()
					showInfoDialog(infoBox)
					secureStorage.setLastShownInfoBoxId(infoBox.infoId)
				}

				return@let View.OnClickListener { view ->
					closeCurrentInfoDialog()
					showInfoDialog(infoBox)
					secureStorage.setLastShownInfoBoxId(infoBox.infoId)
				}

			}

			buttonHeaderEmpty.isVisible = hasInfoBox
			buttonHeaderEmpty.setOnClickListener(onClickListener)
			buttonHeaderNotEmpty.isVisible = hasInfoBox
			buttonHeaderNotEmpty.setOnClickListener(onClickListener)
		}
	}

	private fun closeCurrentInfoDialog() {
		(childFragmentManager.findFragmentByTag(InfoDialogFragment::class.java.canonicalName) as? InfoDialogFragment)?.dismiss()
	}

	private fun showInfoDialog(infoBox: InfoBoxModel) {
		InfoDialogFragment.newInstance(infoBox).show(childFragmentManager, InfoDialogFragment::class.java.canonicalName)
	}
}