package com.goldsheld.app.ui

import android.app.Activity
import android.content.Context
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.goldsheld.app.Graph
import com.goldsheld.app.R
import com.goldsheld.app.data.prefs.AppSettings
import com.goldsheld.app.data.prefs.ContentFilterStore
import com.goldsheld.app.data.prefs.SettingsKeys
import com.goldsheld.app.domain.ModuleSwitchGuard
import com.goldsheld.app.service.image.ImageClassifier
import com.goldsheld.app.service.image.ImageDetectionController
import com.goldsheld.app.service.image.ImageDetectionService
import com.goldsheld.app.service.vpn.DnsVpnController
import kotlinx.coroutines.launch

/**
 * Site block (DNS), keyword block, incognito block and the experimental screen check.
 * Every switch is real: site block drives the DNS VPN + the filter/module settings
 * through [ModuleSwitchGuard]; keywords come from [ContentFilterStore] (adding is
 * always allowed, removing during a lock is refused); image detection is honest
 * about the missing TFLite model — it refuses to start and says so.
 */
class WebFragment : CardsFragment() {
    override val titleRes = R.string.web_title
    override val subtitleRes = R.string.web_sub

    private var settings: AppSettings? = null
    private var listRef: LinearLayout? = null
    private var vpnEnablePending = false
    private var imageEnablePending = false

    private val vpnConsentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (!vpnEnablePending) return@registerForActivityResult
            vpnEnablePending = false
            val ctx = requireContext()
            if (DnsVpnController.hasConsent(ctx)) {
                DnsVpnController.start(ctx)
            } else {
                Toast.makeText(ctx, R.string.ui_vpn_consent, Toast.LENGTH_LONG).show()
            }
        }

    private val projectionConsentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (!imageEnablePending) return@registerForActivityResult
            imageEnablePending = false
            val ctx = requireContext()
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                ImageDetectionController.holdConsent(result.resultCode, result.data!!)
                viewLifecycleOwner.lifecycleScope.launch {
                    val s = Graph.settings.get()
                    settings = s
                    if (ImageDetectionController.startIfWanted(ctx, s.imageSensitivity)) {
                        Toast.makeText(ctx, R.string.ui_image_on, Toast.LENGTH_SHORT).show()
                    }
                    rebuild()
                }
            } else {
                // No consent, no detection: roll the setting back so the switch stays honest.
                viewLifecycleOwner.lifecycleScope.launch {
                    Graph.settings.set(SettingsKeys.IMAGE_DETECTION, false)
                    ImageDetectionController.setWanted(ctx, false)
                    settings = Graph.settings.get()
                    Toast.makeText(ctx, R.string.ui_image_consent, Toast.LENGTH_LONG).show()
                    rebuild()
                }
            }
        }

    override fun buildList(list: LinearLayout) {
        listRef = list
        val inflater = layoutInflater
        val ctx = requireContext()
        val s = settings
        list.addSwitchCard(
            inflater,
            SwitchCardData(
                R.drawable.ic_globe, "সাইট ব্লক (DNS)",
                "ফোনের ভেতরেই চলা ছোট VPN। অ্যাডাল্ট সাইটের ঠিকানা জিজ্ঞেস করলে \"নেই\" বলে দেয়। আপনার ব্রাউজিং কোথাও যায় না।",
                (s?.moduleSiteBlock ?: true) && (s?.filterEnabled ?: false),
                tags = listOf("তালিকা চালু" to ChipStyle.ACCENT, "SafeSearch চালু" to ChipStyle.NEUTRAL),
                onCheckedChange = { on -> onSiteToggle(on) }
            )
        )
        list.addSwitchCard(
            inflater,
            SwitchCardData(
                R.drawable.ic_search, "কীওয়ার্ড ব্লক",
                "ব্রাউজারের ঠিকানা-বার বা সার্চ বক্সে এই শব্দ লিখলেই ট্যাব বন্ধ হয়ে Home-এ যাবে।",
                ContentFilterStore.keywordsEnabled(ctx) && (s?.moduleKeywordBlock ?: true),
                "ব্রাউজার: Chrome, Firefox, Samsung Internet, Edge", false,
                tags = ContentFilterStore.keywords(ctx).map { it to ChipStyle.NEUTRAL },
                extraDashedTag = "+ যোগ করুন",
                onCheckedChange = { on -> onKeywordToggle(on) },
                onTagClick = { keyword -> onKeywordRemove(keyword) },
                onAddTagClick = { showAddKeywordDialog() }
            )
        )
        list.addSwitchCard(
            inflater,
            SwitchCardData(
                R.drawable.ic_shield, "ইনকগনিটো", "লক চলাকালে ইনকগনিটো ট্যাব বন্ধ",
                s?.blockIncognito ?: false,
                onCheckedChange = { on -> onIncognitoToggle(on) }
            )
        )
        list.addSwitchCard(
            inflater,
            SwitchCardData(
                R.drawable.ic_screen, "স্ক্রিনে ছবি শনাক্ত",
                "ফোনের ভেতরের মডেল দিয়ে স্ক্রিন যাচাই। ভুল হতে পারে, ব্যাটারি বেশি খরচ হয়। স্ক্রিন ক্যাপচারের অনুমতি লাগে।",
                (s?.imageDetection ?: false) && ImageClassifier.modelExists(ctx),
                "পরীক্ষামূলক", false,
                onCheckedChange = { on -> onImageToggle(on) }
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            settings = Graph.settings.get()
            rebuild()
        }
    }

    private fun rebuild() {
        val list = listRef ?: return
        if (!isAdded) return
        list.removeAllViews()
        buildList(list)
    }

    // ------------------------------------------------------------ site block

    private fun onSiteToggle(enabled: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            val ctx = requireContext()
            val result = ModuleSwitchGuard.tryChangeModule(
                Graph.lockManager, Graph.settings, SettingsKeys.MODULE_SITE_BLOCK, "siteBlock", enabled
            )
            if (result == ModuleSwitchGuard.ChangeResult.REFUSED) {
                Toast.makeText(ctx, R.string.ui_switch_refused, Toast.LENGTH_LONG).show()
                rebuild()
                return@launch
            }
            Graph.settings.set(SettingsKeys.FILTER_ENABLED, enabled)
            Graph.settings.set(SettingsKeys.DNS_VPN_ENABLED, enabled)
            settings = Graph.settings.get()
            DnsVpnController.setWanted(ctx, enabled)
            if (enabled) {
                runCatching { Graph.blocklist.reload() }
                val consentIntent = DnsVpnController.prepareIntent(ctx)
                if (consentIntent == null) {
                    DnsVpnController.start(ctx)
                } else {
                    vpnEnablePending = true
                    vpnConsentLauncher.launch(consentIntent)
                }
            } else {
                DnsVpnController.stop(ctx)
            }
            rebuild()
        }
    }

    // ------------------------------------------------------------ keywords

    private fun onKeywordToggle(enabled: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            val ctx = requireContext()
            val result = ModuleSwitchGuard.tryChangeModule(
                Graph.lockManager, Graph.settings, SettingsKeys.MODULE_KEYWORD_BLOCK, "keywordBlock", enabled
            )
            if (result == ModuleSwitchGuard.ChangeResult.REFUSED) {
                Toast.makeText(ctx, R.string.ui_switch_refused, Toast.LENGTH_LONG).show()
                rebuild()
                return@launch
            }
            ContentFilterStore.setKeywordsEnabled(ctx, enabled)
            settings = Graph.settings.get()
            rebuild()
        }
    }

    private fun onKeywordRemove(keyword: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val ctx = requireContext()
            val removed = ModuleSwitchGuard.tryRemoveKeyword(Graph.lockManager, ctx, keyword)
            if (!removed) {
                Toast.makeText(ctx, R.string.ui_keyword_remove_refused, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(ctx, R.string.ui_keyword_removed, Toast.LENGTH_SHORT).show()
                rebuild()
            }
        }
    }

    private fun showAddKeywordDialog() {
        val ctx = requireContext()
        val input = EditText(ctx).apply {
            hint = getString(R.string.ui_keyword_hint)
            setPadding(ctx.dp(16), ctx.dp(12), ctx.dp(16), ctx.dp(12))
        }
        AlertDialog.Builder(ctx)
            .setTitle(R.string.ui_keyword_title)
            .setView(input)
            .setPositiveButton(R.string.ui_add) { _, _ ->
                val ok = ContentFilterStore.addKeyword(ctx, input.text.toString())
                Toast.makeText(
                    ctx,
                    if (ok) R.string.ui_keyword_added else R.string.ui_keyword_invalid,
                    Toast.LENGTH_SHORT
                ).show()
                if (ok) rebuild()
            }
            .setNegativeButton(R.string.ui_cancel, null)
            .show()
    }

    // ------------------------------------------------------------ incognito

    private fun onIncognitoToggle(enabled: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            val ctx = requireContext()
            val result = ModuleSwitchGuard.tryChangeModule(
                Graph.lockManager, Graph.settings, SettingsKeys.BLOCK_INCOGNITO, "incognito", enabled
            )
            if (result == ModuleSwitchGuard.ChangeResult.REFUSED) {
                Toast.makeText(ctx, R.string.ui_switch_refused, Toast.LENGTH_LONG).show()
            } else {
                settings = Graph.settings.get()
            }
            rebuild()
        }
    }

    // ------------------------------------------------------------ image detection

    private fun onImageToggle(enabled: Boolean) {
        val ctx = requireContext()
        if (enabled && !ImageClassifier.modelExists(ctx)) {
            // No TFLite model is bundled: the engine refuses to start, so say so plainly.
            Toast.makeText(ctx, R.string.image_detection_model_missing, Toast.LENGTH_LONG).show()
            rebuild()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            if (!enabled) {
                Graph.settings.set(SettingsKeys.IMAGE_DETECTION, false)
                ImageDetectionController.setWanted(ctx, false)
                ImageDetectionService.stop(ctx)
                settings = Graph.settings.get()
                rebuild()
                return@launch
            }
            val s = Graph.settings.get()
            Graph.settings.set(SettingsKeys.IMAGE_DETECTION, true)
            ImageDetectionController.setWanted(ctx, true)
            settings = s.copy(imageDetection = true)
            if (ImageDetectionController.consent == null) {
                val manager = ctx.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                imageEnablePending = true
                projectionConsentLauncher.launch(manager.createScreenCaptureIntent())
            } else if (ImageDetectionController.startIfWanted(ctx, s.imageSensitivity)) {
                Toast.makeText(ctx, R.string.ui_image_on, Toast.LENGTH_SHORT).show()
            }
            rebuild()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listRef = null
    }
}
