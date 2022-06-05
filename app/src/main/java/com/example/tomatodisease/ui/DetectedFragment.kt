package com.example.tomatodisease.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.tomatodisease.databinding.FragmentDetectedObjectsBinding
import com.example.tomatodisease.ui.adapter.DetectedObjectsAdapter
import com.example.tomatodisease.ui.viewmodels.MainViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DetectedFragment : Fragment() {
    private var _binding: FragmentDetectedObjectsBinding? = null
    private val binding get() = _binding!!

    private val viewModel by activityViewModels<MainViewModel>()

    private lateinit var detectedObjectsAdapter: DetectedObjectsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetectedObjectsBinding.inflate(inflater, container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        detectedObjectsAdapter = DetectedObjectsAdapter {
            findNavController().navigate(DetectedFragmentDirections.toResult(it))
        }

        binding.apply {
            recyclerViewDetectedObjects.apply {
                adapter = detectedObjectsAdapter
            }

            toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }
        }

        subscribeObserver()
    }

    private fun subscribeObserver() {
        collectLatestLifecycleFlow(viewModel.detectedObjects) { item ->
            detectedObjectsAdapter.submitList(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun <T> Fragment.collectLatestLifecycleFlow(
        flow: Flow<T>,
        collect: suspend (T) -> Unit,
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                flow.collectLatest(collect)
            }
        }
    }
}