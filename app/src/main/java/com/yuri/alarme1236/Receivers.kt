package com.yuri.alarme1236

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getIntExtra("id", -1)
        val list = AlarmStore.load(ctx)
        val a = list.find { it.id == id } ?: return
        if (!a.enabled) return

        // Agenda o próximo disparo ANTES de tocar, para nunca perder o ritmo.
        Scheduler.advancePast(a, System.currentTimeMillis())
        AlarmStore.save(ctx, list)
        Scheduler.schedule(ctx, a)

        ctx.startForegroundService(
            Intent(ctx, RingService::class.java).putExtra("label", a.label)
        )
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val list = AlarmStore.load(ctx)
        val now = System.currentTimeMillis()
        for (a in list.filter { it.enabled }) {
            Scheduler.advancePast(a, now)
            Scheduler.schedule(ctx, a)
        }
        AlarmStore.save(ctx, list)
    }
}
