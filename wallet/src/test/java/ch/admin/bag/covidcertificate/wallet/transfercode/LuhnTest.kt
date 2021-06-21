package ch.admin.bag.covidcertificate.wallet.transfercode

import ch.admin.bag.covidcertificate.wallet.transfercode.logic.Luhn
import ch.admin.bag.covidcertificate.wallet.transfercode.logic.Luhn.TRANSFER_CODE_LEN
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters


@RunWith(Parameterized::class)
class LuhnTest(
	private val code: CharArray,
	private val expectedCheckChar: Char,
) {

	companion object {

		@Parameters
		@JvmStatic
		fun buildTestCases(): Collection<Array<Any>> {
			val nineLetterCodes = listOf(
				"Y8P8ECFN8",
				"HDTYRB66W",
				"YS6R7H88T",
				"K42K6F7R2",
				"3BY8DAZYS",
				"ADWYF11SY",
				"453S6HUA6",
				"WR7UPHB4A",
				"37WDPRSKM",
				"01AWUUB2M",
				"MA4S9CNUK",
				"SY7M684WA",
				"X216WN3YF",
				"3C2YFKCNP",
				"TNKBZ0TSK",
			).map { Pair(it.slice(0..7), it[8]) }

			val cases = mutableListOf<Pair<String, Char>>(
				Pair("CHAR", 'M'),
				Pair("SW1SS", '9'),
				Pair("APPS", '1'),
				Pair("0123456789", 'S'),
				Pair("TRANSFERC0DE", 'T'),
			)
			cases.addAll(nineLetterCodes)

			return cases.map { arrayOf(it.first.toCharArray(), it.second) }
		}
	}

	@Test
	fun testComputeCheckCharacter() {
		val checkChar = Luhn.computeCheckCharacter(code)
		assertEquals(expectedCheckChar, checkChar)
	}

	@Test
	fun testGenerateNewTransferCode() {
		for (i in 0 until 5) {
			val code = Luhn.generateNewTransferCode()
			val prefix = code.substring(0, TRANSFER_CODE_LEN - 1).toCharArray()
			val checkChar = Luhn.computeCheckCharacter(prefix)

			assertEquals(checkChar, code[TRANSFER_CODE_LEN - 1])
		}
	}

	@Test
	fun checkAlphabetSize() = assertEquals(Luhn.ALPHABET.size, 29)

}