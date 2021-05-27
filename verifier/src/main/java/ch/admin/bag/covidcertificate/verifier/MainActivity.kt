package ch.admin.bag.covidcertificate.verifier

import android.content.DialogInterface
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import ch.admin.bag.covidcertificate.common.config.ConfigModel
import ch.admin.bag.covidcertificate.common.util.UrlUtil
import ch.admin.bag.covidcertificate.verifier.databinding.ActivityMainBinding
import ch.admin.bag.covidcertificate.verifier.networking.ConfigRepository

class MainActivity : AppCompatActivity() {

	private lateinit var binding: ActivityMainBinding

	private val verifierViewModel by viewModels<VerifierViewModel>()

	private var forceUpdateDialog: AlertDialog? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		binding = ActivityMainBinding.inflate(layoutInflater)
		val view = binding.root
		setContentView(view)

		val allowlistedFlavours = listOf("abn", "dev")
		if (!allowlistedFlavours.contains(BuildConfig.FLAVOR)) {
			window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
		}

		if (savedInstanceState == null) {
			supportFragmentManager.beginTransaction()
				.add(R.id.fragment_container, HomeFragment.newInstance())
				.commit()
		}

		verifierViewModel.configLiveData.observe(this) { config -> handleConfig(config) }
	}

	override fun onStart() {
		super.onStart()
		verifierViewModel.loadConfig()
	}

	private fun handleConfig(config: ConfigModel) {
		val configRepository = ConfigRepository.getInstance(this)
		if (config.forceUpdate && configRepository.forceUpdateValid() && forceUpdateDialog == null) {
			val forceUpdateDialog = AlertDialog.Builder(this, R.style.CovidCertificate_AlertDialogStyle)
				.setTitle(R.string.force_update_title)
				.setMessage(R.string.force_update_text)
				.setPositiveButton(R.string.force_update_button, null)
				.setCancelable(false)
				.create()
			forceUpdateDialog.setOnShowListener {
				forceUpdateDialog.getButton(DialogInterface.BUTTON_POSITIVE)
					.setOnClickListener {
						val packageName = packageName
						UrlUtil.openUrl(this@MainActivity, "market://details?id=$packageName")
					}
			}
			this.forceUpdateDialog = forceUpdateDialog
			forceUpdateDialog.show()
		}
	}

}