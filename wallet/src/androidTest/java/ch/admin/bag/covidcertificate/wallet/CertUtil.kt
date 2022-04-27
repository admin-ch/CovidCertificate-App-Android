package ch.admin.bag.covidcertificate.wallet

import android.app.Activity.RESULT_OK
import android.app.Instrumentation
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import com.fasterxml.jackson.databind.ObjectMapper
import android.net.Uri
import android.provider.Settings.Global.getString
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.ViewPagerActions
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import ch.admin.bag.covidcertificate.sdk.core.models.state.VerificationState
import com.squareup.moshi.Moshi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.hamcrest.Matchers.*
import java.io.File
import java.net.URL
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager


private val keystore = BuildConfig.KEYSTORE
private val keystorePass = BuildConfig.KEYSTORE_PASS
private val otp = BuildConfig.OTP
private val url = URL("https://ws.covidcertificate-a.bag.admin.ch/api/v1/covidcertificate/vaccination")
private val httpClient = buildClient()
private val vaccineDateFormat = SimpleDateFormat("yyyy-MM-dd")
private val moshi = Moshi.Builder().build()
private val mapper = ObjectMapper()

enum class UiValidityState{
    VALID,
    EXPIRED
}

fun checkCertValidity(expected: UiValidityState){
    onView(allOf(
        withId(R.id.homescreen_certificates_view_pager),
        isDisplayed()
    )).perform(ViewActions.click())
    /*onView(allOf(
        withId(R.id.certificate_page_card)
    )).perform(ViewActions.click())*/
    when(expected){
        UiValidityState.VALID -> onView(allOf(withId(R.id.certificate_detail_info))).check(matches(withText("Only valid in combination with \nan identity document")))
        UiValidityState.EXPIRED -> {
            onView(allOf(withId(R.id.certificate_detail_info)))
                .check(matches(anyOf(
                    withText(containsString("expired")),
                    withText(containsString("not valid"))
                )
                ))
        }
    }

    Espresso.pressBack()

}

fun importCert(uri: Uri){
    intending(hasAction(Intent.ACTION_GET_CONTENT))
        .respondWith(Instrumentation.ActivityResult(RESULT_OK, Intent().setData(uri)))
    try {
        onView(
            allOf(
                withId(R.id.homescreen_scan_button_small),
                isDisplayed()
            )
        ).perform(ViewActions.click())

    }catch(e: NoMatchingViewException){
        //looks like this is the first cert being imported
    }

    onView(
        allOf(
            withId(R.id.option_import_pdf),
            isDisplayed()
        )
    ).perform(ViewActions.click())

    Thread.sleep(2000)
    onIdle()
    val addButton = onView(
        allOf(
            withId(R.id.certificate_add_button),
            isDisplayed()
        )
    )
    addButton.perform(ViewActions.click())
}

/**
 * Downloads a vaccine cert from the API
 * @return the path of the downloaded PDF
 */
fun downloadVaccineCert(
    vaccineProduct: String,
    dosesGiven: Int,
    dosesRequired: Int,
    date: Date,
    name: String = "Chuck Tester",
    dob: String = "1999-09-09",
    country: String = "CH"
): Uri {
    val dateString = vaccineDateFormat.format(date)
    val requestPayload = CertGenerationPayload(
        CertGenerationPayload.Companion.Name(name.split(' ')[1], name.split(' ')[0]),
        dob,
        listOf(
            CertGenerationPayload.Companion.VaccinationInfo(
                vaccineProduct,
                dosesGiven,
                dosesRequired,
                dateString,
                country
            )
        ),
        "de",
        otp
    )
    //val requestData = mapper.writeValueAsString(requestPayload)
    val requestData = mapper.writeValueAsString(requestPayload)

    val request = Request.Builder()
        .addHeader("X-Signature", sign(requestData))
        .addHeader("Content-Type", "application/json")
        .method("POST", RequestBody.create("application/json".toMediaTypeOrNull(), requestData))
        .url(url)
        .build()
    //Download and check result
    val response = httpClient.newCall(request).execute()
    assert(response.isSuccessful)
    val result = mapper.readTree(response.body!!.string())
    val pdf = Base64.getDecoder().decode(result.get("pdf").asText())
    //Write result to file
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val file = File(context.filesDir, "${result.get("uvci").asText()}.pdf")
    file.writeBytes(pdf)
    return Uri.fromFile(file)
}

/**
 * Sign a cert download request (json)
 */
private fun sign(payload: String): String{
    val signature = Signature.getInstance("SHA256WithRSA")
    val ks = KeyStore.getInstance("pkcs12")
    ks.load(Base64.getDecoder().decode(keystore).inputStream(), keystorePass.toCharArray())

    signature.initSign(ks.getKey("covid", keystorePass.toCharArray()) as PrivateKey)
    val normalizedPayload = payload.replace(" ", "")
    signature.update(normalizedPayload.toByteArray())
    val result = signature.sign()
    return Base64.getEncoder().encodeToString(result)
}


private fun buildClient(): OkHttpClient {
    val ks = KeyStore.getInstance("pkcs12")
    ks.load(Base64.getDecoder().decode(keystore).inputStream(), keystorePass.toCharArray())
    val trustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    trustManagerFactory.init(null as KeyStore?)
    val trustManagers = trustManagerFactory.trustManagers
    val keyManagerFactory =
        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
    keyManagerFactory.init(ks, keystorePass.toCharArray())
    val keyManagers = keyManagerFactory.keyManagers
    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagers, trustManagers, null)
    val sslSocketFactory = sslContext.socketFactory
    return OkHttpClient.Builder()
        .sslSocketFactory(sslSocketFactory, trustManagers[0] as X509TrustManager)
        .build()
}

private data class CertGenerationPayload(
    val name: Name,
    val dateOfBirth: String,
    val vaccinationInfo: List<VaccinationInfo>,
    val language: String,
    val otp: String){
    companion object {
        data class Name(val familyName: String, val givenName: String)
        data class  VaccinationInfo(
            val medicinalProductCode: String,
            val numberOfDoses: Int,
            val totalNumberOfDoses: Int,
            val vaccinationDate: String,
            val countryOfVaccination: String)
    }
}


