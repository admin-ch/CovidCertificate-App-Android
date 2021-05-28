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

import android.content.Context
import ch.admin.bag.covidcertificate.eval.EvalErrorCodes.SIGNATURE_COSE_INVALID
import ch.admin.bag.covidcertificate.eval.chain.*
import ch.admin.bag.covidcertificate.eval.models.Bagdgc
import ch.admin.bag.covidcertificate.eval.nationalrules.NationalRulesVerifier
import ch.admin.bag.covidcertificate.eval.utils.getHardcodedBagSigningKeys
import com.squareup.moshi.Moshi


object Eval {
	private val TAG = Eval::class.java.simpleName

	private val moshi by lazy { Moshi.Builder().build() }

	private val contextIdentifierService = DefaultContextIdentifierService()
	private val base45Service = BagBase45Service()
	private val compressorService = DecompressionService()
	private val noopCoseService = NoopVerificationCoseService()
	private val cborService = DefaultCborService()

	/**
	 * @param qrCodeData content of the scanned qr code, of the format "HC1:base45(...)"
	 * @return DecodeState object which contains the decoded DGC. Signature validity is NOT yet checked.
	 */
	fun decode(qrCodeData: String): DecodeState {
		val verificationResult = VerificationResult()
		val encoded = contextIdentifierService.decode(qrCodeData, verificationResult)
		val compressed = base45Service.decode(encoded, verificationResult)
		val cose = compressorService.decode(compressed, verificationResult)
		val cbor = noopCoseService.decode(cose, verificationResult)
		val eudgc = cborService.decode(cbor, verificationResult)

		return if (verificationResult.cborDecoded) {
			DecodeState.SUCCESS(Bagdgc(eudgc, qrCodeData, cose, verificationResult))
		}
		// If not successful, try to find a reasonable error to bubble up
		else if (!verificationResult.base45Decoded) {
			DecodeState.ERROR(Error(EvalErrorCodes.DECODE_BASE_45))
		} else if (!verificationResult.zlibDecoded) {
			DecodeState.ERROR(Error(EvalErrorCodes.DECODE_Z_LIB))
		} else {
			DecodeState.ERROR(Error(EvalErrorCodes.DECODE_UNKNOWN))
		}
	}

	/**
	 * @param bagdgc Object which was returned from the decode function
	 * @return State for the signature check
	 */
	suspend fun checkSignature(bagdgc: Bagdgc, context: Context): CheckSignatureState {
		val keys = getHardcodedBagSigningKeys()

		val vr = bagdgc.verificationResult

		val timestampVerificationService = TimestampVerificationService()
		timestampVerificationService.validate(vr)
		if (!vr.timestampVerified) {
			return CheckSignatureState.INVALID(EvalErrorCodes.SIGNATURE_TIMESTAMP_INVALID)
		}

		val coseService = VerificationCoseService(keys)
		val type = bagdgc.getType() ?: return CheckSignatureState.INVALID(EvalErrorCodes.SIGNATURE_BAGDGC_TYPE_INVALID)
		coseService.decode(bagdgc.cose, vr, type)

		return if (vr.coseVerified) CheckSignatureState.SUCCESS else CheckSignatureState.INVALID(SIGNATURE_COSE_INVALID)
	}

	/**
	 * @param bagdgc Object which was returned from the decode function
	 * @return State for the revocation check
	 */
	suspend fun checkRevocationStatus(bagdgc: Bagdgc, context: Context): CheckRevocationState {
		return CheckRevocationState.SUCCESS
	}

	/**
	 * @param bagdgc Object which was returned from the decode function
	 * @return State for the Signaturecheck
	 */
	suspend fun checkNationalRules(bagdgc: Bagdgc, context: Context): CheckNationalRulesState {
		return if (!bagdgc.dgc.v.isNullOrEmpty()) {
			NationalRulesVerifier(context).verifyVaccine(bagdgc.dgc.v[0])
		} else if (!bagdgc.dgc.t.isNullOrEmpty()) {
			NationalRulesVerifier(context).verifyTest(bagdgc.dgc.t[0])
		} else if (!bagdgc.dgc.r.isNullOrEmpty()) {
			NationalRulesVerifier(context).verifyRecovery(bagdgc.dgc.r[0])
		} else {
			throw Exception("NO VALID DATA")
		}
	}
}