package ch.admin.bag.covidcertificate.eval.models

/**
 * The type of health certificate.
 *
 * These are explicitly mutually exclusive: a certificate must not contain both a vaccination and a test statement.
 */
enum class CertType(val use: String) {
	VACCINATION("v"),
	TEST("t"),
	RECOVERY("r"),
}