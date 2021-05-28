package ch.admin.bag.covidcertificate.common.html

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import ch.admin.bag.covidcertificate.common.R
import ch.admin.bag.covidcertificate.common.databinding.FragmentHtmlBinding
import ch.admin.bag.covidcertificate.common.util.AssetUtil.loadImpressumHtmlFile
import ch.admin.bag.covidcertificate.common.util.UrlUtil
import ch.admin.bag.covidcertificate.common.views.hideAnimated

class HtmlFragment : Fragment() {

	companion object {
		private const val COVID_CERT_IMPRESSUM_PREFIX = "ccert://"

		private const val ARG_BASE_URL = "ARG_BASE_URL"
		private const val ARG_BUILD_INFO = "ARG_BUILD_INFO"
		private const val ARG_DATA = "ARG_DATA"
		private const val ARG_TITLE = "ARG_TITLE"
		private const val ARG_FRAGMENT_LAYOUT_ID = "ARG_FRAGMENT_LAYOUT_ID"
		fun newInstance(titleRes: Int, buildInfo: BuildInfo, baseUrl: String, data: String?, fragmentLayoutId: Int): HtmlFragment {
			val args = Bundle()
			args.putString(ARG_BASE_URL, baseUrl)
			args.putSerializable(ARG_BUILD_INFO, buildInfo)
			args.putString(ARG_DATA, data)
			args.putInt(ARG_TITLE, titleRes)
			args.putInt(ARG_FRAGMENT_LAYOUT_ID, fragmentLayoutId)
			val fragment = HtmlFragment()
			fragment.arguments = args
			return fragment
		}
	}

	private var _binding: FragmentHtmlBinding? = null
	private val binding get() = _binding!!

	private lateinit var baseUrl: String
	private var data: String? = null
	private var buildInfo: BuildInfo? = null

	@StringRes
	private var titleRes = 0

	@IdRes
	private var fragmentLayoutId = 0

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		requireArguments().apply {
			baseUrl = getString(ARG_BASE_URL) ?: throw IllegalStateException("No baseUrl specified for HtmlFragment")
			buildInfo = getSerializable(ARG_BUILD_INFO) as? BuildInfo?
			data = getString(ARG_DATA)
			titleRes = getInt(ARG_TITLE)
			fragmentLayoutId = getInt(ARG_FRAGMENT_LAYOUT_ID)
		}

	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		_binding = FragmentHtmlBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val toolbar = binding.htmlToolbar
		toolbar.setTitle(titleRes)
		toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }

		val web = binding.htmlWebview
		val loadingSpinner = binding.loadingSpinner

		web.webViewClient = object : WebViewClient() {
			override fun onPageFinished(view: WebView, url: String) {
				loadingSpinner.hideAnimated()
				super.onPageFinished(view, url)
			}

			override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
				if (baseUrl == url) return true
				if (url.toLowerCase().startsWith(COVID_CERT_IMPRESSUM_PREFIX)) {
					val buildInfo = buildInfo ?: throw IllegalStateException("No BuildInfo supplied for imprint")
					val strippedUrl = url.substring(COVID_CERT_IMPRESSUM_PREFIX.length)
					val htmlFragment = newInstance(
						R.string.impressum_title,
						buildInfo,
						baseUrl,
						loadImpressumHtmlFile(view.context, strippedUrl, buildInfo),
						fragmentLayoutId
					)
					parentFragmentManager.beginTransaction()
						.setCustomAnimations(
							R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter,
							R.anim.slide_pop_exit
						)
						.replace(fragmentLayoutId, htmlFragment)
						.addToBackStack(HtmlFragment::class.java.canonicalName)
						.commit()
					return true
				}
				UrlUtil.openUrl(context, url)
				return true
			}
		}
		val webSettings = web.settings
		webSettings.javaScriptEnabled = true
		if (data != null) {
			data?.let { web.loadDataWithBaseURL(baseUrl, it, "text/html", "UTF-8", null) }
		} else {
			web.loadUrl(baseUrl)
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

}