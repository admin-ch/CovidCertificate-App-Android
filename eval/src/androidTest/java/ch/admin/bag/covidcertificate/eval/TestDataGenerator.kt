/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval

import ch.admin.bag.covidcertificate.eval.data.Eudgc
import com.google.gson.Gson
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

object TestDataGenerator {

	private val gson = Gson()

	fun generateVaccineCert(
		dn: Int, // dose number
		sd: Int, // total series of doses
		ma: String, // marketing authorization holder code
		mp: String, // medical product code
		tg: String, // target disease code
		vp: String, // vaccine prophylaxis code
		vaccinationDate: LocalDateTime,
	): Eudgc {
		val vaccineJson = """
               {
                 "v": [
                   {
                     "ci": "urn:uvci:01:CH:9C595501BBC294450BD0F6E2",
                     "co": "BE",
                     "dn": $dn,
                     "dt": "${vaccinationDate.format(DateTimeFormatter.ISO_DATE)}",
                     "is": "Bundesamt für Gesundheit (BAG)",
                     "ma": "$ma",
                     "mp": "$mp",
                     "sd": $sd,
                     "tg": "$tg",
                     "vp": "$vp"
                   }
                 ],
                 "dob": "1990-12-12",
                 "nam": {
                   "fn": "asdf",
                   "gn": "asdf",
                   "fnt": "ASDF",
                   "gnt": "ASDF"
                 },
                 "ver": "1.0.0"
               }
            """
		return gson.fromJson(vaccineJson, Eudgc::class.java)!!
	}

	fun generateTestCert(
		testType: String,
		testResult: String,
		name: String,
		disease: String,
		sampleCollectionWasAgo: Duration,
	): Eudgc {
		val now = OffsetDateTime.now()
		val sampleCollectionTime = now + sampleCollectionWasAgo
		val testResultTime = sampleCollectionTime + Duration.ofHours(10)
		val testJson = """
              {
                "t": [
                  {
                    "ci": "urn:uvci:01:AT:71EE2559DE38C6BF7304FB65A1A451ECE",
                    "co": "AT",
                    "dr": "${testResultTime.format(DateTimeFormatter.ISO_DATE_TIME)}",
                    "is": "BMSGPK Austria",
                    "ma": "$name",
                    "nm": "$name",
                    "sc": "${sampleCollectionTime.format(DateTimeFormatter.ISO_DATE_TIME)}",
                    "tc": "Testing center Vienna 1",
                    "tg": "$disease",
                    "tr": "$testResult",
                    "tt": "$testType"
                  }
                ],
                "dob": "1998-02-26",
                "nam": {
                  "fn": "Musterfrau-Gößinger",
                  "gn": "Gabriele",
                  "fnt": "MUSTERFRAU<GOESSINGER",
                  "gnt": "GABRIELE"
                },
                "ver": "1.0.0"
              }
           """
		return gson.fromJson(testJson, Eudgc::class.java)!!
	}

	fun generateRecoveryCertFromDate(
		validDateFrom: LocalDateTime,
		validDateUntil: LocalDateTime,
		firstTestResult: LocalDateTime,
		disease: String,
	): Eudgc {
		val recoveryJson = """
               {
                 "r": [
                   {
                     "ci": "urn:uvci:01:AT:858CC18CFCF5965EF82F60E493349AA5Y",
                     "co": "AT",
                     "df": "${validDateFrom.format(DateTimeFormatter.ISO_DATE)}",
                     "du": "${validDateUntil.format(DateTimeFormatter.ISO_DATE)}",
                     "fr": "${firstTestResult.format(DateTimeFormatter.ISO_DATE)}",
                     "is": "BMSGPK Austria",
                     "tg": "$disease"
                   }
                 ],
                 "dob": "1998-02-26",
                 "nam": {
                   "fn": "Musterfrau-Gößinger",
                   "gn": "Gabriele",
                   "fnt": "MUSTERFRAU<GOESSINGER",
                   "gnt": "GABRIELE"
                 },
                 "ver": "1.0.0"
               }
            """
		return gson.fromJson(recoveryJson, Eudgc::class.java)!!
	}

	fun generateRecoveryCert(
		validSinceNow: Duration,
		validFromNow: Duration,
		firstResultWasAgo: Duration,
		disease: String,
	): Eudgc {
		val now = LocalDate.now().atStartOfDay()
		val recoveryJson = """
               {
                 "r": [
                   {
                     "ci": "urn:uvci:01:AT:858CC18CFCF5965EF82F60E493349AA5Y",
                     "co": "AT",
                     "df": "${(now + validSinceNow).format(DateTimeFormatter.ISO_DATE)}",
                     "du": "${(now + validFromNow).format(DateTimeFormatter.ISO_DATE)}",
                     "fr": "${(now + firstResultWasAgo).format(DateTimeFormatter.ISO_DATE)}",
                     "is": "BMSGPK Austria",
                     "tg": "$disease"
                   }
                 ],
                 "dob": "1998-02-26",
                 "nam": {
                   "fn": "Musterfrau-Gößinger",
                   "gn": "Gabriele",
                   "fnt": "MUSTERFRAU<GOESSINGER",
                   "gnt": "GABRIELE"
                 },
                 "ver": "1.0.0"
               }
            """
		return gson.fromJson(recoveryJson, Eudgc::class.java)!!
	}

}