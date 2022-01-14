package ch.admin.bag.covidcertificate.wallet

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewAssertion
import org.junit.Assert.assertNotNull


class RecyclerViewNotEmptyAssertion : ViewAssertion {
	override fun check(view: View?, noViewFoundException: NoMatchingViewException?) {
		if (noViewFoundException != null) {
			throw noViewFoundException
		}
		val recyclerView = view as RecyclerView
		val adapter = recyclerView.adapter
		assertNotNull(adapter)
		assert(adapter!!.itemCount > 0)
	}
}

