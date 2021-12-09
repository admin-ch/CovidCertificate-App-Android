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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import ch.admin.bag.covidcertificate.common.config.CheckModeInfoModelWithId
import ch.admin.bag.covidcertificate.common.config.ConfigViewModel
import ch.admin.bag.covidcertificate.sdk.android.CovidCertificateSdk
import ch.admin.bag.covidcertificate.verifier.data.VerifierSecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ModesAndConfigViewModel(application: Application) : ConfigViewModel(application) {

	val verifierSecureStorage = VerifierSecureStorage.getInstance(application)

	private val modesMutableLiveData = MutableLiveData<List<CheckModeInfoModelWithId>>()
	val modesLiveData: LiveData<List<CheckModeInfoModelWithId>> = modesMutableLiveData

	private val selectedModeMutableLiveData = MutableLiveData<String?>()
	val selectedModeLiveData: LiveData<String?> = selectedModeMutableLiveData

	private val isSingleModeMutableLiveData = MutableLiveData(false)
	val isSingleModeLiveData: LiveData<Boolean> = isSingleModeMutableLiveData

	init {
		selectedModeMutableLiveData.value = verifierSecureStorage.getSelectedMode()
	}

	fun loadActiveModes(langKey: String) {
		viewModelScope.launch(Dispatchers.Main) {
			CovidCertificateSdk.Verifier.getActiveModes().combine(configLiveData.asFlow()) { activeModes, config ->
				val configModeItems = mutableListOf<CheckModeInfoModelWithId>()
				for (mode in activeModes) {
					val configItem = config?.getCheckModesInfos(langKey)?.get(mode.id)
					if (configItem != null) {
						configModeItems.add(
							CheckModeInfoModelWithId(
								mode.id,
								configItem.title,
								configItem.hexColor,
								configItem.infos
							)
						)
					} else {
						configModeItems.add(
							CheckModeInfoModelWithId(
								mode.id,
								mode.displayName,
								"#888888",
								listOf()
							)
						)
					}
				}
				configModeItems
			}.collect {
				modesMutableLiveData.value = it
				isSingleModeMutableLiveData.value = it.size == 1
				if (getSelectedMode() == null) {
					setSelectedMode(null)
				}
			}
		}
	}

	fun setSelectedMode(mode: String?) {
		selectedModeMutableLiveData.value = mode
		verifierSecureStorage.setSelectedMode(mode)
	}

	fun getSelectedMode(): CheckModeInfoModelWithId? =
		modesLiveData.value?.firstOrNull { it.id == selectedModeLiveData.value }

	fun resetSelectedModeIfNeeded() {
		if (verifierSecureStorage.resetSelectedModeIfNeeded(
				(configLiveData.value?.checkModeReselectAfterHours ?: 48) * 60 * 60 * 1000L
			)
		) {
			selectedModeMutableLiveData.value = null
		}
	}

}