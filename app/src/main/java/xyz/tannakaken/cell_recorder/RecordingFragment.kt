package xyz.tannakaken.cell_recorder

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.navigation.fragment.findNavController
import xyz.tannakaken.cell_recorder.databinding.FragmentRecordingBinding

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class RecordingFragment : Fragment() {

    private var _binding: FragmentRecordingBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            stopLogging()
        }
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentRecordingBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSecond.setOnClickListener {
            stopLogging()
        }
    }

    private fun stopLogging() {
        val mainActivity = activity as MainActivity
        mainActivity.stopLogging()
        findNavController().navigate(R.id.action_RecordingFragment_to_MainFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}