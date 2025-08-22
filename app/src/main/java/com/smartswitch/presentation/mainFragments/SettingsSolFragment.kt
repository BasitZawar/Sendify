package com.smartswitch.presentation.mainFragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import com.smartswitch.R
import com.smartswitch.databinding.FragmentSettingsSolBinding
import com.smartswitch.utils.extensions.handleBackPressWithAction
import com.smartswitch.utils.extensions.isAlive
import com.smartswitch.utils.extensions.setSafeOnClickListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.core.content.edit

@AndroidEntryPoint
class SettingsSolFragment : Fragment() {
    private var _binding: FragmentSettingsSolBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsSolBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isAlive { activityContext ->

            (activityContext as FragmentActivity).handleBackPressWithAction {
                findNavController().popBackStack()
            }

            // Handle toolbar back button press
            binding.headerLayout.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            binding.shareCardView.setOnClickListener {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "${getString(R.string.app_name)}: https://play.google.com/store/apps/details?id=${activityContext.packageName}"
                    )
                }
                startActivity(shareIntent)
            }

            binding.rateUsCardView.setOnClickListener {
                try {
                    val rateIntent =
                        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${activityContext.packageName}"))
                    startActivity(rateIntent)
                } catch (e: Exception) {
                    try {
                        val uriUrl = Uri.parse("https://market.android.com/details?id=${activityContext.packageName}")
                        startActivity(Intent(Intent.ACTION_VIEW, uriUrl))
                    } catch (e: Exception) {
                        Toast.makeText(activityContext, "No Application Found to open link", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }


            binding.privacyCardView.setOnClickListener {
                val url = "https://solutionoflogicsprivacy.blogspot.com/?m=1"
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(activityContext, "Couldn't launch the market", Toast.LENGTH_SHORT).show()
                }
            }

            binding.moreAppCardView.setOnClickListener {
                try {
                    val rateIntent =
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/dev?id=5121071137067556477"))
                    startActivity(rateIntent)
                } catch (e: Exception) {
                    try {
                        val uriUrl = Uri.parse("https://play.google.com/store/apps/dev?id=5121071137067556477")
                        startActivity(Intent(Intent.ACTION_VIEW, uriUrl))
                    } catch (e: Exception) {
                        Toast.makeText(activityContext, "No Application Found to open link", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }


            setupClickListeners(activityContext)
            bindSwitchToPreference(binding.themeSwitch)        }
    }

    private fun bindSwitchToPreference(switch: SwitchCompat) {
        Log.d("theme",sharedPreferences.getBoolean(Theme_STATUS_PREFERENCE_KEY, false).toString())
        switch.isChecked = sharedPreferences.getBoolean(Theme_STATUS_PREFERENCE_KEY, false)
        switch.setOnCheckedChangeListener { _, isChecked ->
            //savePreferencesValue(key, isChecked)
            sharedPreferences.edit { putBoolean(Theme_STATUS_PREFERENCE_KEY, isChecked) }
            if (isChecked) {
                Toast.makeText(context, "Dark Mode", Toast.LENGTH_SHORT).show()
                AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)
                findNavController().navigateUp()
            } else {
                Toast.makeText(context, "Light Mode", Toast.LENGTH_SHORT).show()
                AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)
                findNavController().navigateUp()
            }
        }
    }

    private fun setupClickListeners(activityContext: FragmentActivity) {
        binding.languageCardView.setSafeOnClickListener {
            findNavController().navigate(SettingsSolFragmentDirections.actionSettingsSendifyFragmentToLanguageFragment("setting"))
        }

        
//        binding.profileCardView.setSafeOnClickListener {
//            //findNavController().navigate(R.id.action_settingsSendifyFragment_to_profileSendifyFragment)
//            //ThemeSelectionDialog(requireContext()).show()
//        }

        binding.historyCardView.setSafeOnClickListener {
            findNavController().navigate(R.id.action_settingsSendifyFragment_to_historySendifyFragment)
        }

    }


//    private fun navigateToHomeFragment(activityContext: FragmentActivity) {
//        val bottomNavigationView = activityContext.findViewById<BottomNavigationView>(R.id.bottom_navigation)
//
//        bottomNavigationView.selectedItemId = R.id.homeItem
//
//        bottomNavigationView.post {
//            findNavController().popBackStack(R.id.homeSendifyFragment, false)
//        }
//    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "SettingsSendifyFragment"
        const val Theme_STATUS_PREFERENCE_KEY = "theme"
    }

}