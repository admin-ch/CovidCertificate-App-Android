/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.verifier

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import ch.admin.bag.covidcertificate.common.config.InfoBoxModel
import ch.admin.bag.covidcertificate.common.dialog.InfoDialogFragment
import ch.admin.bag.covidcertificate.common.html.BuildInfo
import ch.admin.bag.covidcertificate.common.html.HtmlFragment
import ch.admin.bag.covidcertificate.common.util.AssetUtil
import ch.admin.bag.covidcertificate.verifier.data.SecureStorage
import ch.admin.bag.covidcertificate.verifier.databinding.FragmentHomeBinding
import ch.admin.bag.covidcertificate.common.faq.FaqFragment
import ch.admin.bag.covidcertificate.verifier.faq.VerifierFaqFragment
import ch.admin.bag.covidcertificate.verifier.pager.HomescreenPageAdapter
import ch.admin.bag.covidcertificate.verifier.pager.HomescreenPagerFragment
import ch.admin.bag.covidcertificate.verifier.qr.VerifierQrScanFragment
import com.google.android.material.tabs.TabLayoutMediator

class HomeFragment : Fragment() {

	companion object {
		fun newInstance(): HomeFragment {
			return HomeFragment()
		}

	}

	private val verifierViewModel by activityViewModels<VerifierViewModel>()

	private var _binding: FragmentHomeBinding? = null
	private val binding get() = _binding!!

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentHomeBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val adapter = HomescreenPageAdapter(this, HomescreenPagerFragment.getDescriptions().size)
		binding.viewPager.adapter = adapter

		binding.homescreenScanButton.setOnClickListener {
			parentFragmentManager.beginTransaction()
				.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
				.replace(R.id.fragment_container, VerifierQrScanFragment.newInstance())
				.addToBackStack(VerifierQrScanFragment::class.java.canonicalName)
				.commit()
		}

		binding.homescreenSupportButton.setOnClickListener {
			parentFragmentManager.beginTransaction()
				.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
				.replace(R.id.fragment_container, VerifierFaqFragment.newInstance())
				.addToBackStack(VerifierFaqFragment::class.java.canonicalName)
				.commit()
		}

		TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
			//Some implementation
		}.attach()

		binding.homescreenHeader.headerImpressum.setOnClickListener {
			val buildInfo =
				BuildInfo(getString(R.string.verifier_app_title),
					BuildConfig.VERSION_NAME,
					BuildConfig.BUILD_TIME,
					BuildConfig.FLAVOR,
					getString(R.string.verifier_terms_privacy_link)
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

		setupInfoBox()
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun setupInfoBox() {
		verifierViewModel.configLiveData.observe(viewLifecycleOwner) { config ->
			val notificationButton = binding.homescreenHeader.headerNotification
			val localizedInfo = config.getInfoBox(getString(R.string.language_key))
			val hasInfoBox = localizedInfo != null

			val onClickListener = localizedInfo?.let { infoBox ->
				val secureStorage = SecureStorage.getInstance(notificationButton.context)
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

			notificationButton.isVisible = hasInfoBox
			notificationButton.setOnClickListener(onClickListener)
		}
	}

	private fun closeCurrentInfoDialog() {
		(childFragmentManager.findFragmentByTag(InfoDialogFragment::class.java.canonicalName) as? InfoDialogFragment)?.dismiss()
	}

	private fun showInfoDialog(infoBox: InfoBoxModel) {
		InfoDialogFragment.newInstance(infoBox).show(childFragmentManager, InfoDialogFragment::class.java.canonicalName)
	}

}