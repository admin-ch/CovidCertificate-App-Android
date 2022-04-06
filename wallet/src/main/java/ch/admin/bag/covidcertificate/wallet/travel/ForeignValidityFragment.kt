/*
 * Copyright (c) 2022 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.travel

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.transition.TransitionManager
import ch.admin.bag.covidcertificate.common.config.ConfigModel
import ch.admin.bag.covidcertificate.common.extensions.collectWhenStarted
import ch.admin.bag.covidcertificate.common.extensions.getDrawableIdentifier
import ch.admin.bag.covidcertificate.common.util.UrlUtil
import ch.admin.bag.covidcertificate.common.views.animateBackgroundTintColor
import ch.admin.bag.covidcertificate.sdk.android.extensions.DEFAULT_DISPLAY_DATE_FORMATTER
import ch.admin.bag.covidcertificate.sdk.android.extensions.DEFAULT_DISPLAY_DATE_TIME_FORMATTER
import ch.admin.bag.covidcertificate.sdk.core.data.ErrorCodes
import ch.admin.bag.covidcertificate.sdk.core.extensions.isPositiveRatTest
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertType
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.DccCert
import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckNationalRulesState
import ch.admin.bag.covidcertificate.sdk.core.models.state.VerificationState
import ch.admin.bag.covidcertificate.wallet.CertificatesAndConfigViewModel
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.data.WalletSecureStorage
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentForeignValidityBinding
import ch.admin.bag.covidcertificate.wallet.databinding.ItemForeignRulesCheckHintBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

class ForeignValidityFragment : Fragment(R.layout.fragment_foreign_validity) {

	companion object {

		private const val ARG_CERTIFICATE = "ARG_CERTIFICATE"

		fun newInstance(certificateHolder: CertificateHolder) = ForeignValidityFragment().apply {
			arguments = bundleOf(ARG_CERTIFICATE to certificateHolder)
		}
	}

	private var _binding: FragmentForeignValidityBinding? = null
	private val binding get() = _binding!!

	private val viewModel by viewModels<ForeignValidityViewModel>()
	private val configViewModel by activityViewModels<CertificatesAndConfigViewModel>()

	private lateinit var certificateHolder: CertificateHolder
	private var useDateTime = false
	private lateinit var formatter: DateTimeFormatter

	private lateinit var walletStorage: WalletSecureStorage

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		certificateHolder = (arguments?.getSerializable(ARG_CERTIFICATE) as? CertificateHolder)
			?: throw IllegalStateException("ForeignValidityFragment created without Certificate!")
		useDateTime = certificateHolder.certType == CertType.TEST && (certificateHolder.certificate as? DccCert)?.tests?.firstOrNull()?.isPositiveRatTest() != true
		formatter = if (useDateTime) DEFAULT_DISPLAY_DATE_TIME_FORMATTER else DEFAULT_DISPLAY_DATE_FORMATTER
		viewModel.certificateHolder = certificateHolder

		walletStorage = WalletSecureStorage.getInstance(requireContext())
		restoreLastSelection()

		if (!useDateTime) {
			viewModel.setSelectedTime(LocalTime.MAX)
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentForeignValidityBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		setupCountrySelection()
		setupCheckDateSelection()
		setupVerificationState()

		binding.toolbar.setNavigationOnClickListener {
			parentFragmentManager.popBackStack()
		}

		configViewModel.configLiveData.observe(viewLifecycleOwner) { onConfigChanged(it) }
		viewLifecycleOwner.collectWhenStarted(viewModel.viewState) { onViewStateChanged(it) }
		viewLifecycleOwner.collectWhenStarted(viewModel.verificationState) { onVerificationStateChanged(it) }

		viewLifecycleOwner.collectWhenStarted(viewModel.selectedCountryCode) { countryCode ->
			val textColor = ContextCompat.getColor(requireContext(), if (countryCode != null) R.color.black else R.color.grey)
			binding.foreignValidityCountry.setTextColor(textColor)
			binding.foreignValidityCountry.text = countryCode?.let {
				getCountryNameFromCode(it) ?: it
			} ?: getString(R.string.wallet_foreign_rules_check_country_empty_label)
			walletStorage.setForeignRulesCheckSelectedCountry(countryCode)
		}

		viewLifecycleOwner.collectWhenStarted(viewModel.selectedDateTime) { dateTime ->
			binding.foreignValidityDateTime.text = dateTime.format(formatter)
			binding.foreignValidityDateTimeError.isVisible = dateTime < LocalDateTime.now().minusMinutes(5)
			walletStorage.setForeignRulesCheckSelectedDate(dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun restoreLastSelection() {
		walletStorage.getForeignRulesCheckSelectedCountry()?.let {
			viewModel.setSelectedCountry(it)
		}

		walletStorage.getForeignRulesCheckSelectedDate().takeIf { it > 0 }?.let {
			val dateTime = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
			if (dateTime > ZonedDateTime.now()) {
				viewModel.setSelectedDate(dateTime.toLocalDate())
				viewModel.setSelectedTime(dateTime.toLocalTime())
			}
		}
	}

	private fun onConfigChanged(config: ConfigModel) {
		val languageKey = getString(R.string.language_key)
		val linkText = config.getForeignRulesLinkText(languageKey)
		val linkUrl = config.getForeignRulesLinkUrl(languageKey)
		val foreignValidityHints = config.getForeignRulesHints(languageKey)

		binding.foreignValidityHintsGroup.isVisible = foreignValidityHints?.isNotEmpty() == true
		binding.foreignValidityHintsContainer.removeAllViews()
		foreignValidityHints?.forEach { hint ->
			val itemBinding = ItemForeignRulesCheckHintBinding.inflate(layoutInflater, binding.foreignValidityHintsContainer, true)
			val iconId = requireContext().getDrawableIdentifier(hint.iconAndroid)
			itemBinding.icon.setImageResource(iconId)
			itemBinding.hintText.text = hint.text
		}

		binding.foreignValidityMoreInfosGroup.isVisible = !linkText.isNullOrEmpty() && !linkUrl.isNullOrEmpty()
		binding.foreignValidityMoreInfosText.text = linkText
		binding.foreignValidityMoreInfosButton.setOnClickListener {
			linkUrl?.let { UrlUtil.openUrl(requireContext(), it) }
		}
	}

	private fun onViewStateChanged(state: ForeignValidityViewState) {
		binding.apply {
			loadingIndicator.isVisible = state is ForeignValidityViewState.LOADING
			content.isVisible = state is ForeignValidityViewState.SUCCESS
			errorContainer.isVisible = state is ForeignValidityViewState.ERROR

			if (state is ForeignValidityViewState.ERROR) {
				if (state.error.code == ErrorCodes.GENERAL_OFFLINE) {
					errorStatusMessage.text = buildSpannedString {
						bold {
							appendLine(getString(R.string.wallet_transfer_code_no_internet_title))
						}
						append(getString(R.string.wallet_foreign_rules_check_network_error_text))
					}
				} else {
					errorStatusMessage.text = buildSpannedString {
						bold {
							appendLine(getString(R.string.verifier_network_error_text))
						}
						append(getString(R.string.wallet_detail_network_error_text))
					}
				}

				errorCode.text = state.error.code

				errorRetryButton.setOnClickListener {
					viewModel.loadAvailableCountryCodes()
				}
			}
		}
	}

	private fun onVerificationStateChanged(state: VerificationState?) {
		TransitionManager.beginDelayedTransition(binding.root)

		val countryName = viewModel.selectedCountryCode.value?.let { getCountryNameFromCode(it) } ?: ""
		val checkDate = viewModel.selectedDateTime.value.format(formatter)
		when (state) {
			null -> {
				val backgroundColor = ContextCompat.getColor(requireContext(), R.color.blueish)
				val iconColor = ContextCompat.getColor(requireContext(), R.color.blue)
				binding.foreignValidityVerificationStateIcon.isVisible = true
				binding.foreignValidityVerificationStateIcon.setImageResource(R.drawable.ic_info_blue)
				binding.foreignValidityVerificationStateIcon.imageTintList = ColorStateList.valueOf(iconColor)
				binding.foreignValidityVerificationStateProgress.isVisible = false
				binding.foreignValidityVerificationState.animateBackgroundTintColor(backgroundColor)
				binding.foreignValidityVerificationState.text = buildSpannedString {
					bold {
						append(getString(R.string.wallet_foreign_rules_check_state_empty))
					}
				}
				binding.foreignValidityErrorCode.isVisible = false
			}
			is VerificationState.LOADING -> {
				val backgroundColor = ContextCompat.getColor(requireContext(), R.color.greyish)
				binding.foreignValidityVerificationStateIcon.isVisible = false
				binding.foreignValidityVerificationStateProgress.isVisible = true
				binding.foreignValidityVerificationState.animateBackgroundTintColor(backgroundColor)
				binding.foreignValidityVerificationState.setText(R.string.wallet_certificate_verifying)
				binding.foreignValidityErrorCode.isVisible = false
			}
			is VerificationState.SUCCESS -> {
				val backgroundColor = ContextCompat.getColor(requireContext(), R.color.greenish)
				val iconColor = ContextCompat.getColor(requireContext(), R.color.green)
				binding.foreignValidityVerificationStateIcon.isVisible = true
				binding.foreignValidityVerificationStateIcon.setImageResource(R.drawable.ic_check_filled)
				binding.foreignValidityVerificationStateIcon.imageTintList = ColorStateList.valueOf(iconColor)
				binding.foreignValidityVerificationStateProgress.isVisible = false
				binding.foreignValidityVerificationState.animateBackgroundTintColor(backgroundColor)
				binding.foreignValidityVerificationState.setText(R.string.wallet_foreign_rules_check_state_empty)
				binding.foreignValidityVerificationState.text = buildSpannedString {
					bold {
						appendLine(getString(R.string.wallet_foreign_rules_check_state_valid))
					}
					append(
						getString(R.string.wallet_foreign_rules_check_state_country_and_date)
							.replace("{COUNTRY}", countryName)
							.replace("{DATE}", checkDate)
					)
				}
				binding.foreignValidityErrorCode.isVisible = false
			}
			is VerificationState.INVALID -> {
				val backgroundColor = ContextCompat.getColor(requireContext(), R.color.redish)
				val iconColor = ContextCompat.getColor(requireContext(), R.color.red)
				binding.foreignValidityVerificationStateIcon.isVisible = true
				binding.foreignValidityVerificationStateIcon.setImageResource(R.drawable.ic_error)
				binding.foreignValidityVerificationStateIcon.imageTintList = ColorStateList.valueOf(iconColor)
				binding.foreignValidityVerificationStateProgress.isVisible = false
				binding.foreignValidityVerificationState.animateBackgroundTintColor(backgroundColor)
				binding.foreignValidityVerificationState.setText(R.string.wallet_foreign_rules_check_state_empty)
				binding.foreignValidityVerificationState.text = buildSpannedString {
					bold {
						appendLine(getString(R.string.wallet_foreign_rules_check_state_invalid))
					}
					append(
						getString(R.string.wallet_foreign_rules_check_state_country_and_date)
							.replace("{COUNTRY}", countryName)
							.replace("{DATE}", checkDate)
					)
				}

				val nationalRulesState = state.nationalRulesState
				if (nationalRulesState is CheckNationalRulesState.INVALID && nationalRulesState.ruleId != null) {
					binding.foreignValidityErrorCode.isVisible = true
					binding.foreignValidityErrorCode.text = nationalRulesState.ruleId
				} else {
					binding.foreignValidityErrorCode.isVisible = false
				}
			}
			is VerificationState.ERROR -> {
				val backgroundColor = ContextCompat.getColor(requireContext(), R.color.orangeish)
				val iconColor = ContextCompat.getColor(requireContext(), R.color.orange)
				binding.foreignValidityVerificationStateIcon.isVisible = true
				binding.foreignValidityVerificationStateIcon.setImageResource(R.drawable.ic_process_error)
				binding.foreignValidityVerificationStateIcon.imageTintList = ColorStateList.valueOf(iconColor)
				binding.foreignValidityVerificationStateProgress.isVisible = false
				binding.foreignValidityVerificationState.animateBackgroundTintColor(backgroundColor)
				binding.foreignValidityVerificationState.text = buildSpannedString {
					bold {
						append(getString(R.string.wallet_detail_network_error_title))
					}
				}
				binding.foreignValidityErrorCode.isVisible = true
				binding.foreignValidityErrorCode.text = state.error.code
			}
		}
	}

	private fun setupCountrySelection() {
		binding.foreignValidityCountryContainer.setOnClickListener {
			val availableCountryCodes = viewModel.availableCountryCodes.value
			val availableCountries = availableCountryCodes.associateWith { getCountryNameFromCode(it) ?: it }
			val countryNames = availableCountries.values.sorted().toTypedArray()

			val currentlySelectedCountryCode = viewModel.selectedCountryCode.value
			var selection = currentlySelectedCountryCode?.let { countryNames.indexOf(availableCountries[it]) } ?: -1

			MaterialAlertDialogBuilder(requireContext())
				.setTitle(R.string.wallet_foreign_rules_check_country_picker_title)
				.setSingleChoiceItems(countryNames, selection) { dialog, which ->
					selection = which
					val selectedCountryName = countryNames[selection]
					val selectedCountryCode = availableCountries.filterValues { it == selectedCountryName }.keys.single()
					viewModel.setSelectedCountry(selectedCountryCode)
					dialog.dismiss()
				}.show()
		}
	}

	private fun setupCheckDateSelection() {
		binding.foreignValidityDateContainer.setOnClickListener {
			showDatePicker()
		}
	}

	private fun setupVerificationState() {
		binding.foreignValidityVerificationState.setOnClickListener {
			viewModel.reverify()
		}
	}

	private fun showDatePicker() {
		val constraints = CalendarConstraints.Builder()
			.setValidator(DateValidatorPointForward.now())
			.build()

		val selectionTimestamp = viewModel.selectedDateTime.value
			.atZone(ZoneId.systemDefault())
			.toInstant()
			.toEpochMilli()

		val datePicker = MaterialDatePicker.Builder.datePicker()
			.setCalendarConstraints(constraints)
			.setSelection(selectionTimestamp)
			.build()

		datePicker.addOnPositiveButtonClickListener { date ->
			val pickedDate = Instant.ofEpochMilli(date).atZone(ZoneOffset.UTC).toLocalDate()
			viewModel.setSelectedDate(pickedDate)

			if (useDateTime) {
				showTimePicker()
			} else {
				viewModel.setSelectedTime(LocalTime.MAX)
			}
		}

		datePicker.show(parentFragmentManager, null)
	}

	private fun showTimePicker() {
		val timePicker = MaterialTimePicker.Builder()
			.setTimeFormat(TimeFormat.CLOCK_24H)
			.setHour(viewModel.selectedDateTime.value.hour)
			.setMinute(viewModel.selectedDateTime.value.minute)
			.build()

		timePicker.addOnPositiveButtonClickListener {
			val pickedTime = LocalTime.of(timePicker.hour, timePicker.minute)
			viewModel.setSelectedTime(pickedTime)
		}

		timePicker.show(parentFragmentManager, null)
	}

	private fun getCountryNameFromCode(countryCode: String): String? {
		val actualCountryCode = when (countryCode) {
			"EL" -> "GR" // EL is the ISO 639-1 code for Greece
			else -> countryCode
		}

		return Locale("", actualCountryCode).getDisplayCountry(Locale((getString(R.string.language_key)))).takeIf { it.isNotEmpty() }
	}

}