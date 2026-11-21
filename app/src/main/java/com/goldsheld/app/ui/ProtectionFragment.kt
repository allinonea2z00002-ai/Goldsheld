package com.goldsheld.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.goldsheld.app.R
import com.goldsheld.app.databinding.FragmentProtectionBinding

/** Protection tab: a row of sub tabs that switch between the four protection screens. */
class ProtectionFragment : Fragment() {

    private var _binding: FragmentProtectionBinding? = null
    private val binding get() = _binding!!
    private val tabs = ArrayList<TextView>()
    private var selected = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProtectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ctx = requireContext()
        val names = listOf(R.string.sub_lock, R.string.sub_switches, R.string.sub_apps, R.string.sub_web)
        selected = savedInstanceState?.getInt(KEY_SELECTED) ?: 0
        tabs.clear()
        names.forEachIndexed { index, res ->
            val pill = ctx.pill(getString(res), ChipStyle.UNSELECTED, 44)
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.marginEnd = ctx.dp(8)
            pill.isClickable = true
            pill.isFocusable = true
            pill.setOnClickListener { select(index, true) }
            binding.subTabs.addView(pill, lp)
            tabs.add(pill)
        }
        select(selected, savedInstanceState == null)
    }

    private fun select(index: Int, replace: Boolean) {
        selected = index
        val ctx = requireContext()
        tabs.forEachIndexed { i, tv -> ctx.applyPill(tv, if (i == index) ChipStyle.SELECTED else ChipStyle.UNSELECTED) }
        if (!replace) return
        val fragment: Fragment = when (index) {
            0 -> LockFragment()
            1 -> GuardFragment()
            2 -> AppsFragment()
            else -> WebFragment()
        }
        childFragmentManager.beginTransaction().replace(R.id.subContainer, fragment).commit()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECTED, selected)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tabs.clear()
        _binding = null
    }

    private companion object {
        const val KEY_SELECTED = "selected_sub_tab"
    }
}
