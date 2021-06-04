package ch.admin.bag.covidcertificate.common.net

object Config {
	var userAgent: UserAgentInterceptor.UserAgentGenerator = UserAgentInterceptor.UserAgentGenerator { "covid-cert-common" }
}