package ch.admin.bag.covidcertificate.verifier.pager

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class HomescreenPageAdapter(fragment: Fragment, private val itemsCount: Int) : FragmentStateAdapter(fragment) {


	override fun getItemCount(): Int {
		return itemsCount
	}

	override fun createFragment(position: Int): Fragment {
		return HomescreenPagerFragment.getInstance(position)
	}
}