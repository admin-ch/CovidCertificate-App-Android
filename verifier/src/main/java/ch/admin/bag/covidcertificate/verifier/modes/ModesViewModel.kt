/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.verifier.modes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ch.admin.bag.covidcertificate.common.config.CheckModeInfoModel
import ch.admin.bag.covidcertificate.common.net.ConfigRepository
import ch.admin.bag.covidcertificate.verifier.R
import ch.admin.bag.covidcertificate.verifier.data.VerifierSecureStorage

class ModesViewModel(application: Application) : AndroidViewModel(application) {

	val verifierSecureStorage = VerifierSecureStorage.getInstance(application)

	private val modesMutableLiveData = MutableLiveData<List<Map.Entry<String, CheckModeInfoModel>>>()
	val modesLiveData: LiveData<List<Map.Entry<String, CheckModeInfoModel>>> = modesMutableLiveData

	private val selectedModeMutableLiveData = MutableLiveData<String?>()
	val selectedModeLiveData: LiveData<String?> = selectedModeMutableLiveData

	init {
		val config = ConfigRepository.getCurrentConfig(application)
		modesMutableLiveData.value = config?.getCheckModesInfos(application.getString(R.string.language_key))?.map { it }

		selectedModeMutableLiveData.value = verifierSecureStorage.getSelectedMode(48 * 60 * 60 * 1000L)
	}

	fun setSelectedMode(mode: String?) {
		selectedModeMutableLiveData.value = mode
		verifierSecureStorage.setSelectedMode(mode)
	}

	fun getSelectedMode(): CheckModeInfoModel? =
		modesLiveData.value?.firstOrNull { it.key == selectedModeLiveData.value }?.value

}