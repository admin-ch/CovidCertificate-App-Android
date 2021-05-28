/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.eval.net

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import okhttp3.Interceptor

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response

import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException
import java.security.PublicKey

import java.security.SignatureException

/// application/json media type for the payload of a JWS
val APPLICATION_JSON = "application/json".toMediaType()

/**
   This class can be used as an interceptor to verify a JWS and return a response with a plain JSON-Object.
   Since we need a JWS with a JSON body (we explicitly ask for it), we can savely assume that the content-type
   is `application/json`.
 The signature of the JWS is verified with `publicKey`.
 */
 class JwsInterceptor(private val publicKey: PublicKey) :
        Interceptor {
    @Throws(IOException::class)

    override fun intercept(chain: Interceptor.Chain): Response {
        val response: Response = chain.proceed(chain.request())
        if (!response.isSuccessful) {
            return response
        }
        if (response.cacheResponse != null) {
            return response
        }
        val jws = response.body!!.string()

        var body = ""
        try {
            val claimsJws : Jws<Claims> = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(jws)
            // now that the JWS is verified, we can safely assume that the body can be trusted, so serialize it to JSON again
            body = Moshi.Builder().build().adapter<Claims>(Types.newParameterizedType(Map::class.java, String::class.java, Object::class.java)).toJson(claimsJws.body)
        } catch (e: io.jsonwebtoken.security.SignatureException) {
            throw SignatureException("Invalid Signature")
        } catch (o: ExpiredJwtException) {
            throw SignatureException("Expired JWT")
        }
        // SAFE ZONE
        // from here on body contains a JSON-string of the payload of the JWS, whose signature we verified.

        return response.newBuilder()
                .body(
                        body.toResponseBody(APPLICATION_JSON)
                ).build()
    }
}