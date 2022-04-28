package ch.admin.bag.covidcertificate.wallet

import android.app.Activity.RESULT_OK
import android.app.Instrumentation
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import com.fasterxml.jackson.databind.ObjectMapper
import android.net.Uri
import android.util.Log
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.ViewMatchers.*
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
private val vaccination_url =
    URL("https://ws.covidcertificate-a.bag.admin.ch/api/v1/covidcertificate/vaccination")
private val test_url =
    URL("https://ws.covidcertificate-a.bag.admin.ch/api/v1/covidcertificate/test")
private val recovery_url =
    URL("https://ws.covidcertificate-a.bag.admin.ch/api/v1/covidcertificate/recovery")

private val httpClient by lazy {buildClient()}
private val vaccineDateFormat = SimpleDateFormat("yyyy-MM-dd")
private val testDateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
private val moshi = Moshi.Builder().build()
private val mapper = ObjectMapper()

enum class UiValidityState {
    VALID,
    EXPIRED,
    NOT_YET_VALID
}

enum class VaccineType(val id: String) {
    PFIZER("EU/1/20/1528"),
    MODERNA("EU/1/20/1507"),
    ASTRAZENECA("EU/1/21/1529"),
    JANSSEN("EU/1/20/1525"),
    NUVOVAXOVID("EU/1/21/1618"),
    NUVOVAXOVID_OLD("NVX-CoV2373"),
    CORONAVAC("CoronaVac"),
    CORV("BBIBP-CorV"),
    COVISHIELD("Covishield"),
    COVAXIN("Covaxin"),
    R_COVI("R-COVI"),
    RECOMBININANT("Covid-19-recombinant"),
    COVOVAX("Covovax"),
    CORV_T("BBIBP-CorV_T"),
    CORONAVAC_T("CoronaVac_T"),
    COVAXIN_T("Covaxin_T")
}

enum class TestType(val id: String) {
    PCR("LP6464-4"),
    RAT("LP217198-3")
}


fun checkCertValidity(expected: UiValidityState) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    onView(
        allOf(
            withId(R.id.homescreen_certificates_view_pager),
            isDisplayed()
        )
    ).perform(ViewActions.click())
    onIdle()
    when (expected) {
        UiValidityState.VALID -> onView(allOf(withId(R.id.certificate_detail_info))).check(
            matches(
                withText(R.string.verifier_verify_success_info)
            )
        )
        UiValidityState.EXPIRED -> {
            onView(allOf(withId(R.id.certificate_detail_info)))
                .check(
                    matches(
                        anyOf(
                            withText(R.string.wallet_error_expired)
                        )
                    )
                )
        }
        UiValidityState.NOT_YET_VALID -> {
            onView(allOf(withId(R.id.certificate_detail_info)))
                .check(
                    matches(
                        anyOf(
                            withText(
                                startsWith(
                                    context.getString(R.string.wallet_error_valid_from)
                                        .substringBefore(":")
                                )
                            )
                        )
                    )
                )
        }
    }

    Espresso.pressBack()

}

fun importCert(uri: Uri) {
    intending(hasAction(Intent.ACTION_GET_CONTENT))
        .respondWith(Instrumentation.ActivityResult(RESULT_OK, Intent().setData(uri)))
    try {
        onView(
            allOf(
                withId(R.id.homescreen_scan_button_small),
                isDisplayed()
            )
        ).perform(ViewActions.click())

    } catch (e: NoMatchingViewException) {
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

private fun downloadCertPdf(payload: CertGenerationPayload, url: URL): Uri {
    val requestData = mapper.writeValueAsString(payload)
    val request = Request.Builder()
        .addHeader("X-Signature", sign(requestData))
        .addHeader("Content-Type", "application/json")
        .method("POST", RequestBody.create("application/json".toMediaTypeOrNull(), requestData))
        .url(url)
        .build()
    //Download and check result
    val response = httpClient.newCall(request).execute()
    if (!response.isSuccessful) {
        Log.e("TEST", response.body!!.string())
        assert(false)
    }
    val result = mapper.readTree(response.body!!.string())
    val pdf = Base64.getDecoder().decode(result.get("pdf").asText())
    //Write result to file
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val file = File(context.filesDir, "${result.get("uvci").asText()}.pdf")
    file.writeBytes(pdf)
    return Uri.fromFile(file)
}

/**
 * Downloads a vaccine cert from the API
 * @return the path of the downloaded PDF
 */
fun downloadVaccineCert(
    vaccineProduct: VaccineType,
    dosesGiven: Int,
    dosesRequired: Int,
    date: Date,
    name: String = "Chuck Tester",
    dob: String = "1999-09-09",
    country: String = "CH"
): Uri {
    val dateString = vaccineDateFormat.format(date)
    val requestPayload = CertGenerationPayload.VaccineGenerationPayload(
        CertGenerationPayload.Companion.Name(name.split(' ')[1], name.split(' ')[0]),
        dob,
        "de",
        otp,
        listOf(
            CertGenerationPayload.Companion.VaccinationInfo(
                vaccineProduct.id,
                dosesGiven,
                dosesRequired,
                dateString,
                country
            )
        ),
    )
    return downloadCertPdf(requestPayload, vaccination_url)
}

/**
 * Downloads a test cert from the API
 * @return the path of the downloaded PDF
 */
fun downloadTestCert(
    testType: TestType,
    date: Date,
    name: String = "Chuck Tester",
    testCenter: String = "a;aaaa",
    dob: String = "1999-09-09",
    country: String = "CH"
): Uri {
    val dateString = testDateTimeFormat.format(date)
    val requestPayload = CertGenerationPayload.TestGenerationPayload(
        CertGenerationPayload.Companion.Name(name.split(' ')[1], name.split(' ')[0]),
        dob,
        "de",
        otp,
        listOf(
            CertGenerationPayload.Companion.TestInfo(
                testType.id,
                dateString,
                testCenter,
                country,
                if(testType == TestType.RAT) "1232" else null
            )
        ),
    )
    return downloadCertPdf(requestPayload, test_url)
}

/**
 * Downloads a recovery cert from the API
 * @return the path of the downloaded PDF
 */
fun downloadRecoveryCert(
    date: Date,
    name: String = "Chuck Tester",
    dob: String = "1999-09-09",
    country: String = "CH"
): Uri {
    val dateString = vaccineDateFormat.format(date)
    val requestPayload = CertGenerationPayload.RecoveryGenerationPayload(
        CertGenerationPayload.Companion.Name(name.split(' ')[1], name.split(' ')[0]),
        dob,
        "de",
        otp,
        listOf(
            CertGenerationPayload.Companion.RecoveryInfo(dateString, country)
            )
        )
    return downloadCertPdf(requestPayload, recovery_url)
}

/**
 * Sign a cert download request (json)
 */
private fun sign(payload: String): String {
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

private sealed class CertGenerationPayload(
    val name: Name,
    val dateOfBirth: String,
    val language: String,
    val otp: String
) {
    companion object {
        data class Name(val familyName: String, val givenName: String)
        data class VaccinationInfo(
            val medicinalProductCode: String,
            val numberOfDoses: Int,
            val totalNumberOfDoses: Int,
            val vaccinationDate: String,
            val countryOfVaccination: String
        )

        data class TestInfo(
            val typeCode: String,
            val sampleDateTime: String,
            val testingCentreOrFacility: String,
            val memberStateOfTest: String,
            val manufacturerCode: String?
        )
        data class RecoveryInfo(
            val dateOfFirstPositiveTestResult: String,
            val countryOfTest: String
        )
    }
    class VaccineGenerationPayload(name: Name, dateOfBirth: String, language: String, otp: String, val vaccinationInfo: List<VaccinationInfo>): CertGenerationPayload(name, dateOfBirth, language, otp)
    class TestGenerationPayload(name: Name, dateOfBirth: String, language: String, otp: String, val testInfo: List<TestInfo>): CertGenerationPayload(name, dateOfBirth, language, otp)
    class RecoveryGenerationPayload(name: Name, dateOfBirth: String, language: String, otp: String, val RecoveryInfo: List<RecoveryInfo>): CertGenerationPayload(name, dateOfBirth, language, otp)
}

