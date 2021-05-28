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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ch.admin.bag.covidcertificate.common.config.ConfigModel
import ch.admin.bag.covidcertificate.verifier.networking.ConfigRepository
import kotlinx.coroutines.launch

class VerifierViewModel(application: Application) : AndroidViewModel(application) {

	private val configMutableLiveData = MutableLiveData<ConfigModel>()
	val configLiveData: LiveData<ConfigModel> = configMutableLiveData

	fun loadConfig() {
		val configRepository = ConfigRepository.getInstance(getApplication())
		viewModelScope.launch {
			configRepository.loadConfig(getApplication())?.let { config -> configMutableLiveData.postValue(config) }
		}
	}

}