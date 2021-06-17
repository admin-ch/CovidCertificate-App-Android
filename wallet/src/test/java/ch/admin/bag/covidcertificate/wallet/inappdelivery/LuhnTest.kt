package ch.admin.bag.covidcertificate.wallet.inappdelivery

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
			val cases = listOf<Pair<String, Char>>(
				Pair("CHAR", 'M'),
				Pair("SW1SS", '9'),
				Pair("APPS", '1'),
				Pair("0123456789", 'S'),
				Pair("TRANSFERC0DE", 'T'),
			)

			return cases.map { arrayOf(it.first.toCharArray(), it.second) }
		}
	}

	@Test
	fun computeCheckCharacter() {
		val checkChar = Luhn.computeCheckCharacter(code)
		assertEquals(expectedCheckChar, checkChar)
	}

	@Test
	fun generateNewTransferCode() {
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