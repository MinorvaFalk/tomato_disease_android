package com.example.tomatodisease.ui.adapter


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tomatodisease.databinding.CustomFeatureCardBinding
import com.example.tomatodisease.domain.model.Feature
import com.example.tomatodisease.ui.adapter.FeatureAdapter.ViewHolder

class FeatureAdapter(
    private val listener: OnItemClickListener
): ListAdapter<Feature, ViewHolder>(ITEM_COMPARATOR) {

    // Make sure binding is public, because onBindViewHolder need to access it
    inner class ViewHolder(val binding: CustomFeatureCardBinding): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    getItem(pos)?.let {
                        listener.onItemClick(it)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CustomFeatureCardBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { feature ->
            holder.binding.apply {
                featureTitle.text = feature.name

                feature.description?.let {
                    featureDescription.text = it
                }
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(category: Feature)
    }

    companion object {
        private val ITEM_COMPARATOR = object : DiffUtil.ItemCallback<Feature>() {
            override fun areItemsTheSame(
                oldItem: Feature,
                newItem: Feature,
            ): Boolean = oldItem == newItem

            override fun areContentsTheSame(
                oldItem: Feature,
                newItem: Feature,
            ): Boolean = oldItem.name == newItem.name
        }
    }

}