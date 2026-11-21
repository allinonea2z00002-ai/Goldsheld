package com.goldsheld.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.goldsheld.app.R
import com.goldsheld.app.databinding.FragmentHomeBinding
import com.goldsheld.app.databinding.ItemStatusRowBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private data class Row(val label: String, val state: String, val ok: Boolean)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ctx = requireContext()
        binding.clock.text = "02:14:35"
        binding.percent.text = "৬২% সম্পন্ন"
        binding.ring.progress = 0.62f

        val rows = listOf(
            Row("পাওয়ার মেনু ব্লক", "চালু", true),
            Row("অ্যাপ ব্লক (৫টি)", "বন্ধ — অনুমতি নেই", false),
            Row("অ্যাপের ফিচার ব্লক", "বন্ধ — অনুমতি নেই", false),
            Row("সাইট ব্লক (DNS)", "চালু", true),
            Row("কীওয়ার্ড ব্লক", "বন্ধ — অনুমতি নেই", false),
            Row("অ্যাপ ইনস্টল / আনইনস্টল ব্লক", "চালু", true)
        )
        rows.forEachIndexed { index, row ->
            val item = ItemStatusRowBinding.inflate(layoutInflater, binding.statusList, false)
            item.label.text = row.label
            item.state.text = row.state
            item.state.setTextColor(ctx.color(if (row.ok) R.color.ok else R.color.warn_text))
            binding.statusList.addView(item.root)
            if (index < rows.lastIndex) {
                val line = View(ctx)
                line.setBackgroundColor(ctx.color(R.color.divider))
                binding.statusList.addView(line, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(1)))
            }
        }
        binding.btnEmergency.setOnClickListener { ctx.demoToast() }
        binding.btnUrge.setOnClickListener { ctx.demoToast() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
