/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.verifier.verification

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import ch.admin.bag.covidcertificate.common.util.getInvalidErrorCode
import ch.admin.bag.covidcertificate.common.views.VerticalMarginItemDecoration
import ch.admin.bag.covidcertificate.sdk.android.CovidCertificateSdk
import ch.admin.bag.covidcertificate.sdk.android.models.VerifierCertificateHolder
import ch.admin.bag.covidcertificate.sdk.android.verification.state.VerifierDecodeState
import ch.admin.bag.covidcertificate.sdk.core.models.state.VerificationState
import ch.admin.bag.covidcertificate.verifier.R
import ch.admin.bag.covidcertificate.verifier.data.VerifierSecureStorage
import ch.admin.bag.covidcertificate.verifier.databinding.FragmentVerificationBinding
import ch.admin.bag.covidcertificate.verifier.zebra.ZebraActionBroadcastReceiver
import kotlin.math.max
import kotlin.math.roundToInt

class VerificationFragment : Fragment() {

	companion object {
		private val TAG = VerificationFragment::class.java.canonicalName
		private const val ARG_DECODE_DGC = "ARG_DECODE_DGC"

		const val RESULT_FRAGMENT_POPPED = "RESULT_FRAGMENT_POPPED"

		fun newInstance(certificateHolder: VerifierCertificateHolder): VerificationFragment {
			return VerificationFragment().apply {
				arguments = Bundle().apply {
					putSerializable(ARG_DECODE_DGC, certificateHolder)
				}
			}
		}
	}

	private var _binding: FragmentVerificationBinding? = null
	private val binding get() = _binding!!
	private val verificationViewModel: VerificationViewModel by viewModels()
	private val zebraBroadcastReceiver by lazy { ZebraActionBroadcastReceiver(VerifierSecureStorage.getInstance(requireContext())) }
	private var certificateHolder: VerifierCertificateHolder? = null
	private var isClosedByUser = false

	private lateinit var verificationAdapter: VerificationAdapter

	private val onBackPressedCallback = object : OnBackPressedCallback(true) {
		override fun handleOnBackPressed() {
			isClosedByUser = true
			parentFragmentManager.popBackStack()
			setFragmentResult(RESULT_FRAGMENT_POPPED, bundleOf())
			remove()
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (arguments?.containsKey(ARG_DECODE_DGC) == false) {
			return
		}

		certificateHolder = requireArguments().getSerializable(ARG_DECODE_DGC) as VerifierCertificateHolder

		requireActivity().onBackPressedDispatcher.addCallback(onBackPressedCallback)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentVerificationBinding.inflate(inflater, container, false)
		return binding.root
	}

	@SuppressLint("SetTextI18n")
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		view.doOnLayout { setupScrollBehavior() }

		verificationAdapter = VerificationAdapter {
			certificateHolder?.let {
				verificationViewModel.retryVerification(it)
			}
		}

		binding.verificationStatusRecyclerView.apply {
			layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
			adapter = verificationAdapter
			addItemDecoration(VerticalMarginItemDecoration(context, R.dimen.spacing_very_small))
		}

		binding.verificationFooterButton.setOnClickListener {
			isClosedByUser = true
			onBackPressedCallback.remove()
			parentFragmentManager.popBackStack()
			setFragmentResult(RESULT_FRAGMENT_POPPED, bundleOf())
		}

		verificationViewModel.verificationLiveData.observe(viewLifecycleOwner) {
			updateHeaderAndVerificationView(it)
		}

		verifyAndDisplayCertificateHolder()
	}

	override fun onResume() {
		super.onResume()
		zebraBroadcastReceiver.registerWith(requireContext()) { decodeQrCodeData(it) }
	}

	override fun onPause() {
		super.onPause()
		// Pop the backstack back to the QR scanner screen when the verification fragment is put into the background, unless
		// it was closed by the user (e.g. with the back or OK button)
		if (!isClosedByUser) {
			onBackPressedCallback.remove()
			parentFragmentManager.popBackStack()
			setFragmentResult(RESULT_FRAGMENT_POPPED, bundleOf())
		}
		zebraBroadcastReceiver.unregisterWith(requireContext())
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun verifyAndDisplayCertificateHolder() {
		val certificateHolder = certificateHolder ?: return
		binding.verificationScrollView.smoothScrollTo(0, 0)
		val personName = certificateHolder.getPersonName()

		if (personName.familyName.isNullOrBlank()) {
			binding.verificationFamilyName.text = personName.standardizedFamilyName
		} else {
			binding.verificationFamilyName.text = personName.familyName
		}
		if (personName.givenName.isNullOrBlank()) {
			binding.verificationGivenName.text = personName.standardizedGivenName
		} else {
			binding.verificationGivenName.text = personName.givenName
		}
		binding.verificationBirthdate.text = certificateHolder.getFormattedDateOfBirth()
		binding.verificationStandardizedNameLabel.text = "${personName.standardizedFamilyName}<<${personName.standardizedGivenName}"

		verificationViewModel.startVerification(certificateHolder)
	}

	private fun updateHeaderAndVerificationView(verificationState: VerificationState) {
		updateHeader(verificationState)
		updateStatusBubbles(verificationState)
	}

	private fun updateHeader(state: VerificationState) {
		val context = binding.root.context

		val isLoading = state == VerificationState.LOADING

		binding.verificationHeaderProgressBar.isVisible = isLoading
		binding.verificationHeaderIcon.isVisible = !isLoading
		binding.verificationHeaderIcon.setImageResource(state.getValidationStatusIconLarge())
		ColorStateList.valueOf(ContextCompat.getColor(context, state.getHeaderColor())).let { headerBackgroundTint ->
			binding.verificationBaseGroup.backgroundTintList = headerBackgroundTint
			binding.verificationContentGroup.backgroundTintList = headerBackgroundTint
			binding.verificationHeaderGroup.backgroundTintList = headerBackgroundTint
		}
	}

	private fun updateStatusBubbles(state: VerificationState) {
		val context = binding.root.context

		verificationAdapter.setItems(state.getVerificationStateItems(context))

		binding.verificationErrorCode.apply {
			visibility = View.INVISIBLE
			if (state is VerificationState.ERROR) {
				visibility = View.VISIBLE
				text = state.error.code
			} else if (state is VerificationState.INVALID) {
				val errorCode = state.getInvalidErrorCode()
				if (errorCode.isNotEmpty()) {
					visibility = View.VISIBLE
					text = errorCode
				}
			}
		}
	}

	private fun setupScrollBehavior() {
		val headerCollapseManager = HeaderCollapseManager(resources, binding)
		binding.verificationScrollView.setOnScrollChangeListener(headerCollapseManager)
	}

	private fun decodeQrCodeData(qrCodeData: String) {
		when (val decodeState = CovidCertificateSdk.Verifier.decode(qrCodeData)) {
			is VerifierDecodeState.SUCCESS -> {
				certificateHolder = decodeState.certificateHolder
				verifyAndDisplayCertificateHolder()
			}
			is VerifierDecodeState.ERROR -> {
				// Ignore errors when scanning in the details screen
			}
		}
	}

	internal class HeaderCollapseManager(resources: Resources, binding: FragmentVerificationBinding) : View.OnScrollChangeListener {

		private val root = binding.verificationBaseGroup
		private val headerGroup = binding.verificationHeaderGroup
		private val headerShadow = binding.verificationHeaderGroupShadow
		private val sheetGroup = binding.verificationSheetGroup

		private val scrollOffset = (sheetGroup.layoutParams as ViewGroup.MarginLayoutParams).topMargin
		private val minHeaderHeight = resources.getDimensionPixelSize(R.dimen.header_height_default)
		private val maxHeaderHeight = resources.getDimensionPixelSize(R.dimen.header_height_max)
		private val diffMaxMinHeaderHeight = maxHeaderHeight - minHeaderHeight
		private val headerShadowAnimRange = resources.getDimensionPixelSize(R.dimen.spacing_medium)
		private val sheetDefaultCornerRadius = resources.getDimensionPixelSize(R.dimen.corner_radius_sheet)

		override fun onScrollChange(v: View, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) {
			val scrollVertical = max(scrollY - scrollOffset, 0)

			val progressPadding = (scrollVertical / minHeaderHeight.toFloat()).coerceIn(0f, 1f)
			headerGroup.setPadding(0, ((1f - progressPadding) * minHeaderHeight).roundToInt(), 0, 0)

			val progressSize = (scrollVertical / diffMaxMinHeaderHeight.toFloat()).coerceIn(0f, 1f)
			val lp: ViewGroup.LayoutParams = headerGroup.layoutParams
			lp.height = (maxHeaderHeight - progressSize * diffMaxMinHeaderHeight).roundToInt()
			headerGroup.layoutParams = lp

			val progressHeaderShadow =
				((scrollVertical - diffMaxMinHeaderHeight) / headerShadowAnimRange.toFloat()).coerceIn(0f, 1f)
			headerShadow.alpha = progressHeaderShadow

			val progressSheetCorner = ((scrollVertical - minHeaderHeight) / scrollOffset.toFloat()).coerceIn(0f, 1f)
			val sheetCornerRadius = (1f - progressSheetCorner) * sheetDefaultCornerRadius
			(sheetGroup.background as? GradientDrawable)?.cornerRadii =
				floatArrayOf(sheetCornerRadius, sheetCornerRadius, sheetCornerRadius, sheetCornerRadius, 0f, 0f, 0f, 0f)

			root.requestLayout()
		}
	}


}