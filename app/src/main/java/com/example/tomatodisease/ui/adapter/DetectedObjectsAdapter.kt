package com.example.tomatodisease.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tomatodisease.databinding.ItemDetectedObjectsBinding
import com.example.tomatodisease.domain.model.DetectedObjectItem

typealias onItemClick = (DetectedObjectItem) -> Unit

class DetectedObjectsAdapter(
    private val listener: onItemClick
): ListAdapter<DetectedObjectItem, DetectedObjectsAdapter.ViewHolder>(ITEM_COMPARATOR) {

    inner class ViewHolder(val binding: ItemDetectedObjectsBinding): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    getItem(pos)?.let {
                        listener(it)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDetectedObjectsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { item ->
            holder.binding.apply {
                item.id?.let {
                    tvId.text = it.toString()
                    layoutId.isVisible = true
                }

                item.confidence?.let {
                    tvConfidence.text = it.toString()
                    layoutConfidence.isVisible = true
                }

                tvClassName.text = if (item.className.isNullOrEmpty()) "Not Found" else item.className

                imgDetectedObject.setImageBitmap(item.imageBitmap)
            }
        }
    }

    companion object {
        private val ITEM_COMPARATOR = object : DiffUtil.ItemCallback<DetectedObjectItem>() {
            override fun areItemsTheSame(
                oldItem: DetectedObjectItem,
                newItem: DetectedObjectItem,
            ): Boolean = oldItem == newItem

            override fun areContentsTheSame(
                oldItem: DetectedObjectItem,
                newItem: DetectedObjectItem,
            ): Boolean = oldItem.id == newItem.id
        }
    }
}