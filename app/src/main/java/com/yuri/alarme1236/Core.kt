package com.yuri.alarme1236

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject

data class Alarm(
    val id: Int,
    val label: String,
    val intervalHours: Int,
    var nextTrigger: Long,
    var enabled: Boolean
)

object AlarmStore {
    private const val PREFS = "alarmes"
    private const val KEY = "lista"

    fun load(ctx: Context): MutableList<Alarm> {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val out = mutableListOf<Alarm>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                Alarm(
                    o.getInt("id"),
                    o.getString("label"),
                    o.getInt("h"),
                    o.getLong("next"),
                    o.getBoolean("on")
                )
            )
        }
        return out
    }

    fun save(ctx: Context, list: List<Alarm>) {
        val arr = JSONArray()
        for (a in list) {
            arr.put(
                JSONObject()
                    .put("id", a.id)
                    .put("label", a.label)
                    .put("h", a.intervalHours)
                    .put("next", a.nextTrigger)
                    .put("on", a.enabled)
            )
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }

    fun nextId(list: List<Alarm>): Int = (list.maxOfOrNull { it.id } ?: 0) + 1
}

object Scheduler {
    private const val FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    private fun firePending(ctx: Context, id: Int): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, id,
            Intent(ctx, AlarmReceiver::class.java).putExtra("id", id),
            FLAGS
        )

    fun schedule(ctx: Context, a: Alarm) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val show = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java), FLAGS
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(a.nextTrigger, show), firePending(ctx, a.id))
    }

    fun cancel(ctx: Context, id: Int) {
        ctx.getSystemService(AlarmManager::class.java).cancel(firePending(ctx, id))
    }

    /** Avança o próximo disparo, de intervalo em intervalo, até ficar no futuro. */
    fun advancePast(a: Alarm, now: Long) {
        val step = a.intervalHours * 3_600_000L
        while (a.nextTrigger <= now) a.nextTrigger += step
    }
}
