package com.goldsheld.app.ui

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.lifecycleScope
import com.goldsheld.app.Graph
import com.goldsheld.app.R
import com.goldsheld.app.admin.DevicePolicyHelper
import com.goldsheld.app.data.prefs.AppSettings
import com.goldsheld.app.data.prefs.SettingsKeys
import com.goldsheld.app.databinding.ItemSwitchCardBinding
import com.goldsheld.app.domain.ModuleSwitchGuard
import kotlinx.coroutines.launch

/**
 * Protection switches: power menu, APK install, uninstall, app info. Each switch reflects
 * the real [AppSettings] module switch and writes back through [ModuleSwitchGuard]: while
 * a lock is active, turning a switch off is refused (the switch reverts and a toast
 * explains); turning one on strengthens the running lock at once. Notes reflect the
 * real Device Owner and accessibility state.
 */
class GuardFragment : CardsFragment() {
    override val titleRes = R.string.guard_title
    override val subtitleRes = R.string.guard_sub

    private var settings: AppSettings? = null
    private var powerCard: ItemSwitchCardBinding? = null
    private var apkCard: ItemSwitchCardBinding? = null
    private var uninstallCard: ItemSwitchCardBinding? = null
    private var appInfoCard: ItemSwitchCardBinding? = null

    override fun buildList(list: LinearLayout) {
        val inflater = layoutInflater
        val ctx = requireContext()
        val s = settings
        val deviceOwner = DevicePolicyHelper.isDeviceOwner(ctx)
        val a11yOn = Graph.integrity.isAccessibilityEnabled()
        powerCard = list.addSwitchCard(
            inflater,
            SwitchCardData(
                R.drawable.ic_power, "পাওয়ার মেনু ব্লক",
                "লক চলাকালে পাওয়ার বাটন চেপে বন্ধ বা রিস্টার্ট করা যাবে না। সময় শেষ বা লোকেশনে পৌঁছালে নিজে খুলে যাবে।",
                s?.modulePowerMenu ?: true,
                if (deviceOwner) "Device Owner চালু আছে — পূর্ণ সুরক্ষা"
                else "Device Owner নেই — অ্যাক্সেসিবিলিটি দিয়ে আংশিক সুরক্ষা",
                deviceOwner,
                onCheckedChange = { on ->
                    onModuleToggle(SettingsKeys.MODULE_POWER_MENU, "powerMenu", on, powerCard)
                }
            )
        )
        apkCard = list.addSwitchCard(
            inflater,
            SwitchCardData(
                R.drawable.ic_download, "APK ইনস্টল ব্লক",
                "Play Store ও ফাইল থেকে নতুন অ্যাপ ইনস্টল বন্ধ, যাতে নতুন ব্রাউজার বা VPN দিয়ে ফাঁকি দেওয়া না যায়।",
                s?.moduleApkInstall ?: true,
                when {
                    deviceOwner -> "Device Owner চালু আছে — পূর্ণ সুরক্ষা"
                    a11yOn -> "কাজ করছে"
                    else -> "অ্যাক্সেসিবিলিটি বন্ধ — কাজ করছে না"
                },
                deviceOwner || a11yOn,
                onCheckedChange = { on ->
                    onModuleToggle(SettingsKeys.MODULE_APK_INSTALL, "apkInstall", on, apkCard)
                }
            )
        )
        uninstallCard = list.addSwitchCard(
            inflater,
            SwitchCardData(
                R.drawable.ic_trash, "আনইনস্টল ব্লক",
                "লক চলাকালে অ্যাপ মুছে ফেলা যাবে না।",
                s?.moduleUninstall ?: true,
                if (deviceOwner) "Device Owner চালু আছে — পূর্ণ সুরক্ষা"
                else "Device Owner নেই — অ্যাক্সেসিবিলিটি দিয়ে আংশিক সুরক্ষা",
                deviceOwner,
                onCheckedChange = { on ->
                    onModuleToggle(SettingsKeys.MODULE_UNINSTALL, "uninstall", on, uninstallCard)
                }
            )
        )
        appInfoCard = list.addSwitchCard(
            inflater,
            SwitchCardData(
                R.drawable.ic_info, "অ্যাপ ইনফো ব্লক",
                "সেটিংসে অ্যাপ ইনফো পেজ খুললে Force stop বাটন ধরার আগেই Home-এ ফেরত পাঠায়।",
                s?.moduleAppInfo ?: true,
                if (a11yOn) "কাজ করছে" else "আংশিক — অ্যাক্সেসিবিলিটি বন্ধ থাকলে কাজ করে না",
                a11yOn,
                onCheckedChange = { on ->
                    onModuleToggle(SettingsKeys.MODULE_APP_INFO, "appInfo", on, appInfoCard)
                }
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            settings = Graph.settings.get()
            // Rebuild once with the real switch states (the first build used defaults).
            binding.list.removeAllViews()
            buildList(binding.list)
        }
    }

    private fun onModuleToggle(
        key: Preferences.Key<Boolean>,
        module: String,
        enabled: Boolean,
        card: ItemSwitchCardBinding?
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = ModuleSwitchGuard.tryChangeModule(
                Graph.lockManager, Graph.settings, key, module, enabled
            )
            if (result == ModuleSwitchGuard.ChangeResult.REFUSED) {
                // Reverting isChecked does not re-fire the click listener.
                card?.sw?.isChecked = !enabled
                Toast.makeText(requireContext(), R.string.ui_switch_refused, Toast.LENGTH_LONG).show()
            } else {
                settings = Graph.settings.get()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        powerCard = null
        apkCard = null
        uninstallCard = null
        appInfoCard = null
    }
}
