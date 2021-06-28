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
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ch.admin.bag.covidcertificate.common.config.ConfigModel
import ch.admin.bag.covidcertificate.common.config.ConfigViewModel
import ch.admin.bag.covidcertificate.common.util.UrlUtil
import ch.admin.bag.covidcertificate.common.util.setSecureFlagToBlockScreenshots
import ch.admin.bag.covidcertificate.eval.CovidCertificateSdk
import ch.admin.bag.covidcertificate.wallet.data.WalletSecureStorage
import ch.admin.bag.covidcertificate.wallet.databinding.ActivityMainBinding
import ch.admin.bag.covidcertificate.wallet.homescreen.HomeFragment
import ch.admin.bag.covidcertificate.wallet.onboarding.OnboardingActivity
import ch.admin.bag.covidcertificate.wallet.pdf.PdfViewModel

class MainActivity : AppCompatActivity() {

	companion object {
		private const val KEY_IS_INTENT_CONSUMED = "KEY_IS_INTENT_CONSUMED"
	}

	private val configViewModel by viewModels<ConfigViewModel>()
	private val deeplinkViewModel by viewModels<DeeplinkViewModel>()
	private val pdfViewModel by viewModels<PdfViewModel>()

	private lateinit var binding: ActivityMainBinding
	val secureStorage by lazy { WalletSecureStorage.getInstance(this) }

	private var forceUpdateDialog: AlertDialog? = null
	private var isIntentConsumed = false

	private val onAndUpdateBoardingLauncher =
		registerForActivityResult(StartActivityForResult()) { activityResult: ActivityResult ->
			if (activityResult.resultCode == RESULT_OK) {
				secureStorage.setOnboardingCompleted(true)
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

		window.setSecureFlagToBlockScreenshots(BuildConfig.FLAVOR)

		if (savedInstanceState == null) {
			val onboardingCompleted: Boolean = secureStorage.getOnboardingCompleted()
			if (!onboardingCompleted) {
				onAndUpdateBoardingLauncher.launch(Intent(this, OnboardingActivity::class.java))
			} else {
				showHomeFragment()
			}
		}

		configViewModel.configLiveData.observe(this) { config -> handleConfig(config) }

		CovidCertificateSdk.registerWithLifecycle(lifecycle)

		if (savedInstanceState != null) {
			isIntentConsumed = savedInstanceState.getBoolean(KEY_IS_INTENT_CONSUMED)
		}
	}

	override fun onNewIntent(intent: Intent?) {
		super.onNewIntent(intent)
		setIntent(intent)
		isIntentConsumed = false
	}

	override fun onResume() {
		super.onResume()
		checkIntentForActions()
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putBoolean(KEY_IS_INTENT_CONSUMED, isIntentConsumed)
	}

	override fun onStart() {
		super.onStart()
		configViewModel.loadConfig(BuildConfig.BASE_URL, BuildConfig.VERSION_NAME, BuildConfig.BUILD_TIME.toString())
		CovidCertificateSdk.getCertificateVerificationController().refreshTrustList(lifecycleScope)
	}

	override fun onDestroy() {
		super.onDestroy()
		CovidCertificateSdk.unregisterWithLifecycle(lifecycle)
	}

	private fun showHomeFragment() {
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
				.apply { window?.setSecureFlagToBlockScreenshots(BuildConfig.FLAVOR) }
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