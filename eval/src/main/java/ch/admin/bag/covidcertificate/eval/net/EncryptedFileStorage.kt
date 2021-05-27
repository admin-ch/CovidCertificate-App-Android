package ch.admin.bag.covidcertificate.eval.net

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object EncryptedFileStorage {
	private val TAG = EncryptedFileStorage::class.java.simpleName

	fun writeFile(context: Context, path: String, content: String) {
		val masterKeyAlias: String = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

		val file = File(context.filesDir, path)

		val encryptedFile: EncryptedFile = EncryptedFile.Builder(
			file,
			context,
			masterKeyAlias,
			EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
		).build()

		if (file.exists()){
			// Yeah, interesting design choice by Google, see also https://stackoverflow.com/q/63127401
			file.delete()
		}

		val encryptedOutputStream: FileOutputStream = encryptedFile.openFileOutput()
		try {
			encryptedOutputStream.write(content.encodeToByteArray())
		} catch (e: Exception) {
			Log.e(TAG, e.toString())
		} finally {
			encryptedOutputStream.close()
		}
	}

	fun readFile(context: Context, path: String): String {
		val masterKeyAlias: String = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

		val file = File(context.filesDir, path)
		val encryptedFile: EncryptedFile = EncryptedFile.Builder(
			file,
			context,
			masterKeyAlias,
			EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
		).build()

		if (!file.exists()) return ""

		val encryptedInputStream: FileInputStream = encryptedFile.openFileInput()
		val byteArrayOutputStream = ByteArrayOutputStream()
		var nextByte: Int = encryptedInputStream.read()
		while (nextByte != -1) {
			byteArrayOutputStream.write(nextByte)
			nextByte = encryptedInputStream.read()
		}
		val bytes: ByteArray = byteArrayOutputStream.toByteArray()
		return bytes.decodeToString()
	}

}