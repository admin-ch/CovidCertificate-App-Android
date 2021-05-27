package ch.admin.bag.covidcertificate.common.faq.model

import java.io.Serializable

interface Faq

data class Header(val iconName: String?, val title: String, val subtitle: String?) : Faq, Serializable

data class Question(
	val question: String,
	val answer: String,
	var isSelected: Boolean = false,
	val linkTitle: String? = null,
	val linkUrl: String? = null,
) : Faq, Serializable
