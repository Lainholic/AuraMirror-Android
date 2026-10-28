package com.synapse.auramirror

import android.graphics.Bitmap

sealed interface SleepReadResult {
    data class Success(val sleepData: SleepData) : SleepReadResult
    data object Loading : SleepReadResult
    data object PermissionRequired : SleepReadResult
    data object NoData : SleepReadResult
    data object Unavailable : SleepReadResult
    data class Error(val message: String) : SleepReadResult
}

data class SleepData(
    val durationHours: Double,
    val startTimeIso: String = "",
    val endTimeIso: String = "",
    val isDeficit: Boolean = durationHours < 6.5
)

data class CapturedImage(
    val bytes: ByteArray,
    val rotationDegrees: Int = 0,
    val isSample: Boolean = false
)

/** Where a displayed value came from (Result Passport). */
enum class DataSource(val label: String) {
    YOUCAM("YouCam Skin AI"),
    HEALTH_CONNECT("Health Connect"),
    RULE_TABLE("Ingredient rule table"),
    MASK_GRID("Mask grid stat"),
    LOCAL_DELTA("Local scan history"),
    NOT_AVAILABLE("No data")
}

data class ZoneBalanceInsight(
    val metricKey: String = "",
    val metricLabel: String,
    val tZoneDensityPercent: Int,
    val uZoneDensityPercent: Int,
    val tZoneSharePercent: Int,
    val uZoneSharePercent: Int,
    val activeSampleCount: Int,
    val dominantZone: String,
    val summary: String
)

data class CosmeticMatchItem(
    val ingredient: String,
    val reason: String,
    val metric: String
)

data class CosmeticCheckResult(
    val productName: String,
    val verdict: String, // GOOD_MATCH, CAUTION, CONFLICT, NEUTRAL
    val fitScore: Int,
    val summary: String,
    val conflicts: List<CosmeticMatchItem>,
    val cautions: List<CosmeticMatchItem>,
    val benefits: List<CosmeticMatchItem>
)

/** One YouCam metric. Higher ui_score = better condition (YouCam convention). */
data class SkinMetric(
    val key: String,
    val label: String,
    val uiScore: Int,
    val maskKey: String?,
    val threshold: Int = if (key == "dark_circle_v2" || key == "redness") 80 else 75,
    val hasIngredientRule: Boolean = key in setOf("oiliness", "dark_circle_v2", "moisture", "redness", "pore")
) {
    val isBelowThreshold: Boolean get() = uiScore < threshold
}

data class IngredientSuggestion(
    val tag: String,
    val productName: String,
    val brand: String,
    val activeIngredients: String,
    val purpose: String,
    val buyUrl: String = "",
    val metricKey: String = "",
    val score: Int? = null,
    val threshold: Int? = null
)

data class WellnessGuide(
    val assessmentStatus: String,
    val headline: String,
    val disclaimer: String,
    val morning: String,
    val evening: String,
    val lifestyle: String,
    val suggestions: List<IngredientSuggestion>
)

data class ScanResult(
    val faceBitmap: Bitmap,
    val metrics: List<SkinMetric>,
    val isSample: Boolean = false,
    val previousScores: Map<String, Int> = emptyMap(),
    val previousScanMillis: Long? = null,
    val zoneInsight: ZoneBalanceInsight? = null,
    val overallScore: Int?,
    val skinAge: Int?,
    val masks: Map<String, Bitmap>,
    val guide: WellnessGuide?,
    val guideError: String? = null,
    val sleepSource: DataSource,
    val sleepHours: Double?,
    val sleepStatusText: String = "",
    val analyzedAtMillis: Long
)
