package ch.admin.bag.covidcertificate.common.aws

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface AWSService {

	@Headers("Accept: application/json")
	@POST("covider-cert-sampl-upload")
	suspend fun upload(@Body body: AWSBody )
}