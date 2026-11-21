package com.goldsheld.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.goldsheld.app.R
import com.goldsheld.app.databinding.FragmentLockBinding
import com.goldsheld.app.databinding.ItemPlaceRowBinding

/** Lock setup: condition (time / location / both), time chips, saved place, ANY / ALL rule. */
class LockFragment : Fragment() {

    private var _binding: FragmentLockBinding? = null
    private val binding get() = _binding!!

    private val modeNames = listOf("সময়", "লোকেশন", "সময় + লোকেশন")
    private val timeNames = listOf("৩০ মিনিট", "১ ঘণ্টা", "৩ ঘণ্টা", "৬ ঘণ্টা", "১২ ঘণ্টা", "২৪ ঘণ্টা", "কাস্টম")
    private val places = listOf("অফিস" to "ব্যাসার্ধ ২০০ মি", "বাসা" to "ব্যাসার্ধ ১৫০ মি", "নতুন জায়গা যোগ করুন" to "বর্তমান অবস্থান থেকে")

    private var mode = 2
    private var time = 3
    private var place = 0
    private var ruleAny = true

    private val modeViews = ArrayList<TextView>()
    private val timeViews = ArrayList<TextView>()
    private val placeViews = ArrayList<ItemPlaceRowBinding>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ctx = requireContext()
        modeViews.clear(); timeViews.clear(); placeViews.clear()

        modeNames.forEachIndexed { i, name ->
            val tv = TextView(ctx)
            tv.text = name
            tv.gravity = android.view.Gravity.CENTER
            tv.minHeight = ctx.dp(44)
            tv.textSize = 14f
            tv.setTypeface(null, android.graphics.Typeface.BOLD)
            tv.isClickable = true
            tv.isFocusable = true
            tv.setOnClickListener { mode = i; refresh() }
            binding.modes.addView(tv, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            modeViews.add(tv)
        }
        timeNames.forEachIndexed { i, name ->
            val pill = ctx.pill(name, ChipStyle.UNSELECTED, 44)
            pill.isClickable = true
            pill.isFocusable = true
            pill.setOnClickListener { time = i; refresh() }
            binding.times.addView(pill)
            timeViews.add(pill)
        }
        places.forEachIndexed { i, (name, sub) ->
            val row = ItemPlaceRowBinding.inflate(layoutInflater, binding.places, false)
            row.name.text = name
            row.sub.text = sub
            row.root.setOnClickListener { place = i; refresh() }
            binding.places.addView(row.root)
            placeViews.add(row)
        }
        binding.ruleAny.setOnClickListener { ruleAny = true; refresh() }
        binding.ruleAll.setOnClickListener { ruleAny = false; refresh() }
        binding.btnStart.setOnClickListener { ctx.demoToast() }
        refresh()
    }

    private fun refresh() {
        val ctx = requireContext()
        modeViews.forEachIndexed { i, tv ->
            if (i == mode) {
                tv.setBackgroundResource(R.drawable.bg_segment_selected)
                tv.setTextColor(ctx.color(R.color.on_accent))
            } else {
                tv.background = null
                tv.setTextColor(ctx.color(R.color.text))
            }
        }
        timeViews.forEachIndexed { i, tv -> ctx.applyPill(tv, if (i == time) ChipStyle.SELECTED else ChipStyle.UNSELECTED) }
        placeViews.forEachIndexed { i, row ->
            row.radio.setImageResource(if (i == place) R.drawable.radio_on else R.drawable.radio_off)
        }
        binding.ruleAny.setBackgroundResource(if (ruleAny) R.drawable.bg_option_selected else R.drawable.bg_option)
        binding.ruleAll.setBackgroundResource(if (!ruleAny) R.drawable.bg_option_selected else R.drawable.bg_option)

        val timeText = timeNames[time]
        val placeText = places[place].first
        val before = "তার আগে পাওয়ার মেনু, অ্যাপ, সাইট ও ইনস্টল বন্ধ থাকবে।"
        val lead = when (mode) {
            0 -> "$timeText পার হলে লক খুলবে।"
            1 -> "$placeText-এ পৌঁছালে লক খুলবে।"
            else -> if (ruleAny) "$timeText পার হলে অথবা $placeText-এ পৌঁছালে লক খুলবে।"
            else "$timeText পার হলে এবং $placeText-এ পৌঁছালে লক খুলবে।"
        }
        binding.summary.text = "সারাংশ: $lead $before"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
