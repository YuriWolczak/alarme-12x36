package com.yuri.alarme1236

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

class MainActivity : Activity() {

    private val now0 = Calendar.getInstance()
    private var hour = now0.get(Calendar.HOUR_OF_DAY)
    private var minute = now0.get(Calendar.MINUTE)
    private var interval = 48
    private var startOffset = 0

    private lateinit var timeText: TextView
    private lateinit var intervalText: TextView
    private lateinit var seek: SeekBar
    private lateinit var nameInput: EditText
    private lateinit var listBox: LinearLayout
    private val presetChips = LinkedHashMap<Int, TextView>()
    private val startChips = mutableListOf<TextView>()

    private val dayFmt = SimpleDateFormat("EEE dd/MM", Locale("pt", "BR"))
    private val timeFmt = SimpleDateFormat("HH:mm", Locale("pt", "BR"))

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun col(id: Int): Int = getColor(id)

    private fun shape(color: Int, radiusDp: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun tv(t: String, sp: Float, colorId: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = t
            textSize = sp
            setTextColor(col(colorId))
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun caption(t: String) = tv(t, 12f, R.color.muted, true).apply {
        letterSpacing = 0.08f
        setPadding(0, 0, 0, dp(6))
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = shape(col(R.color.card), 24)
        setPadding(dp(20), dp(18), dp(20), dp(20))
        layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = dp(14) }
    }

    private fun chip(label: String) = TextView(this).apply {
        text = label
        gravity = Gravity.CENTER
        textSize = 13.5f
        maxLines = 2
        setPadding(dp(6), dp(8), dp(6), dp(8))
    }

    private fun paintChip(c: TextView, selected: Boolean) {
        c.background = shape(col(if (selected) R.color.accent else R.color.chip), 16)
        c.setTextColor(col(if (selected) R.color.on_accent else R.color.text))
        c.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private fun refreshChips() {
        startChips.forEachIndexed { i, c -> paintChip(c, i == startOffset) }
        presetChips.forEach { (h, c) -> paintChip(c, h == interval) }
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(32))
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(col(R.color.bg))
            isFillViewport = true
            addView(content)
            setOnApplyWindowInsetsListener { v, ins ->
                v.setPadding(0, ins.systemWindowInsetTop, 0, ins.systemWindowInsetBottom)
                ins
            }
        }

        // Cabeçalho
        content.addView(tv("Shiftly", 34f, R.color.text, true))
        content.addView(
            tv("Alarmes que repetem no seu ritmo", 15f, R.color.muted)
                .apply { setPadding(0, dp(2), 0, dp(20)) }
        )

        // Cartão: horário e primeiro disparo
        val timeCard = card()
        timeCard.addView(caption("HORÁRIO DO ALARME"))
        timeText = tv("", 64f, R.color.accent, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(2))
            setOnClickListener { pickTime() }
        }
        timeCard.addView(timeText, LinearLayout.LayoutParams(MATCH, WRAP))
        timeCard.addView(
            tv("Toque no horário para mudar", 13f, R.color.muted).apply {
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(18))
            },
            LinearLayout.LayoutParams(MATCH, WRAP)
        )
        timeCard.addView(caption("PRIMEIRO DISPARO"))
        val startRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("Próximo horário", "Amanhã", "Depois de amanhã").forEachIndexed { i, label ->
            val c = chip(label)
            c.setOnClickListener { startOffset = i; refreshChips() }
            startChips.add(c)
            startRow.addView(c, LinearLayout.LayoutParams(0, dp(56), 1f).apply {
                if (i > 0) marginStart = dp(8)
            })
        }
        timeCard.addView(startRow, LinearLayout.LayoutParams(MATCH, WRAP))
        content.addView(timeCard)

        // Cartão: intervalo
        val intCard = card()
        intCard.addView(caption("REPETIR A CADA"))
        intervalText = tv("", 30f, R.color.text, true).apply { setPadding(0, 0, 0, dp(14)) }
        intCard.addView(intervalText)
        val presetRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(12, 24, 36, 48).forEachIndexed { i, h ->
            val c = chip("${h}h")
            c.setOnClickListener { setInterval(h, true) }
            presetChips[h] = c
            presetRow.addView(c, LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                if (i > 0) marginStart = dp(8)
            })
        }
        intCard.addView(presetRow, LinearLayout.LayoutParams(MATCH, WRAP))
        seek = SeekBar(this).apply {
            max = 47
            val accent = ColorStateList.valueOf(col(R.color.accent))
            progressTintList = accent
            thumbTintList = accent
            progressBackgroundTintList = ColorStateList.valueOf(col(R.color.chip))
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    if (fromUser) setInterval(p + 1, false)
                }

                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        intCard.addView(seek, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(14) })
        intCard.addView(
            tv("Deslize para escolher de 1 a 48 horas. Escala 12x36: use 48h.", 12.5f, R.color.muted)
                .apply { setPadding(0, dp(6), 0, 0) }
        )
        content.addView(intCard)

        // Cartão: nome
        val nameCard = card()
        nameCard.addView(caption("NOME"))
        nameInput = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setSingleLine()
            setText("Trabalho")
            hint = "Ex.: Trabalho"
            textSize = 17f
            setTextColor(col(R.color.text))
            setHintTextColor(col(R.color.muted))
            background = shape(col(R.color.chip), 14)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        nameCard.addView(nameInput, LinearLayout.LayoutParams(MATCH, WRAP))
        content.addView(nameCard)

        // Botão criar
        val create = Button(this).apply {
            text = "Criar alarme"
            isAllCaps = false
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(col(R.color.on_accent))
            stateListAnimator = null
            background = RippleDrawable(
                ColorStateList.valueOf(0x33FFFFFF),
                shape(col(R.color.accent), 28),
                null
            )
            setOnClickListener { createAlarm() }
        }
        content.addView(create, LinearLayout.LayoutParams(MATCH, dp(56)))

        // Lista
        content.addView(
            tv("Seus alarmes", 22f, R.color.text, true)
                .apply { setPadding(0, dp(28), 0, dp(12)) }
        )
        listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(listBox, LinearLayout.LayoutParams(MATCH, WRAP))

        setContentView(scroll)
        // Tira o foco do campo de nome para o teclado não abrir sozinho.
        scroll.isFocusableInTouchMode = true
        scroll.requestFocus()

        updateTime()
        setInterval(interval, true)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun updateTime() {
        timeText.text = String.format(Locale.US, "%02d:%02d", hour, minute)
    }

    private fun setInterval(h: Int, moveSeek: Boolean) {
        interval = h
        if (moveSeek) seek.progress = h - 1
        intervalText.text = if (h == 1) "1 hora" else "$h horas"
        refreshChips()
    }

    private fun pickTime() {
        TimePickerDialog(this, { _, h, m ->
            hour = h
            minute = m
            updateTime()
        }, hour, minute, true).show()
    }

    private fun createAlarm() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (startOffset > 0) {
            cal.add(Calendar.DAY_OF_YEAR, startOffset)
        } else if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val list = AlarmStore.load(this)
        val name = nameInput.text.toString().trim().ifEmpty { "Alarme" }
        val alarm = Alarm(AlarmStore.nextId(list), name, interval, cal.timeInMillis, true)
        list.add(alarm)
        AlarmStore.save(this, list)
        Scheduler.schedule(this, alarm)

        Toast.makeText(
            this,
            "Primeiro disparo: ${dayFmt.format(Date(alarm.nextTrigger))} às ${timeFmt.format(Date(alarm.nextTrigger))}",
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

    private fun confirmDelete(a: Alarm) {
        AlertDialog.Builder(this)
            .setTitle("Excluir alarme?")
            .setMessage(a.label)
            .setPositiveButton("Excluir") { _, _ -> delete(a.id) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun delete(id: Int) {
        Scheduler.cancel(this, id)
        val list = AlarmStore.load(this)
        list.removeAll { it.id == id }
        AlarmStore.save(this, list)
        render()
    }

    private fun until(t: Long): String {
        val mins = ((t - System.currentTimeMillis()) / 60000).coerceAtLeast(0)
        val h = mins / 60
        val m = mins % 60
        return if (h > 0) "toca em ${h}h ${m}min" else "toca em ${m}min"
    }

    private fun render() {
        listBox.removeAllViews()
        val list = AlarmStore.load(this)
            .sortedWith(compareBy({ !it.enabled }, { it.nextTrigger }))

        if (list.isEmpty()) {
            val empty = card().apply {
                addView(tv("Nenhum alarme ainda. Crie o primeiro acima.", 15f, R.color.muted))
            }
            listBox.addView(empty)
            return
        }

        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        val accent = col(R.color.accent)

        for (a in list) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = shape(col(R.color.card), 24)
                setPadding(dp(20), dp(16), dp(10), dp(16))
                alpha = if (a.enabled) 1f else 0.55f
            }
            val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            info.addView(tv(a.label, 15f, R.color.muted, true))
            info.addView(tv(timeFmt.format(Date(a.nextTrigger)), 36f, R.color.text, true))
            info.addView(
                tv("${dayFmt.format(Date(a.nextTrigger))} · a cada ${a.intervalHours}h", 13f, R.color.muted)
            )
            if (a.enabled) {
                info.addView(
                    tv(until(a.nextTrigger), 13f, R.color.accent, true)
                        .apply { setPadding(0, dp(2), 0, 0) }
                )
            }
            row.addView(info, LinearLayout.LayoutParams(0, WRAP, 1f))

            val sw = Switch(this).apply {
                isChecked = a.enabled
                thumbTintList = ColorStateList(
                    states, intArrayOf(accent, col(R.color.muted))
                )
                trackTintList = ColorStateList(
                    states, intArrayOf((accent and 0xFFFFFF) or 0x66000000, col(R.color.chip))
                )
                setOnCheckedChangeListener { _, on -> toggle(a.id, on) }
            }
            row.addView(sw)

            val del = tv("✕", 18f, R.color.muted).apply {
                setPadding(dp(14), dp(10), dp(10), dp(10))
                setOnClickListener { confirmDelete(a) }
            }
            row.addView(del)

            listBox.addView(row, LinearLayout.LayoutParams(MATCH, WRAP).apply {
                bottomMargin = dp(12)
            })
        }
    }
}
