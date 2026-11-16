package com.yuri.alarme1236

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var picker: TimePicker
    private lateinit var startSpinner: Spinner
    private lateinit var hoursInput: EditText
    private lateinit var labelInput: EditText
    private lateinit var listBox: LinearLayout
    private val fmt = SimpleDateFormat("EEE dd/MM HH:mm", Locale("pt", "BR"))

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(48), dp(16), dp(24))
        }

        root.addView(TextView(this).apply {
            text = "Alarme 12x36"
            textSize = 26f
        })

        picker = TimePicker(this).apply { setIs24HourView(true) }
        root.addView(picker)

        root.addView(label("Primeiro disparo"))
        startSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("Próxima vez que der esse horário", "Amanhã", "Depois de amanhã")
            )
        }
        root.addView(startSpinner)

        root.addView(label("Repetir a cada quantas horas? (1 a 48)"))
        hoursInput = EditText(this).apply {
            setText("48")
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        root.addView(hoursInput)

        root.addView(label("Nome do alarme"))
        labelInput = EditText(this).apply {
            setText("Trabalho")
            setSingleLine()
        }
        root.addView(labelInput)

        root.addView(Button(this).apply {
            text = "Criar alarme"
            setOnClickListener { createAlarm() }
        })

        root.addView(TextView(this).apply {
            text = "Seus alarmes"
            textSize = 20f
            setPadding(0, dp(24), 0, dp(8))
        })
        listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(listBox)

        setContentView(ScrollView(this).apply { addView(root) })
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun label(t: String) = TextView(this).apply {
        text = t
        setPadding(0, dp(12), 0, 0)
    }

    private fun createAlarm() {
        val h = hoursInput.text.toString().toIntOrNull()
        if (h == null || h !in 1..48) {
            Toast.makeText(this, "Digite um intervalo entre 1 e 48 horas", Toast.LENGTH_LONG).show()
            return
        }

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, picker.hour)
            set(Calendar.MINUTE, picker.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val offsetDays = startSpinner.selectedItemPosition
        if (offsetDays > 0) {
            cal.add(Calendar.DAY_OF_YEAR, offsetDays)
        } else if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val list = AlarmStore.load(this)
        val name = labelInput.text.toString().trim().ifEmpty { "Alarme" }
        val alarm = Alarm(AlarmStore.nextId(list), name, h, cal.timeInMillis, true)
        list.add(alarm)
        AlarmStore.save(this, list)
        Scheduler.schedule(this, alarm)

        Toast.makeText(
            this,
            "Primeiro disparo: ${fmt.format(Date(alarm.nextTrigger))}",
            Toast.LENGTH_LONG
        ).show()
        render()
    }

    private fun toggle(id: Int, on: Boolean) {
        val list = AlarmStore.load(this)
        val a = list.find { it.id == id } ?: return
        a.enabled = on
        if (on) {
            Scheduler.advancePast(a, System.currentTimeMillis())
            Scheduler.schedule(this, a)
        } else {
            Scheduler.cancel(this, id)
        }
        AlarmStore.save(this, list)
        listBox.post { render() }
    }

    private fun delete(id: Int) {
        Scheduler.cancel(this, id)
        val list = AlarmStore.load(this)
        list.removeAll { it.id == id }
        AlarmStore.save(this, list)
        render()
    }

    private fun render() {
        listBox.removeAllViews()
        val list = AlarmStore.load(this)
        if (list.isEmpty()) {
            listBox.addView(TextView(this).apply { text = "Nenhum alarme ainda." })
            return
        }
        for (a in list) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, dp(8))
            }
            row.addView(TextView(this).apply {
                text = "${a.label}\nA cada ${a.intervalHours}h · próximo: ${fmt.format(Date(a.nextTrigger))}"
                textSize = 16f
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(Switch(this).apply {
                isChecked = a.enabled
                setOnCheckedChangeListener { _, on -> toggle(a.id, on) }
            })
            row.addView(Button(this).apply {
                text = "X"
                setOnClickListener { delete(a.id) }
            })
            listBox.addView(row)
        }
    }
}
