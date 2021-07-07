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

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ch.admin.bag.covidcertificate.common.config.ConfigModel
import ch.admin.bag.covidcertificate.common.config.ConfigViewModel
import ch.admin.bag.covidcertificate.common.util.UrlUtil
import ch.admin.bag.covidcertificate.common.util.setSecureFlagToBlockScreenshots
import ch.admin.bag.covidcertificate.sdk.android.CovidCertificateSdk
import ch.admin.bag.covidcertificate.verifier.data.VerifierSecureStorage
import ch.admin.bag.covidcertificate.verifier.databinding.ActivityMainBinding
import ch.admin.bag.covidcertificate.verifier.updateboarding.UpdateboardingActivity

class MainActivity : AppCompatActivity() {

	private lateinit var binding: ActivityMainBinding

	private val configViewModel by viewModels<ConfigViewModel>()
	private val secureStorage by lazy { VerifierSecureStorage.getInstance(this) }

	private var forceUpdateDialog: AlertDialog? = null

	private val updateboardingLauncher =
		registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult: ActivityResult ->
			if (activityResult.resultCode == RESULT_OK) {
				secureStorage.setCertificateLightUpdateboardingCompleted(true)
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
			val certificateLightUpdateboardingCompleted = secureStorage.getCertificateLightUpdateboardingCompleted()
			if (!certificateLightUpdateboardingCompleted) {
				val intent = Intent(this, UpdateboardingActivity::class.java)
				updateboardingLauncher.launch(intent)
			} else {
				showHomeFragment()
			}
		}

		configViewModel.configLiveData.observe(this) { config -> handleConfig(config) }
	}

	override fun onStart() {
		super.onStart()

		if (secureStorage.getCertificateLightUpdateboardingCompleted()) {
			configViewModel.loadConfig(BuildConfig.BASE_URL, BuildConfig.VERSION_NAME, BuildConfig.BUILD_TIME.toString())
			CovidCertificateSdk.refreshTrustList(lifecycleScope)
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		CovidCertificateSdk.unregisterWithLifecycle(lifecycle)
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

}