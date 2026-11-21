package com.goldsheld.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.goldsheld.app.databinding.FragmentCardsBinding

/** Base for screens that are a title, a subtitle and a vertical list of cards. */
abstract class CardsFragment : Fragment() {

    private var _binding: FragmentCardsBinding? = null
    protected val binding get() = _binding!!

    protected abstract val titleRes: Int
    protected abstract val subtitleRes: Int
    protected abstract fun buildList(list: LinearLayout)

    /** Optional bottom outline button; return its text or null to hide it. */
    protected open fun bottomButtonText(): String? = null

    /** Default: the button does nothing. Fragments that show one override this. */
    protected open fun onBottomButton() {
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCardsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.title.setText(titleRes)
        binding.subtitle.setText(subtitleRes)
        buildList(binding.list)
        val bottom = bottomButtonText()
        if (bottom != null) {
            binding.bottomBtn.text = bottom
            binding.bottomBtn.visibility = View.VISIBLE
            binding.bottomBtn.setOnClickListener { onBottomButton() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
