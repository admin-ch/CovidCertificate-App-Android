package ch.admin.bag.covidcertificate.wallet.transfercode

import ch.admin.bag.covidcertificate.wallet.transfercode.logic.Luhn
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters


@RunWith(Parameterized::class)
class LuhnTestWrongCodes(
		private val code: CharArray,
		private val expectedCheckChar: Char,
) {

    companion object {

        @Parameters
        @JvmStatic
        fun buildTestCases(): Collection<Array<Any>> {
            val cases = listOf(
					"Y8P8ECFN9",
					"HDTYRC66W",
					"YS6RH788T",
					"K43K6F7R2",
					"3B8YDAZYS",
					"ADWFY11SY",
					"453S6HU6A",
					"WR7UHPB4A",
					"37WDRPSKM",
					"10AWUUB2M",
					"MAS49CNUK",
					"SY7M864WA",
					"$%(*(!@#$" + "_!@*#", // the sequence $_ doesn't go well with Kotlin (even in raw strings)
			).map { Pair(it.slice(0..(it.length - 2)), it[it.length - 1]) }

            return cases.map { arrayOf(it.first.toCharArray(), it.second) }
        }
    }

    @Test
    fun testComputeCheckCharacter() {
        val checkChar = Luhn.computeCheckCharacter(code)
        assertNotEquals(expectedCheckChar, checkChar)
    }

}