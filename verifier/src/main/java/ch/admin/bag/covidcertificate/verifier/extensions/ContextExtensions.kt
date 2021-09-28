package ch.admin.bag.covidcertificate.verifier.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.security.MessageDigest

@SuppressLint("PackageManagerGetSignatures")
fun Context.getApplicationSignature(): List<String> {
	try {
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			val sig = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo
			if (sig.hasMultipleSigners()) {
				// Send all with apkContentsSigners
				sig.apkContentsSigners.map { it.toShaHash() }
			} else {
				// Send one with signingCertificateHistory
				sig.signingCertificateHistory.map { it.toShaHash() }
			}
		} else {
			val sig = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
			sig.map { it.toShaHash() }
		}
	} catch (e: Exception) {
		// Ignore exception
	}
	return emptyList()
}

private fun Signature.toShaHash(): String {
	val digest = MessageDigest.getInstance("SHA")
	digest.update(this.toByteArray())
	return bytesToHex(digest.digest())
}

private fun bytesToHex(bytes: ByteArray): String {
	val hexArray = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')
	val hexChars = CharArray(bytes.size * 2)
	var v: Int
	for (j in bytes.indices) {
		v = bytes[j].toInt() and 0xFF
		hexChars[j * 2] = hexArray[v.ushr(4)]
		hexChars[j * 2 + 1] = hexArray[v and 0x0F]
	}
	return String(hexChars)
}