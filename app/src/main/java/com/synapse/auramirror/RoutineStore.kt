package com.synapse.auramirror

import android.content.Context
import org.json.JSONObject
import java.time.LocalDate

/** Today's wellness routine check state & previous scan delta history, stored locally on device. */
class RoutineStore(context: Context) {
    private val prefs = context.getSharedPreferences("aura_routine", Context.MODE_PRIVATE)

    private fun key(item: String, date: LocalDate = LocalDate.now()) = "$date|$item"

    fun isDone(item: String, date: LocalDate = LocalDate.now()): Boolean =
        prefs.getBoolean(key(item, date), false)

    fun setDone(item: String, done: Boolean, date: LocalDate = LocalDate.now()) {
        prefs.edit().putBoolean(key(item, date), done).apply()
    }

    fun readLastScanScores(): Pair<Map<String, Int>, Long?> {
        val raw = prefs.getString("last_scan_scores_json", null) ?: return emptyMap<String, Int>() to null
        val ts = prefs.getLong("last_scan_timestamp", 0L).takeIf { it > 0L }
        val map = mutableMapOf<String, Int>()
        runCatching {
            val obj = JSONObject(raw)
            obj.keys().forEach { k ->
                val v = obj.optInt(k, -1)
                if (v in 0..100) map[k] = v
            }
        }
        return map to ts
    }

    fun saveScanScores(metrics: List<SkinMetric>, timestampMillis: Long) {
        val obj = JSONObject()
        metrics.forEach { m -> obj.put(m.key, m.uiScore) }
        prefs.edit()
            .putString("last_scan_scores_json", obj.toString())
            .putLong("last_scan_timestamp", timestampMillis)
            .apply()
    }

    fun getSavedLang(): AppLang {
        val code = prefs.getString("saved_app_lang", AppLang.EN.code) ?: AppLang.EN.code
        return AppLang.values().firstOrNull { it.code == code } ?: AppLang.EN
    }

    fun setSavedLang(lang: AppLang) {
        prefs.edit().putString("saved_app_lang", lang.code).apply()
    }

    fun getSavedLightTheme(): Boolean = prefs.getBoolean("saved_light_theme", true)

    fun setSavedLightTheme(isLight: Boolean) {
        prefs.edit().putBoolean("saved_light_theme", isLight).apply()
    }
}
