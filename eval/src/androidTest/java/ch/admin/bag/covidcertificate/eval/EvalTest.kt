package ch.admin.bag.covidcertificate.eval

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EvalTest {

	@Test
	fun testDecode() {
		val decodeState = Eval.decode(HC1_A)
		assertTrue(decodeState is DecodeState.SUCCESS)
	}
}