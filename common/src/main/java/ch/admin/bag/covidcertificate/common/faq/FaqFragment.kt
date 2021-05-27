package ch.admin.bag.covidcertificate.common.faq

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import ch.admin.bag.covidcertificate.common.databinding.FragmentFaqBinding
import ch.admin.bag.covidcertificate.common.faq.model.Faq
import ch.admin.bag.covidcertificate.common.util.UrlUtil
import ch.admin.bag.covidcertificate.common.views.hideAnimated


abstract class FaqFragment : Fragment() {

	private lateinit var binding: FragmentFaqBinding

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		binding = FragmentFaqBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		binding.certificatesOverviewToolbar.setNavigationOnClickListener {
			parentFragmentManager.popBackStack()
		}

		setupFaqProvider()
	}

	protected abstract fun setupFaqProvider()

	protected fun setupFaqList(items: List<Faq>) {
		binding.faqLoadingView.hideAnimated()

		val recyclerView = binding.faqRecyclerView
		(recyclerView.itemAnimator as SimpleItemAnimator?)?.supportsChangeAnimations = false
		val layoutManager = LinearLayoutManager(recyclerView.context, LinearLayoutManager.VERTICAL, false)
		recyclerView.layoutManager = layoutManager
		val adapter = FaqAdapter(onItemClickListener = object : OnUrlClickListener {
			override fun onLinkClicked(url: String) {
				context?.let { UrlUtil.openUrl(it, url) }
			}

		})
		recyclerView.adapter = adapter
		adapter.setItems(items)
	}
}