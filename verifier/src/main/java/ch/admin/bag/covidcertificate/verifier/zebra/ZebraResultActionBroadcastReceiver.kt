package ch.admin.bag.covidcertificate.verifier.zebra

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import ch.admin.bag.covidcertificate.verifier.data.VerifierSecureStorage

class ZebraResultActionBroadcastReceiver : BroadcastReceiver() {

	companion object {
		private const val KEY_ZEBRA_ENUMERATE_SCANNER_RESULT = "com.symbol.datawedge.api.RESULT_ENUMERATE_SCANNERS"
	}

	override fun onReceive(context: Context, intent: Intent) {
		if (intent.hasExtra(KEY_ZEBRA_ENUMERATE_SCANNER_RESULT)) {
			handleEnumerateScannersResult(context, intent)
		}
	}

	private fun handleEnumerateScannersResult(context: Context, intent: Intent) {
		val storage = VerifierSecureStorage.getInstance(context)
		val scanners = intent.getSerializableExtra(KEY_ZEBRA_ENUMERATE_SCANNER_RESULT) as? List<Bundle>
		if (scanners != null && scanners.isNotEmpty()) {
			storage.setHasZebraScanner(true)

			// Create a profile in the Data Wedge API
			val createProfileIntent = ZebraDataWedgeApiUtil.getCreateProfileIntent()
			context.sendBroadcast(createProfileIntent)

			// Configure the Data Wedge API
			val setConfigIntent = ZebraDataWedgeApiUtil.getSetConfigIntent(context)
			context.sendBroadcast(setConfigIntent)
		} else {
			storage.setHasZebraScanner(false)
		}
	}

}