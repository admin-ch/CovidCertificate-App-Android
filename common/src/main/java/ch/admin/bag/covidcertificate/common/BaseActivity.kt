package ch.admin.bag.covidcertificate.common

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import ch.admin.bag.covidcertificate.common.data.ConfigSecureStorage
import ch.admin.bag.covidcertificate.common.util.LocaleUtil

open class BaseActivity : AppCompatActivity() {

	override fun attachBaseContext(newBase: Context) {
		val language = ConfigSecureStorage.getInstance(newBase).getUserLanguage()
		super.attachBaseContext(LocaleUtil.updateLanguage(newBase, language))
	}

}