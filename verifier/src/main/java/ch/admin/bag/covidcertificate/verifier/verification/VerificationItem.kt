package ch.admin.bag.covidcertificate.verifier.verification

import android.text.SpannableString

sealed class VerificationItem

data class StatusItem(val statusString: SpannableString, val statusIcon: Int, val bubbleColor: Int, val isLoading: Boolean) :
	VerificationItem()

data class InfoItem(val statusString: String, val infoIconColor: Int, val bubbleColor: Int, val showRetry: Boolean) :
	VerificationItem()