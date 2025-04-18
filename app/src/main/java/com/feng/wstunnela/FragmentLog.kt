package com.feng.wstunnela

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.feng.wstunnela.databinding.FragmentLogBinding
import kotlin.getValue

class FragmentLog : Fragment() {
    private var _binding: FragmentLogBinding? = null
    private val binding get() = _binding!!
    private val vm: vm by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentLogBinding.inflate(inflater, container, false)
        binding.textView4.movementMethod = ScrollingMovementMethod.getInstance()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.runlog_textvie4.observe(viewLifecycleOwner) { running_log ->
            binding.textView4.text = running_log
        }
    }
}