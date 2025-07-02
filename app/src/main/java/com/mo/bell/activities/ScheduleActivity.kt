// 📁 app/src/main/java/com/mo/bell/activities/ScheduleActivity.kt (النسخة النهائية والمُصححة)

package com.mo.bell.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mo.bell.R
import com.mo.bell.data.Lesson
import com.mo.bell.databinding.ActivityScheduleBinding
import com.mo.bell.utils.SettingsManager // <-- تم إضافة الـ import هنا لحل الخطأ الحرج

class ScheduleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScheduleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // تم تعريف SettingsManager هنا بشكل صحيح
        val settingsManager = SettingsManager(this)
        val schedule = settingsManager.getSchedule()

        binding.toolbar.title = "الجدول الأسبوعي"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.recyclerViewSchedule.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewSchedule.adapter = ScheduleAdapter(schedule)
    }
}

class ScheduleAdapter(private val lessons: List<Lesson>) : RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val day: TextView = view.findViewById(R.id.tvLessonDay)
        val period: TextView = view.findViewById(R.id.tvLessonPeriod)
        val time: TextView = view.findViewById(R.id.tvLessonTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_lesson, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lesson = lessons[position]
        val context = holder.itemView.context // للوصول إلى strings.xml

        holder.day.text = lesson.day
        // تم تعديل هذا السطر ليستخدم الطريقة الاحترافية ويحل التحذير
        holder.period.text = context.getString(R.string.lesson_period_label, lesson.period)
        holder.time.text = "${lesson.startTime} - ${lesson.endTime}"
    }

    override fun getItemCount() = lessons.size
}