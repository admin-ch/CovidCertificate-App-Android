/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package ch.admin.bag.covidcertificate.wallet.transfercode.logic

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import ch.admin.bag.covidcertificate.sdk.core.extensions.fromBase64
import ch.admin.bag.covidcertificate.sdk.core.extensions.toBase64
import ch.admin.bag.covidcertificate.wallet.data.WalletSecureStorage
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Security
import java.security.Signature
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAKeyGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource.PSpecified
import javax.crypto.spec.SecretKeySpec


object TransferCodeCrypto {

	const val ANDROID_KEYSTORE_NAME = "AndroidKeyStore"
	const val BOUNCY_CASTLE_PROVIDER = "BC"

	init {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
			// Remove platform provided bouncy castle provider and add updated one from library on Android 23 and lower
			Security.removeProvider(BOUNCY_CASTLE_PROVIDER)
			Security.addProvider(BouncyCastleProvider())
		}
	}

	fun createKeyPair(keyAlias: String, context: Context): KeyPair? {
		val keyPurpose =
			KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY

		val keyGenParameterSpec = KeyGenParameterSpec
			.Builder(keyAlias, keyPurpose)
			.apply {
				// 2048 bit with public exponent 65537
				setAlgorithmParameterSpec(RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4))
				setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA1)
				setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
				setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PSS)

				// Encourage storing the key in strongbox (the TPM)
				val hasStrongBox = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
					context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
				} else false
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && hasStrongBox) {
					setIsStrongBoxBacked(true)
				}
			}.build()

		try {
			return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
				val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, BOUNCY_CASTLE_PROVIDER).apply {
					initialize(RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4))
				}

				val kp = kpg.generateKeyPair()

				val storage = WalletSecureStorage.getInstance(context)
				storage.setTransferCodePublicKey(keyAlias, kp.public.encoded.toBase64())
				storage.setTransferCodePrivateKey(keyAlias, kp.private.encoded.toBase64())

				kp
			} else {
				val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE_NAME).apply {
					initialize(keyGenParameterSpec)
				}

				kpg.generateKeyPair()
			}
		} catch (e: Throwable) {
			e.printStackTrace()
			return null
		}
	}

	fun loadKeyPair(keyAlias: String, context: Context): KeyPair? {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
			val storage = WalletSecureStorage.getInstance(context)
			val encodedPublicKey = storage.getTransferCodePublicKey(keyAlias)?.fromBase64()
			val encodedPrivateKey = storage.getTransferCodePrivateKey(keyAlias)?.fromBase64()

			return if (encodedPublicKey != null && encodedPrivateKey != null) {
				val kf = KeyFactory.getInstance("RSA", BOUNCY_CASTLE_PROVIDER)

				val publicKeySpec = X509EncodedKeySpec(encodedPublicKey)
				val publicKey = kf.generatePublic(publicKeySpec)

				val privateKeySpec = PKCS8EncodedKeySpec(encodedPrivateKey)
				val privateKey = kf.generatePrivate(privateKeySpec)

				KeyPair(publicKey, privateKey)
			} else {
				null
			}
		} else {
			val keystore = KeyStore.getInstance(ANDROID_KEYSTORE_NAME).apply {
				load(null)
			}
			if (!keystore.containsAlias(keyAlias)) {
				return null
			}
			val entry = keystore.getEntry(keyAlias, null) as? KeyStore.PrivateKeyEntry ?: return null

			return KeyPair(entry.certificate.publicKey, entry.privateKey)
		}
	}

	fun deleteKeyEntry(keyAlias: String, context: Context) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
			val storage = WalletSecureStorage.getInstance(context)
			storage.setTransferCodePublicKey(keyAlias, null)
			storage.setTransferCodePrivateKey(keyAlias, null)
		} else {
			val keystore = KeyStore.getInstance(ANDROID_KEYSTORE_NAME).apply {
				load(null)
			}

			if (keystore.containsAlias(keyAlias)) {
				keystore.deleteEntry(keyAlias)
			}
		}
	}

	fun decrypt(keyPair: KeyPair, ciphertextBase64: String): String? {
		/*
		 * This is somewhat like a KEM:
		 * ciphertext = RSA-OAEP( aesIV || aesKey ) || AES-GCM(healthcert)
		 *
		 * Also note that all indices below are inclusive to inclusive!
		 */
		val ciphertext = ciphertextBase64.fromBase64()

		val ciphertextWrappedAesKeyAndIv = ciphertext.sliceArray(0..255) // 256 bytes = 2048 bits => one RSA-OAEP "block"
		val ciphertextCertificate = ciphertext.sliceArray(256..ciphertext.lastIndex)

		try {
			// Decrypt the symmetric AES that was encapsulated under RSA-OAEP
			val rsaPlaintext = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
				val oaepParams = OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec("SHA-1"), PSpecified.DEFAULT)
				val cipher = Cipher.getInstance("RSA/ECB/OAEPPadding", BOUNCY_CASTLE_PROVIDER)
				cipher.init(Cipher.DECRYPT_MODE, keyPair.private, oaepParams)
				cipher.doFinal(ciphertextWrappedAesKeyAndIv)
			} else {
				val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
				cipher.init(Cipher.DECRYPT_MODE, keyPair.private)
				cipher.doFinal(ciphertextWrappedAesKeyAndIv)
			}

			val aesIv = rsaPlaintext.sliceArray(0..11) // 12 byte IV
			val aesKey = rsaPlaintext.sliceArray(12..43) // 32 byte = 256 bit key

			val AES_GCM_TAG_LEN = 128 // one AES block
			val gcmSpec = GCMParameterSpec(AES_GCM_TAG_LEN, aesIv) // algorithm
			val secretKeySpec = SecretKeySpec(aesKey, "AES") // key

			// Decrypt the health certificate encrypted under AES-GCM
			val aesPlaintext = Cipher.getInstance("AES/GCM/NoPadding").run {
				init(Cipher.DECRYPT_MODE, secretKeySpec, gcmSpec)
				doFinal(ciphertextCertificate)
			}
			return aesPlaintext.decodeToString()
		} catch (e: Throwable) {
			e.printStackTrace()
			return null
		}
	}

	fun sign(keyPair: KeyPair, message: String): String? {
		val msg = message.encodeToByteArray()
		return try {
			val signature = Signature.getInstance("SHA256withRSA/PSS").run {
				initSign(keyPair.private)
				update(msg)
				sign()
			}
			signature.toBase64()
		} catch (e: Throwable) {
			e.printStackTrace()
			null
		}
	}

	/**
	 * Creates a message to sign over for the delivery requsts.
	 *
	 * `action` should be one of: register, get, delete.
	 * This binds the context for which the signature is intended.
	 */
	fun buildMessage(action: String, transferCode: String): String {
		val timestamp = Instant.now().toEpochMilli()
		return "$action:$transferCode:$timestamp"
	}

}

fun KeyPair.isInsideSecureHardware(): Boolean {
	val factory: KeyFactory = KeyFactory.getInstance(private.algorithm, TransferCodeCrypto.ANDROID_KEYSTORE_NAME)
	val keyInfo: KeyInfo = factory.getKeySpec(private, KeyInfo::class.java) as KeyInfo
	return keyInfo.isInsideSecureHardware
}
