/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.common.util

import android.content.Context
import ch.admin.bag.covidcertificate.common.R
import ch.admin.bag.covidcertificate.common.config.ConfigModel
import ch.admin.bag.covidcertificate.common.html.BuildInfo
import com.squareup.moshi.Moshi
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

object AssetUtil {

	private const val PREFIX_ASSET_FILE = "file:///android_asset/"
	private const val FOLDER_NAME_IMPRESSUM = "impressum/"
	private const val FILE_NAME_IMPRESSUM = "impressum.html"
	private const val FOLDER_NAME_DISCLAIMER = "disclaimer/"
	private const val DISCLAIMER_FALLBACK_LANGUAGE = "de"
	private const val FILE_NAME_DATA_PROTECTION_STATEMENT = "data_protection_statement.html"
	private const val FILE_NAME_TERMS_OF_USE = "terms_of_use.html"

	private const val REPLACE_STRING_APP_NAME = "{APP_NAME}"
	private const val REPLACE_STRING_VERSION = "{VERSION}"
	private const val REPLACE_STRING_APPVERSION = "{APPVERSION}"
	private const val REPLACE_STRING_RELEASEDATE = "{RELEASEDATE}"
	private const val REPLACE_STRING_BUILDNR = "{BUILD}"
	private const val REPLACE_STRING_LAW_LINK = "{LAW_LINK}"
	private const val REPLACE_STRING_APP_IDENTIFIER = "{PARAM_APP_IDENTIFIER}"

	private val RELEASE_DATE_FORMAT = SimpleDateFormat("dd.MM.yyyy").apply { timeZone = TimeZone.getTimeZone("Europe/Zurich") }

	private const val ASSET_FILENAME_DEFAULT_CONFIG = "faq/config.json"

	private fun loadAssetJson(context: Context, filename: String): String? {
		return try {
			val inputStream = context.assets.open(filename)
			val json = inputStream.bufferedReader().use { it.readText() }
			inputStream.close()
			json
		} catch (ex: IOException) {
			ex.printStackTrace()
			null
		}
	}

	fun loadDefaultConfig(context: Context): ConfigModel? = loadAssetJson(context, ASSET_FILENAME_DEFAULT_CONFIG)?.let {
		Moshi.Builder().build().adapter(ConfigModel::class.java).fromJson(it)
	}

	fun getImpressumBaseUrl(context: Context): String {
		return PREFIX_ASSET_FILE + getFolderNameImpressum(context)
	}

	private fun getFolderNameImpressum(context: Context): String {
		return FOLDER_NAME_IMPRESSUM + context.getString(R.string.language_key) + "/"
	}

	fun getImpressumHtml(context: Context, buildInfo: BuildInfo): String? {
		return loadImpressumHtmlFile(context, FILE_NAME_IMPRESSUM, buildInfo)
	}

	fun loadImpressumHtmlFile(
		context: Context,
		filename: String,
		buildInfo: BuildInfo,
	): String? {
		return try {
			val html = StringBuilder()
			BufferedReader(InputStreamReader(context.assets.open(getFolderNameImpressum(context) + filename))).use { reader ->
				var line: String?
				while (reader.readLine().also { line = it } != null) {
					html.append(line)
				}
			}
			var impressum = html.toString()
			val buildString = "${buildInfo.buildTime} / ${buildInfo.flavor}"
			impressum = impressum.replace(REPLACE_STRING_VERSION, buildInfo.versionName)
			impressum = impressum.replace(REPLACE_STRING_APPVERSION, buildInfo.versionName)
			impressum = impressum.replace(REPLACE_STRING_RELEASEDATE, RELEASE_DATE_FORMAT.format(buildInfo.buildTime))
			impressum = impressum.replace(REPLACE_STRING_BUILDNR, buildString)
			impressum = impressum.replace(REPLACE_STRING_APP_NAME, buildInfo.appName)
			impressum = impressum.replace(REPLACE_STRING_LAW_LINK, buildInfo.agbUrl)
			impressum = impressum.replace(REPLACE_STRING_APP_IDENTIFIER, buildInfo.appIdentifier)
			impressum
		} catch (e: IOException) {
			e.printStackTrace()
			""
		}
	}

	private fun getFolderNameDisclaimer(context: Context): String {
		return FOLDER_NAME_DISCLAIMER + context.getString(R.string.language_key) + "/"
	}

	private fun getDefaultLanguageFolderNameDisclaimer(): String {
		return "$FOLDER_NAME_DISCLAIMER$DISCLAIMER_FALLBACK_LANGUAGE/"
	}

	fun getTermsOfUse(context: Context): String {
		var htmlString = loadHtml(
			context,
			getFolderNameDisclaimer(context) + FILE_NAME_TERMS_OF_USE
		)
		if (htmlString == null) htmlString = loadHtml(
			context,
			getDefaultLanguageFolderNameDisclaimer() + FILE_NAME_TERMS_OF_USE
		)
		if (htmlString == null) htmlString = ""
		return replaceUlTags(htmlString)
	}

	fun getDataProtection(context: Context): String {
		var htmlString = loadHtml(
			context,
			getFolderNameDisclaimer(context) + FILE_NAME_DATA_PROTECTION_STATEMENT
		)
		if (htmlString == null) htmlString = loadHtml(
			context,
			getDefaultLanguageFolderNameDisclaimer() + FILE_NAME_DATA_PROTECTION_STATEMENT
		)
		if (htmlString == null) htmlString = ""
		return replaceUlTags(htmlString)
	}

	private fun replaceUlTags(htmlString: String): String {
		return htmlString.replace("<ul>", "<myul>").replace("</ul>", "</myul>").replace("<li>", "<myli>")
			.replace("</li>", "</myli>")
	}

	private fun loadHtml(context: Context, path: String): String? {
		return try {
			val html = StringBuilder()
			BufferedReader(InputStreamReader(context.assets.open(path))).use { reader ->
				var line: String?
				while (reader.readLine().also { line = it } != null) {
					html.append(line)
				}
			}
			html.toString()
		} catch (e: IOException) {
			e.printStackTrace()
			null
		}
	}

}