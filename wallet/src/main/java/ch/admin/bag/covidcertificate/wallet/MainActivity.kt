/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.wallet

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import ch.admin.bag.covidcertificate.common.BaseActivity
import ch.admin.bag.covidcertificate.common.config.ConfigModel
import ch.admin.bag.covidcertificate.common.onboarding.BaseOnboardingActivity
import ch.admin.bag.covidcertificate.common.util.UrlUtil
import ch.admin.bag.covidcertificate.sdk.android.CovidCertificateSdk
import ch.admin.bag.covidcertificate.sdk.android.repository.TimeShiftDetectionConfig
import ch.admin.bag.covidcertificate.wallet.data.WalletSecureStorage
import ch.admin.bag.covidcertificate.wallet.databinding.ActivityMainBinding
import ch.admin.bag.covidcertificate.wallet.homescreen.HomeFragment
import ch.admin.bag.covidcertificate.wallet.onboarding.OnboardingActivity
import ch.admin.bag.covidcertificate.wallet.pdf.PdfViewModel

class MainActivity : BaseActivity() {

	companion object {
		private const val KEY_IS_INTENT_CONSUMED = "KEY_IS_INTENT_CONSUMED"
	}

	private val configViewModel by viewModels<CertificatesAndConfigViewModel>()
	private val deeplinkViewModel by viewModels<DeeplinkViewModel>()
	private val pdfViewModel by viewModels<PdfViewModel>()

	private lateinit var binding: ActivityMainBinding
	private val secureStorage by lazy { WalletSecureStorage.getInstance(this) }

	private var forceUpdateDialog: AlertDialog? = null
	private var isIntentConsumed = false

	private val onboardingLauncher = registerForActivityResult(StartActivityForResult()) { result ->
		if (result.resultCode == RESULT_OK) {
			secureStorage.setOnboardingCompleted(true)
			secureStorage.setCertificateLightUpdateboardingCompleted(true)
			secureStorage.setAgbUpdateboardingCompleted(true)
			secureStorage.setValidityUpdateboardingCompleted(true)

			// Load the config and trust list here because onStart ist called before the activity result and the onboarding
			// completion flags are therefore not yet set to true
			loadConfigAndTrustList()
			showHomeFragment()
		} else {
			finish()
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		binding = ActivityMainBinding.inflate(layoutInflater)
		val view = binding.root
		setContentView(view)

		if (savedInstanceState == null) {
			val onboardingCompleted = secureStorage.getOnboardingCompleted()
			val certificateLightUpdateboardingCompleted = secureStorage.getCertificateLightUpdateboardingCompleted()
			val agbUpdateboardingCompleted = secureStorage.getAgbUpdateboardingCompleted()
			val validityUpdateboardingCompleted = secureStorage.getValidityUpdateboardingCompleted()

			val onboardingType = when {
				!onboardingCompleted -> OnboardingActivity.OnboardingType.FRESH_INSTALL
				!certificateLightUpdateboardingCompleted -> OnboardingActivity.OnboardingType.CERTIFICATE_LIGHT
				!agbUpdateboardingCompleted -> OnboardingActivity.OnboardingType.AGB_UPDATE
				!validityUpdateboardingCompleted -> OnboardingActivity.OnboardingType.VALIDITY
				else -> null
			}

			if (onboardingType != null) {
				val intent = Intent(this, OnboardingActivity::class.java).apply {
					putExtra(BaseOnboardingActivity.EXTRA_ONBOARDING_TYPE, onboardingType.name)
				}
				onboardingLauncher.launch(intent)
			} else {
				showHomeFragment()
			}
		}

		configViewModel.configLiveData.observe(this) { config ->
			CovidCertificateSdk.setTimeShiftDetectingConfig(TimeShiftDetectionConfig(config.timeshiftDetectionEnabled ?: false))
			handleConfig(config)
		}
		pdfViewModel.clearPdfFiles()

		if (savedInstanceState != null) {
			isIntentConsumed = savedInstanceState.getBoolean(KEY_IS_INTENT_CONSUMED)
		}
	}

	override fun onNewIntent(intent: Intent?) {
		super.onNewIntent(intent)
		setIntent(intent)
		isIntentConsumed = false
	}

	override fun onStart() {
		super.onStart()

		// Every time the app comes into the foreground and the onboarding was completed, reload the config and trust list
		if (secureStorage.getOnboardingCompleted() && secureStorage.getCertificateLightUpdateboardingCompleted()) {
			loadConfigAndTrustList()
		}
	}

	override fun onResume() {
		super.onResume()
		checkIntentForActions()
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putBoolean(KEY_IS_INTENT_CONSUMED, isIntentConsumed)
	}

	override fun onDestroy() {
		super.onDestroy()
		CovidCertificateSdk.unregisterWithLifecycle(lifecycle)
	}

	private fun loadConfigAndTrustList() {
		configViewModel.loadConfig(BuildConfig.BASE_URL, BuildConfig.VERSION_NAME, BuildConfig.BUILD_TIME.toString())
		CovidCertificateSdk.refreshTrustList(lifecycleScope)
	}

	private fun showHomeFragment() {
		CovidCertificateSdk.registerWithLifecycle(lifecycle)

		supportFragmentManager.beginTransaction()
			.add(R.id.fragment_container, HomeFragment.newInstance())
			.commit()
	}

	private fun handleConfig(config: ConfigModel) {
		if (config.forceUpdate && forceUpdateDialog == null) {
			val forceUpdateDialog = AlertDialog.Builder(this, R.style.CovidCertificate_AlertDialogStyle)
				.setTitle(R.string.force_update_title)
				.setMessage(R.string.force_update_text)
				.setPositiveButton(R.string.force_update_button, null)
				.setCancelable(false)
				.create()
			forceUpdateDialog.setOnShowListener {
				forceUpdateDialog.getButton(DialogInterface.BUTTON_POSITIVE)
					.setOnClickListener {
						val packageName = packageName
						UrlUtil.openUrl(this@MainActivity, "market://details?id=$packageName")
					}
			}
			this.forceUpdateDialog = forceUpdateDialog
			forceUpdateDialog.show()
		}
	}

	private fun checkIntentForActions() {
		val launchedFromHistory = intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
		if (!launchedFromHistory && !isIntentConsumed) {
			isIntentConsumed = true
			when (intent.action) {
				Intent.ACTION_SEND -> {
					if ("application/pdf" == intent.type) {
						handleCertificatePDF(intent)
					}
				}
				Intent.ACTION_VIEW -> {
					handleCertificateDeeplink(intent)
				}
			}
		}
	}

	private fun handleCertificatePDF(intent: Intent) {
		if (secureStorage.getOnboardingCompleted()) {
			intent.clipData?.let { pdfViewModel.importPdf(clipData = it) }
		}
	}

	// Expect URIs of the form "covidcert://hc1:<base45>" and "hcert://hc1:<base45>"
	private fun handleCertificateDeeplink(intent: Intent) {
		val uri = intent.data ?: return
		// The base45 contains weird characters that confuse normal uri parsing (so we can't simply use uri.scheme and uri.path)
		val uriString = uri.toString()

		val prefixes = listOf("covidcert://", "hcert://")
		for (prefix in prefixes) {
			if (uriString.startsWith(prefix)) {
				val cert = uriString.substringAfter(prefix)
				deeplinkViewModel.importDeeplink(cert)
			}
		}
	}
}