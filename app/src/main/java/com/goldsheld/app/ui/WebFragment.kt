package com.goldsheld.app.ui

import android.widget.LinearLayout
import com.goldsheld.app.R

/** Site block (DNS), keyword block, incognito block and the experimental screen check. */
class WebFragment : CardsFragment() {
    override val titleRes = R.string.web_title
    override val subtitleRes = R.string.web_sub

    override fun buildList(list: LinearLayout) {
        val inflater = layoutInflater
        list.addSwitchCard(
            inflater,
            SwitchCardData(
                R.drawable.ic_globe, "সাইট ব্লক (DNS)",
                "ফোনের ভেতরেই চলা ছোট VPN। অ্যাডাল্ট সাইটের ঠিকানা জিজ্ঞেস করলে \"নেই\" বলে দেয়। আপনার ব্রাউজিং কোথাও যায় না।",
                true,
                tags = listOf("তালিকা চালু" to ChipStyle.ACCENT, "SafeSearch চালু" to ChipStyle.NEUTRAL)
            )
        )
        list.addSwitchCard(
            inflater,
            SwitchCardData(
                R.drawable.ic_search, "কীওয়ার্ড ব্লক",
                "ব্রাউজারের ঠিকানা-বার বা সার্চ বক্সে এই শব্দ লিখলেই ট্যাব বন্ধ হয়ে Home-এ যাবে।",
                true, "ব্রাউজার: Chrome, Firefox, Samsung Internet, Edge", false,
                tags = listOf("porn", "xxx", "sex video", "নগ্ন", "পর্ন").map { it to ChipStyle.NEUTRAL },
                extraDashedTag = "+ যোগ করুন"
            )
        )
        list.addSwitchCard(
            inflater,
            SwitchCardData(
                R.drawable.ic_shield, "ইনকগনিটো", "লক চলাকালে ইনকগনিটো ট্যাব বন্ধ", true
            )
        )
        list.addSwitchCard(
            inflater,
            SwitchCardData(
                R.drawable.ic_screen, "স্ক্রিনে ছবি শনাক্ত",
                "ফোনের ভেতরের মডেল দিয়ে স্ক্রিন যাচাই। ভুল হতে পারে, ব্যাটারি বেশি খরচ হয়। স্ক্রিন ক্যাপচারের অনুমতি লাগে।",
                false, "পরীক্ষামূলক", false
            )
        )
    }
}
