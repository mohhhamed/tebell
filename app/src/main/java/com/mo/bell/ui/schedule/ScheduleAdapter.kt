package com.mo.bell.ui.schedule

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mo.bell.R
import com.mo.bell.data.database.entities.Schedule
import com.mo.bell.databinding.ItemScheduleClassBinding

class ScheduleAdapter(
    private val onClassClicked: (Schedule) -> Unit
) : ListAdapter<Schedule, ScheduleAdapter.ScheduleViewHolder>(ScheduleDiffCallback()) {

    private var currentClass: Schedule? = null
    private var nextClass: Schedule? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val binding = ItemScheduleClassBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ScheduleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun setCurrentClass(current: Schedule?) {
        val oldCurrent = currentClass
        currentClass = current
        
        // تحديث العناصر المتأثرة
        updateAffectedItems(oldCurrent, current)
    }

    fun setNextClass(next: Schedule?) {
        val oldNext = nextClass
        nextClass = next
        
        // تحديث العناصر المتأثرة
        updateAffectedItems(oldNext, next)
    }

    private fun updateAffectedItems(oldItem: Schedule?, newItem: Schedule?) {
        currentList.forEachIndexed { index, schedule ->
            if (schedule.id == oldItem?.id || schedule.id == newItem?.id) {
                notifyItemChanged(index)
            }
        }
    }

    inner class ScheduleViewHolder(
        private val binding: ItemScheduleClassBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(schedule: Schedule) {
            binding.apply {
                // معلومات الحصة
                classNameText.text = schedule.className
                subjectNameText.text = schedule.subjectName
                teacherNameText.text = schedule.teacherName
                timeText.text = "${schedule.startTime} - ${schedule.endTime}"

                // تحديد نوع الحصة
                val classType = when {
                    schedule.id == currentClass?.id -> ClassType.CURRENT
                    schedule.id == nextClass?.id -> ClassType.NEXT
                    else -> ClassType.NORMAL
                }

                // تطبيق التصميم حسب النوع
                applyClassTypeStyle(classType)

                // إعداد النقر
                root.setOnClickListener {
                    onClassClicked(schedule)
                }
            }
        }

        private fun applyClassTypeStyle(type: ClassType) {
            val context = binding.root.context
            
            when (type) {
                ClassType.CURRENT -> {
                    binding.statusIndicator.setBackgroundColor(
                        context.getColor(R.color.current_class_color)
                    )
                    binding.statusText.text = context.getString(R.string.current_class)
                    binding.statusText.visibility = android.view.View.VISIBLE
                    binding.root.alpha = 1.0f
                }
                ClassType.NEXT -> {
                    binding.statusIndicator.setBackgroundColor(
                        context.getColor(R.color.next_class_color)
                    )
                    binding.statusText.text = context.getString(R.string.next_class)
                    binding.statusText.visibility = android.view.View.VISIBLE
                    binding.root.alpha = 1.0f
                }
                ClassType.NORMAL -> {
                    binding.statusIndicator.setBackgroundColor(
                        context.getColor(R.color.normal_class_color)
                    )
                    binding.statusText.visibility = android.view.View.GONE
                    binding.root.alpha = 0.7f
                }
            }
        }
    }

    private enum class ClassType {
        CURRENT, NEXT, NORMAL
    }

    private class ScheduleDiffCallback : DiffUtil.ItemCallback<Schedule>() {
        override fun areItemsTheSame(oldItem: Schedule, newItem: Schedule): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Schedule, newItem: Schedule): Boolean {
            return oldItem == newItem
        }
    }
}

