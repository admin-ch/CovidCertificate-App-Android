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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import ch.admin.bag.covidcertificate.common.config.ConfigModel
import ch.admin.bag.covidcertificate.common.extensions.collectWhenStarted
import ch.admin.bag.covidcertificate.common.extensions.getDrawableIdentifier
import ch.admin.bag.covidcertificate.common.util.UrlUtil
import ch.admin.bag.covidcertificate.sdk.android.extensions.DEFAULT_DISPLAY_DATE_FORMATTER
import ch.admin.bag.covidcertificate.sdk.android.extensions.DEFAULT_DISPLAY_DATE_TIME_FORMATTER
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertType
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.wallet.CertificatesAndConfigViewModel
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentForeignValidityBinding
import ch.admin.bag.covidcertificate.wallet.databinding.ItemModeListInfoBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
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

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		certificateHolder = (arguments?.getSerializable(ARG_CERTIFICATE) as? CertificateHolder)
			?: throw IllegalStateException("ForeignValidityFragment created without Certificate!")
		useDateTime = certificateHolder.certType == CertType.TEST
		formatter = if (useDateTime) DEFAULT_DISPLAY_DATE_TIME_FORMATTER else DEFAULT_DISPLAY_DATE_FORMATTER

		viewModel.certificateHolder = certificateHolder
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

		binding.toolbar.setNavigationOnClickListener {
			parentFragmentManager.popBackStack()
		}

		configViewModel.configLiveData.observe(viewLifecycleOwner) { onConfigChanged(it) }

		viewLifecycleOwner.collectWhenStarted(viewModel.selectedCountryCode) { countryCode ->
			val textColor = ContextCompat.getColor(requireContext(), if (countryCode != null) R.color.black else R.color.grey)
			binding.foreignValidityCountry.setTextColor(textColor)
			binding.foreignValidityCountry.text = countryCode?.let {
				getCountryNameFromCode(it) ?: it
			} ?: getString(R.string.wallet_foreign_rules_check_country_empty_label)
		}

		viewLifecycleOwner.collectWhenStarted(viewModel.selectedDateTime) {
			binding.foreignValidityDateTime.text = it.format(formatter)
		}

		viewLifecycleOwner.collectWhenStarted(viewModel.verificationState) {
			// TODO Update UI state
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun onConfigChanged(config: ConfigModel) {
		val foreignValidityHints = config.getForeignRulesHints(getString(R.string.language_key))
		binding.foreignValidityHintsGroup.isVisible = foreignValidityHints?.isNotEmpty() == true
		binding.foreignValidityHintsContainer.removeAllViews()
		foreignValidityHints?.forEach { hint ->
			val itemBinding = ItemModeListInfoBinding.inflate(layoutInflater, binding.foreignValidityHintsContainer, true)
			val iconId = requireContext().getDrawableIdentifier(hint.iconAndroid)
			itemBinding.modeListInfoImage.setImageResource(iconId)
			itemBinding.modeListInfoText.text = hint.text
		}

		binding.foreignValidityMoreInfosGroup.isVisible = !config.foreignRulesLinkText.isNullOrEmpty() && !config.foreignRulesLinkUrl.isNullOrEmpty()
		binding.foreignValidityMoreInfosText.text = config.foreignRulesLinkText
		binding.foreignValidityMoreInfosButton.setOnClickListener {
			config.foreignRulesLinkUrl?.let {
				UrlUtil.openUrl(requireContext(), it)
			}
		}
	}

	private fun setupCountrySelection() {
		binding.foreignValidityCountryContainer.setOnClickListener {
			val availableCountryCodes = viewModel.availableCountryCodes.value
			val countryNames = availableCountryCodes.map { getCountryNameFromCode(it) ?: it }.toTypedArray()
			var selection = viewModel.selectedCountryCode.value?.let { availableCountryCodes.indexOf(it) } ?: -1

			MaterialAlertDialogBuilder(requireContext())
				.setTitle(R.string.wallet_foreign_rules_check_country_picker_title)
				.setSingleChoiceItems(countryNames, selection) { dialog, which ->
					selection = which
					viewModel.setSelectedCountry(availableCountryCodes[selection])
					dialog.dismiss()
				}.show()
		}
	}

	private fun setupCheckDateSelection() {
		binding.foreignValidityDateContainer.setOnClickListener {
			showDatePicker()
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