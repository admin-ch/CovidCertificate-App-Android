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
import android.view.WindowManager
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import ch.admin.bag.covidcertificate.common.util.UrlUtil
import ch.admin.bag.covidcertificate.common.config.ConfigModel
import ch.admin.bag.covidcertificate.wallet.data.SecureStorage
import ch.admin.bag.covidcertificate.wallet.databinding.ActivityMainBinding
import ch.admin.bag.covidcertificate.wallet.homescreen.HomeFragment
import ch.admin.bag.covidcertificate.wallet.networking.ConfigRepository
import ch.admin.bag.covidcertificate.wallet.onboarding.OnboardingActivity

class MainActivity : AppCompatActivity() {

	private val certificateViewModel by viewModels<CertificatesViewModel>()

	private lateinit var binding: ActivityMainBinding
	val secureStorage by lazy { SecureStorage.getInstance(this) }

	private var forceUpdateDialog: AlertDialog? = null

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

		val allowlistedFlavours = listOf("abn", "dev")
		if (!allowlistedFlavours.contains(BuildConfig.FLAVOR)) {
			window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
		}

		if (savedInstanceState == null) {
			val onboardingCompleted: Boolean = secureStorage.getOnboardingCompleted()
			if (!onboardingCompleted) {
				onAndUpdateBoardingLauncher.launch(Intent(this, OnboardingActivity::class.java))
			} else {
				showHomeFragment()
			}
		}

		certificateViewModel.configLiveData.observe(this) { config -> handleConfig(config) }
	}

	fun showHomeFragment() {
		supportFragmentManager.beginTransaction()
			.add(R.id.fragment_container, HomeFragment.newInstance())
			.commit()
	}

	override fun onStart() {
		super.onStart()
		certificateViewModel.loadConfig()
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
}