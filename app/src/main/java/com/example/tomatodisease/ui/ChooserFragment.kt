package com.example.tomatodisease.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.tomatodisease.databinding.FragmentChooserBinding
import com.example.tomatodisease.domain.model.Feature
import com.example.tomatodisease.domain.model.features
import com.example.tomatodisease.ui.adapter.FeatureAdapter

class ChooserFragment : Fragment(), FeatureAdapter.OnItemClickListener {
    private var _binding: FragmentChooserBinding? = null
    private val binding
        get() = _binding!!

    private lateinit var featureAdapter: FeatureAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChooserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        featureAdapter = FeatureAdapter(this)
            .also {
                it.submitList(features)
            }

        binding.apply {
            featureList.apply {
                adapter = featureAdapter
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onItemClick(category: Feature) {
        findNavController().navigate(category.dest)
    }
}