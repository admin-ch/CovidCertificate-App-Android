package ch.admin.bag.covidcertificate.common.util

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import ch.admin.bag.covidcertificate.common.R
import ch.admin.bag.covidcertificate.common.databinding.FragmentLanguageSettingBinding

class LanguageDialog : DialogFragment() {


    class LanguageSettingFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }

    private var _binding: FragmentLanguageSettingBinding? = null
    private val binding get() = _binding!!

    private lateinit var language: String

    private lateinit var manager: SharedPreferences


    companion object {
        const val TAG = "LanguageDialog"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLanguageSettingBinding.inflate(inflater, container, false)

        manager = PreferenceManager.getDefaultSharedPreferences(requireContext())
        language = manager.getString("language", "default") ?: "default"

        childFragmentManager
            .beginTransaction()
            .replace(R.id.settings, LanguageSettingFragment())
            .commit()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupClickListeners() {
        binding.btnNegative.setOnClickListener {
            // Undo language change and close dialog
            manager.edit().putString("language", language).apply()
            dismiss()
        }
        binding.btnPositive.setOnClickListener {
            //If language changed, restart activity
            val newLanguage = manager.getString("language", "default") ?: "default"

            if (newLanguage != language){
                val intent = requireActivity().intent
                intent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            or Intent.FLAG_ACTIVITY_NO_ANIMATION
                )
                requireActivity().overridePendingTransition(0, 0)
                requireActivity().finish()

                requireActivity().overridePendingTransition(0, 0)
                startActivity(intent)
            }

        }
    }

}