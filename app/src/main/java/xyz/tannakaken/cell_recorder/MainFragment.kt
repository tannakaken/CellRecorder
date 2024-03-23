package xyz.tannakaken.cell_recorder

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import xyz.tannakaken.cell_recorder.databinding.FragmentMainBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun getLocation() {
        val mainActivity = activity as MainActivity
        val prettyJson = Json { // this returns the JsonBuilder
            prettyPrint = true
            // optional: specify indent
            prettyPrintIndent = " "
        }
        mainActivity.getLog { cellLogRow ->
            binding.textView.text = prettyJson.encodeToString(cellLogRow)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonStart.setOnClickListener {
            val mainActivity = activity as MainActivity
            mainActivity.startLogging()
            findNavController().navigate(R.id.action_MainFragment_to_RecordingFragment)
        }
        binding.buttonUpdate.setOnClickListener {
            getLocation()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}