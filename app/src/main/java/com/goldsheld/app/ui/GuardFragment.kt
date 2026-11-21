package com.goldsheld.app.ui

import android.widget.LinearLayout
import com.goldsheld.app.R

/** Protection switches: power menu, APK install, uninstall, app info. */
class GuardFragment : CardsFragment() {
    override val titleRes = R.string.guard_title
    override val subtitleRes = R.string.guard_sub

    override fun buildList(list: LinearLayout) {
        val inflater = layoutInflater
        list.addSwitchCard(
            inflater,
            SwitchCardData(
                R.drawable.ic_power, "পাওয়ার মেনু ব্লক",
                "লক চলাকালে পাওয়ার বাটন চেপে বন্ধ বা রিস্টার্ট করা যাবে না। সময় শেষ বা লোকেশনে পৌঁছালে নিজে খুলে যাবে।",
                true, "Device Owner চালু আছে — পূর্ণ সুরক্ষা", true
            )
        )
        list.addSwitchCard(
            inflater,
            SwitchCardData(
                R.drawable.ic_download, "APK ইনস্টল ব্লক",
                "Play Store ও ফাইল থেকে নতুন অ্যাপ ইনস্টল বন্ধ, যাতে নতুন ব্রাউজার বা VPN দিয়ে ফাঁকি দেওয়া না যায়।",
                true, "কাজ করছে", true
            )
        )
        list.addSwitchCard(
            inflater,
            SwitchCardData(
                R.drawable.ic_trash, "আনইনস্টল ব্লক",
                "লক চলাকালে অ্যাপ মুছে ফেলা যাবে না।",
                true, "Device Owner চালু আছে — পূর্ণ সুরক্ষা", true
            )
        )
        list.addSwitchCard(
            inflater,
            SwitchCardData(
                R.drawable.ic_info, "অ্যাপ ইনফো ব্লক",
                "সেটিংসে অ্যাপ ইনফো পেজ খুললে Force stop বাটন ধরার আগেই Home-এ ফেরত পাঠায়।",
                true, "আংশিক — অ্যাক্সেসিবিলিটি বন্ধ থাকলে কাজ করে না", false
            )
        )
    }
}
