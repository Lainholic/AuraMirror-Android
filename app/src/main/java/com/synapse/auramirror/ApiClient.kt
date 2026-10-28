package com.synapse.auramirror

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.min

/**
 * Talks to the local AuraMirror proxy (youcam_mvp/server.py).
 * The YouCam secret key stays on the proxy; the APK never contains it.
 * Default URL works with `adb reverse tcp:8000 tcp:8000` on both emulator and device.
 */
class ApiClient(private val baseUrl: String = BuildConfig.PROXY_BASE_URL) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(85, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS) // aligned with server.py 75s deadline
        .retryOnConnectionFailure(false)
        .build()

    private val json = "application/json".toMediaType()

    class ApiException(message: String) : Exception(message)

    /** Rotate upright, center-crop square, scale to 600px: same framing the proxy/YouCam use. */
    suspend fun prepareFace(captured: CapturedImage): Bitmap = withContext(Dispatchers.Default) {
        if (captured.bytes.isEmpty() || captured.bytes.size > 12_000_000) {
            throw ApiException("Captured image size is out of safe bounds.")
        }
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(captured.bytes, 0, captured.bytes.size, opts)
        if (opts.outWidth <= 0 || opts.outHeight <= 0 || opts.outWidth > 8192 || opts.outHeight > 8192) {
            throw ApiException("Captured image dimensions are invalid.")
        }
        var sample = 1
        while (min(opts.outWidth, opts.outHeight) / (sample * 2) >= 600 ||
            max(opts.outWidth, opts.outHeight) / sample > 2048
        ) {
            sample *= 2
        }
        val decoded = BitmapFactory.decodeByteArray(
            captured.bytes, 0, captured.bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample }
        ) ?: throw ApiException("Captured image could not be decoded.")

        val upright = if (captured.rotationDegrees != 0 || !captured.isSample) {
            val m = Matrix().apply {
                if (captured.rotationDegrees != 0) {
                    postRotate(captured.rotationDegrees.toFloat())
                }
                if (!captured.isSample) {
                    // Mirror horizontally so the captured photo matches the front camera preview 1:1
                    postScale(-1f, 1f)
                }
            }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true)
                .also { if (it !== decoded) decoded.recycle() }
        } else decoded

        val rawSide = min(upright.width, upright.height)
        val side = if (captured.isSample) {
            rawSide
        } else {
            // Match the 1.35x center digital framing of the 1:1 square camera preview so face width > 65%
            (rawSide / 1.35f).toInt().coerceAtLeast(1)
        }
        val square = Bitmap.createBitmap(
            upright, (upright.width - side) / 2, (upright.height - side) / 2, side, side
        )
        val scaled = Bitmap.createScaledBitmap(square, 600, 600, true)
        if (square !== upright) upright.recycle()
        if (scaled !== square) square.recycle()
        scaled
    }

    suspend fun analyze(face: Bitmap): JSONObject = withContext(Dispatchers.IO) {
        val out = ByteArrayOutputStream()
        face.compress(Bitmap.CompressFormat.JPEG, 92, out)
        val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        val body = JSONObject().put("image_base64", "data:image/jpeg;base64,$b64")
        postCancellable("/api/analyze", body)
    }

    suspend fun prescribe(metrics: Map<String, Int>, sleepHours: Double?): JSONObject =
        withContext(Dispatchers.IO) {
            val m = JSONObject()
            metrics.forEach { (k, v) -> m.put(k, v) }
            val wearable = JSONObject()
            if (sleepHours != null) wearable.put("sleep_hours", sleepHours)
            postCancellable("/api/prescribe", JSONObject().put("skin_metrics", m).put("wearable_data", wearable))
        }

    suspend fun checkProduct(productName: String, ingredientsText: String, metrics: Map<String, Int>): JSONObject =
        withContext(Dispatchers.IO) {
            val m = JSONObject()
            metrics.forEach { (k, v) -> m.put(k, v) }
            val body = JSONObject()
                .put("product_name", productName)
                .put("ingredients_text", ingredientsText)
                .put("skin_metrics", m)
            postCancellable("/api/check-product", body)
        }

    private suspend fun postCancellable(path: String, body: JSONObject): JSONObject =
        suspendCancellableCoroutine { cont ->
            val req = Request.Builder()
                .url(baseUrl.trimEnd('/') + path)
                .post(body.toString().toRequestBody(json))
                .build()
            val call = http.newCall(req)
            cont.invokeOnCancellation { call.cancel() }

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!cont.isActive) return
                    cont.resumeWithException(
                        ApiException("Cannot reach AuraMirror proxy at $baseUrl (${e.javaClass.simpleName}).")
                    )
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use { resp ->
                        if (!cont.isActive) return
                        try {
                            val source = resp.body?.source()
                            if (source != null) {
                                source.request(15_000_001L)
                                if (source.buffer.size > 15_000_000L) {
                                    cont.resumeWithException(ApiException("Proxy response exceeds 15MB safety limit."))
                                    return
                                }
                            }
                            val text = resp.body?.string().orEmpty()
                            if (!resp.isSuccessful) {
                                val detail = runCatching { JSONObject(text).optString("detail") }.getOrNull()
                                cont.resumeWithException(
                                    ApiException("Proxy ${resp.code}: ${detail?.takeIf { it.isNotBlank() } ?: "request failed"}")
                                )
                                return
                            }
                            cont.resume(JSONObject(text))
                        } catch (e: Exception) {
                            if (cont.isActive) {
                                cont.resumeWithException(ApiException("Invalid response from proxy: ${e.message}"))
                            }
                        }
                    }
                }
            })
        }

    companion object {
        private val LABELS = linkedMapOf(
            "oiliness" to "Oiliness",
            "dark_circle_v2" to "Dark circles",
            "moisture" to "Moisture",
            "redness" to "Redness",
            "pore" to "Pores",
            "acne" to "Acne",
            "wrinkle" to "Wrinkles",
            "texture" to "Texture",
            "age_spot" to "Age spots"
        )

        fun parseMetrics(scoreInfo: JSONObject): List<SkinMetric> =
            LABELS.mapNotNull { (key, label) ->
                val o = scoreInfo.optJSONObject(key) ?: return@mapNotNull null
                if (!o.has("ui_score")) return@mapNotNull null
                val s = o.optDouble("ui_score", Double.NaN)
                if (s.isNaN() || s < 0 || s > 100) return@mapNotNull null
                SkinMetric(key, label, s.toInt(), o.optString("output_mask_name").ifBlank { null })
            }

        suspend fun decodeMasks(masks: JSONObject?): Map<String, Bitmap> =
            withContext(Dispatchers.Default) {
                if (masks == null) return@withContext emptyMap()
                val result = mutableMapOf<String, Bitmap>()
                masks.keys().asSequence().take(12).forEach { name ->
                    val uri = masks.optString(name)
                    val raw = uri.substringAfter(",", uri)
                    if (raw.isEmpty() || raw.length > 6_000_000) return@forEach
                    runCatching {
                        val bytes = Base64.decode(raw, Base64.DEFAULT)
                        if (bytes.size > 4_500_000) return@runCatching null
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                        if (bounds.outWidth <= 0 || bounds.outHeight <= 0 ||
                            bounds.outWidth > 4096 || bounds.outHeight > 4096
                        ) {
                            return@runCatching null
                        }
                        var sample = 1
                        while (min(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 600 ||
                            max(bounds.outWidth, bounds.outHeight) / sample > 1200
                        ) {
                            sample *= 2
                        }
                        BitmapFactory.decodeByteArray(
                            bytes, 0, bytes.size,
                            BitmapFactory.Options().apply { inSampleSize = sample }
                        )
                    }.getOrNull()?.let { result[name] = it }
                }
                result
            }

        fun parseGuide(o: JSONObject): WellnessGuide {
            val routine = o.optJSONObject("routine") ?: JSONObject()
            val recs = o.optJSONArray("recommendations")
            val list = buildList {
                if (recs != null) for (i in 0 until recs.length()) {
                    val r = recs.getJSONObject(i)
                    add(
                        IngredientSuggestion(
                            tag = r.optString("tag"),
                            productName = r.optString("product_name"),
                            brand = r.optString("brand"),
                            activeIngredients = r.optString("active_ingredients"),
                            purpose = r.optString("purpose"),
                            buyUrl = r.optString("buy_url"),
                            metricKey = r.optString("metric_key"),
                            score = if (r.has("score")) r.optInt("score") else null,
                            threshold = if (r.has("threshold")) r.optInt("threshold") else null
                        )
                    )
                }
            }
            return WellnessGuide(
                assessmentStatus = o.optString("assessment_status"),
                headline = o.optString("headline"),
                disclaimer = o.optString("disclaimer"),
                morning = routine.optString("morning"),
                evening = routine.optString("evening"),
                lifestyle = routine.optString("lifestyle"),
                suggestions = list
            )
        }

        fun parseCosmeticCheck(o: JSONObject): CosmeticCheckResult {
            val verdict = o.optString("verdict", "")
            val validVerdicts = setOf("GOOD_MATCH", "MIXED_CAUTION", "CAUTION", "CONFLICT", "NEUTRAL")
            if (verdict !in validVerdicts) {
                throw ApiException("Proxy returned an unrecognized compatibility verdict.")
            }
            val fitScore = o.optInt("fit_score", -1)
            if (fitScore !in 0..100) {
                throw ApiException("Proxy returned an invalid compatibility score.")
            }
            fun parseItems(arrName: String): List<CosmeticMatchItem> {
                val arr = o.optJSONArray(arrName) ?: return emptyList()
                return buildList {
                    for (i in 0 until arr.length()) {
                        val item = arr.optJSONObject(i) ?: continue
                        val ing = item.optString("ingredient")
                        if (ing.isNotBlank()) {
                            add(
                                CosmeticMatchItem(
                                    ingredient = ing,
                                    reason = item.optString("reason"),
                                    metric = item.optString("metric")
                                )
                            )
                        }
                    }
                }
            }
            return CosmeticCheckResult(
                productName = o.optString("product_name").ifBlank { "Custom INCI Formula" },
                verdict = verdict,
                fitScore = fitScore,
                summary = o.optString("summary"),
                conflicts = parseItems("conflicts"),
                cautions = parseItems("cautions"),
                benefits = parseItems("benefits")
            )
        }

        /**
         * Post-processes a decoded YouCam PNG mask bitmap to compute experimental fixed-grid
         * highlight density in the upper-center region vs lower-peripheral region.
         * Returns null when total highlighted samples are insufficient (< 15) to avoid fabricating 50/50 claims.
         */
        fun computeZoneBalance(
            metrics: List<SkinMetric>,
            masks: Map<String, Bitmap>,
            preferredMaskKey: String? = null
        ): ZoneBalanceInsight? {
            val maskable = metrics.filter { it.maskKey != null && masks.containsKey(it.maskKey) }
            val comparator = compareBy<SkinMetric>({ it.uiScore }, { it.uiScore - it.threshold })
            val targetMetric = if (preferredMaskKey != null) {
                maskable.firstOrNull { it.maskKey == preferredMaskKey }
            } else {
                maskable.filter { it.isBelowThreshold }.minWithOrNull(comparator)
                    ?: maskable.minWithOrNull(comparator)
            } ?: return null

            val bmp = masks[targetMetric.maskKey] ?: return null
            val w = bmp.width
            val h = bmp.height
            if (w < 32 || h < 32) return null

            var tZoneActive = 0
            var tZoneTotal = 0
            var uZoneActive = 0
            var uZoneTotal = 0

            val step = max(1, min(w, h) / 60)
            for (y in (h / 8) until (h * 7 / 8) step step) {
                val normY = y.toFloat() / h
                for (x in (w / 8) until (w * 7 / 8) step step) {
                    val normX = x.toFloat() / w
                    val px = bmp.getPixel(x, y)
                    val alpha = (px ushr 24) and 0xFF
                    val r = (px ushr 16) and 0xFF
                    val g = (px ushr 8) and 0xFF
                    val b = px and 0xFF
                    val maxCh = max(r, max(g, b))
                    val minCh = min(r, min(g, b))
                    val isHighlighted = alpha in 35..255 && (maxCh - minCh > 25 || (alpha < 245 && (r + g + b) > 90))

                    val inUpperCenterGrid = (normY in 0.18f..0.42f && normX in 0.28f..0.72f) ||
                        (normY in 0.42f..0.66f && normX in 0.40f..0.60f)
                    val inLowerCheekGrid = normY in 0.44f..0.82f && normX in 0.20f..0.80f && !inUpperCenterGrid

                    if (inUpperCenterGrid) {
                        tZoneTotal++
                        if (isHighlighted) tZoneActive++
                    } else if (inLowerCheekGrid) {
                        uZoneTotal++
                        if (isHighlighted) uZoneActive++
                    }
                }
            }

            val totalActive = tZoneActive + uZoneActive
            if (tZoneTotal == 0 || uZoneTotal == 0 || totalActive < 15) return null

            val tDensityFloat = (tZoneActive * 100.0) / tZoneTotal
            val uDensityFloat = (uZoneActive * 100.0) / uZoneTotal
            val densitySum = tDensityFloat + uDensityFloat
            if (densitySum <= 0.001) return null

            val tShare = Math.round((tDensityFloat * 100.0) / densitySum).toInt().coerceIn(0, 100)
            val uShare = 100 - tShare
            val tDensityPct = Math.round(tDensityFloat).toInt().coerceIn(0, 100)
            val uDensityPct = Math.round(uDensityFloat).toInt().coerceIn(0, 100)

            val dominant = when {
                tShare >= 58 -> "Upper-Center Grid Higher (${targetMetric.label})"
                uShare >= 58 -> "Lower-Cheek Grid Higher (${targetMetric.label})"
                else -> "Even Grid Distribution (${targetMetric.label})"
            }
            val summary =
                "Experimental fixed-grid mask statistic ($totalActive active samples): Upper-center density $tDensityPct% vs Lower-cheek density $uDensityPct% (relative density ratio $tShare : $uShare). Depends on face framing inside the guide."

            return ZoneBalanceInsight(
                metricKey = targetMetric.key,
                metricLabel = targetMetric.label,
                tZoneDensityPercent = tDensityPct,
                uZoneDensityPercent = uDensityPct,
                tZoneSharePercent = tShare,
                uZoneSharePercent = uShare,
                activeSampleCount = totalActive,
                dominantZone = dominant,
                summary = summary
            )
        }
    }
}
