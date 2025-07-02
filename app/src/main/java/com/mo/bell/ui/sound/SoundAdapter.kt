package com.mo.bell.ui.sound

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mo.bell.audio.BellSoundType
import com.mo.bell.audio.SoundInfo
import com.mo.bell.databinding.ItemSoundBinding

class SoundAdapter(
    private val onSoundSelected: (BellSoundType) -> Unit
) : ListAdapter<SoundInfo, SoundAdapter.SoundViewHolder>(SoundDiffCallback()) {

    private var selectedSoundType: BellSoundType = BellSoundType.DEFAULT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SoundViewHolder {
        val binding = ItemSoundBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SoundViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SoundViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun setSelectedSound(soundType: BellSoundType) {
        val oldSelectedType = selectedSoundType
        selectedSoundType = soundType
        
        // تحديث العناصر المتأثرة فقط
        currentList.forEachIndexed { index, soundInfo ->
            if (soundInfo.type == oldSelectedType || soundInfo.type == soundType) {
                notifyItemChanged(index)
            }
        }
    }

    inner class SoundViewHolder(
        private val binding: ItemSoundBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(soundInfo: SoundInfo) {
            binding.apply {
                soundNameText.text = soundInfo.name
                soundDescriptionText.text = soundInfo.description
                
                // تحديد حالة الاختيار
                val isSelected = soundInfo.type == selectedSoundType
                soundRadioButton.isChecked = isSelected
                
                // إعداد النقر
                root.setOnClickListener {
                    onSoundSelected(soundInfo.type)
                }
                
                soundRadioButton.setOnClickListener {
                    onSoundSelected(soundInfo.type)
                }
            }
        }
    }

    private class SoundDiffCallback : DiffUtil.ItemCallback<SoundInfo>() {
        override fun areItemsTheSame(oldItem: SoundInfo, newItem: SoundInfo): Boolean {
            return oldItem.type == newItem.type
        }

        override fun areContentsTheSame(oldItem: SoundInfo, newItem: SoundInfo): Boolean {
            return oldItem == newItem
        }
    }
}

