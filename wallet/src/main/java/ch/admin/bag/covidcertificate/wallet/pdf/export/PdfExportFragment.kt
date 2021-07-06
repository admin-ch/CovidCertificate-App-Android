/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet.pdf.export

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import ch.admin.bag.covidcertificate.common.util.makeSubStringBold
import ch.admin.bag.covidcertificate.sdk.core.data.ErrorCodes
import ch.admin.bag.covidcertificate.sdk.core.extensions.fromBase64
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.wallet.R
import ch.admin.bag.covidcertificate.wallet.databinding.FragmentPdfExportBinding
import ch.admin.bag.covidcertificate.wallet.pdf.PdfViewModel
import java.io.File

class PdfExportFragment : Fragment(R.layout.fragment_pdf_export) {

	companion object {
		const val REQUEST_KEY_PDF_EXPORT = "REQUEST_KEY_PDF_EXPORT"
		const val RESULT_KEY_PDF_URI = "RESULT_KEY_PDF_URI"
		private const val ARG_CERTIFICATE_HOLDER = "ARG_CERTIFICATE_HOLDER"

		fun newInstance(certificateHolder: CertificateHolder) = PdfExportFragment().apply {
			arguments = bundleOf(
				ARG_CERTIFICATE_HOLDER to certificateHolder
			)
		}
	}

	private val viewModel by viewModels<PdfViewModel>()

	private var _binding: FragmentPdfExportBinding? = null
	private val binding get() = _binding!!

	private lateinit var certificateHolder: CertificateHolder

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		certificateHolder = (arguments?.getSerializable(ARG_CERTIFICATE_HOLDER) as? CertificateHolder)
			?: throw IllegalArgumentException("PDF export fragment created without a CertificateHolder!")
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentPdfExportBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }

		binding.certificatePdfExportButton.setOnClickListener { viewModel.exportPdf(certificateHolder) }
		binding.certificatePdfExportRetryButton.setOnClickListener { viewModel.exportPdf(certificateHolder) }

		viewModel.pdfExportState.observe(viewLifecycleOwner) { onExportStateChanged(it) }
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	@SuppressLint("SetTextI18n")
	private fun onExportStateChanged(state: PdfExportState) {
		when (state) {
			is PdfExportState.LOADING -> {
				binding.certificatePdfExportLoadingIndicator.isVisible = true
				binding.certificatePdfExportContent.isVisible = false
			}
			is PdfExportState.ERROR -> {
				binding.certificatePdfExportLoadingIndicator.isVisible = false
				binding.certificatePdfExportContent.isVisible = true
				binding.certificatePdfExportIntroLayout.isVisible = false
				binding.certificatePdfExportErrorLayout.isVisible = true
				binding.certificatePdfExportTitle.setText(R.string.wallet_certificate_export_detail_error_title)
				binding.certificatePdfExportErrorCode.text = state.error.code

				if (state.error.code == ErrorCodes.GENERAL_OFFLINE) {
					binding.certificatePdfExportStatusIcon.setImageResource(R.drawable.ic_no_connection)
					val title = getString(R.string.wallet_certificate_export_detail_network_error_title)
					val text = getString(R.string.wallet_certificate_export_detail_network_error_text)
					binding.certificatePdfExportStatusText.text = "$title\n$text".makeSubStringBold(title)
				} else {
					binding.certificatePdfExportStatusIcon.setImageResource(R.drawable.ic_process_error)
					val title = getString(R.string.wallet_certificate_export_detail_general_error_title)
					val text = getString(R.string.wallet_certificate_export_detail_general_error_text)
					binding.certificatePdfExportStatusText.text = "$title\n$text".makeSubStringBold(title)
				}
			}
			is PdfExportState.SUCCESS -> {
				val folder = File(requireContext().cacheDir, "certificate-pdfs")
				if (!folder.exists()) folder.mkdirs()

				val pdfFile = File.createTempFile("${System.currentTimeMillis()}-certificate", ".pdf", folder)
				pdfFile.writeBytes(state.pdfData.fromBase64())
				val uri = FileProvider.getUriForFile(requireContext(), "ch.admin.bag.covidcertificate.wallet.fileprovider", pdfFile)

				setFragmentResult(REQUEST_KEY_PDF_EXPORT, bundleOf(RESULT_KEY_PDF_URI to uri))
				parentFragmentManager.popBackStack()
			}
		}
	}

}