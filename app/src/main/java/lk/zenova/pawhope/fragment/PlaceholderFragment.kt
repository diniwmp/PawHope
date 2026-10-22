package lk.zenova.pawhope.fragment

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import lk.zenova.pawhope.R

class PlaceholderFragment : Fragment() {

    companion object {
        private const val ARG_TITLE = "title"

        fun newInstance(title: String): PlaceholderFragment {
            return PlaceholderFragment().apply {
                arguments = Bundle().apply { putString(ARG_TITLE, title) }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return TextView(requireContext()).apply {
            text = arguments?.getString(ARG_TITLE) ?: ""
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(requireContext().getColor(R.color.text_primary))
        }
    }
}