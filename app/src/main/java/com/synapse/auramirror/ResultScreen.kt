package com.synapse.auramirror

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.launch

private data class AuraThemePalette(
    val isLight: Boolean,
    val screenBg: Brush,
    val cardBg: Color,
    val cardBorder: Color,
    val subCardBg: Color,
    val subCardBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accentSoftBg: Color,
    val good: Color,
    val warn: Color,
    val warnSoftBg: Color,
    val danger: Color,
    val dangerSoftBg: Color,
    val ringTrack: Color,
    val radarGrid: Color,
    val radarFill: Color
)

private val BrightClinicalPalette = AuraThemePalette(
    isLight = true,
    screenBg = Brush.verticalGradient(
        listOf(Color(0xFFF4FBF9), Color(0xFFF8FAFC), Color(0xFFFFF5F7))
    ),
    cardBg = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFDCE6F2),
    subCardBg = Color(0xFFF6F9FC),
    subCardBorder = Color(0xFFE2EBF5),
    textPrimary = Color(0xFF141E30),
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF64748B),
    accent = Color(0xFF10B981),
    accentSoftBg = Color(0xFFECFDF5),
    good = Color(0xFF059669),
    warn = Color(0xFFD97706),
    warnSoftBg = Color(0xFFFFFBEB),
    danger = Color(0xFFF43F5E),
    dangerSoftBg = Color(0xFFFFF1F2),
    ringTrack = Color(0xFFE2E8F0),
    radarGrid = Color(0xFFCBD5E1),
    radarFill = Color(0x4010B981)
)

private val LuminousAuroraPalette = AuraThemePalette(
    isLight = false,
    screenBg = Brush.verticalGradient(
        listOf(Color(0xFF1A1834), Color(0xFF1E1B3A), Color(0xFF14182E))
    ),
    cardBg = Color(0xFF252246),
    cardBorder = Color(0xFF4B4682),
    subCardBg = Color(0xFF1C1A38),
    subCardBorder = Color(0xFF3E3A70),
    textPrimary = Color(0xFFFAFAFF),
    textSecondary = Color(0xFFCDD2F0),
    textMuted = Color(0xFF9BA5D2),
    accent = Color(0xFF00E6D2),
    accentSoftBg = Color(0xFF1E3750),
    good = Color(0xFF34D399),
    warn = Color(0xFFFBBF24),
    warnSoftBg = Color(0xFF3B2F22),
    danger = Color(0xFFFF5F87),
    dangerSoftBg = Color(0xFF441C38),
    ringTrack = Color(0xFF3A3669),
    radarGrid = Color(0xFF565091),
    radarFill = Color(0x4D00E6D2)
)

@Composable
fun ResultScreen(
    result: ScanResult,
    api: ApiClient,
    routineStore: RoutineStore,
    lang: AppLang,
    onCycleLang: () -> Unit,
    sleepReloadTick: Int = 0,
    isRetryingGuide: Boolean = false,
    onRetryGuide: () -> Unit = {},
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLightTheme by remember { mutableStateOf(routineStore.getSavedLightTheme()) }
    val palette = if (isLightTheme) BrightClinicalPalette else LuminousAuroraPalette

    var activeMask by remember { mutableStateOf<String?>(null) }
    var showPassport by remember { mutableStateOf(false) }
    val today = remember(sleepReloadTick) { LocalDate.now() }
    var routineTick by remember { mutableIntStateOf(0) }
    val metricComparator = remember {
        compareBy<SkinMetric>({ it.uiScore }, { it.uiScore - it.threshold })
    }

    var productNameInput by remember { mutableStateOf("") }
    var ingredientsInput by remember { mutableStateOf("") }
    var isCheckingCosmetic by remember { mutableStateOf(false) }
    var cosmeticResult by remember { mutableStateOf<CosmeticCheckResult?>(null) }
    var cosmeticError by remember { mutableStateOf<String?>(null) }
    var showAllCosmeticMatches by remember { mutableStateOf(false) }

    val displayedZone = remember(activeMask, result) {
        if (activeMask != null) {
            ApiClient.computeZoneBalance(result.metrics, result.masks, activeMask)
        } else {
            result.zoneInsight
        }
    }

    val belowThresholdMetrics = remember(result.metrics) {
        result.metrics.filter { it.isBelowThreshold }.sortedWith(metricComparator)
    }
    val focus = remember(result.metrics, belowThresholdMetrics) {
        belowThresholdMetrics.firstOrNull() ?: result.metrics.sortedWith(metricComparator).firstOrNull()
    }

    val overallDisplayScore = remember(result) {
        result.overallScore ?: if (result.metrics.isNotEmpty()) {
            result.metrics.map { it.uiScore }.average().toInt()
        } else 0
    }

    val avgDelta = remember(result) {
        val deltas = result.metrics.mapNotNull { m ->
            result.previousScores[m.key]?.let { prev -> m.uiScore - prev }
        }
        if (deltas.isNotEmpty()) deltas.sum() / deltas.size else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.screenBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ================= TOP BAR =================
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AURA MIRROR",
                color = palette.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false
            )
            Spacer(Modifier.weight(1f))
            // 4-Language Cycle Button (EN / 日本語 / 中文 / 한국어)
            Surface(
                onClick = onCycleLang,
                shape = RoundedCornerShape(50),
                color = palette.accentSoftBg,
                border = BorderStroke(1.dp, palette.accent)
            ) {
                Text(
                    text = "🌐 ${lang.buttonLabel}",
                    color = palette.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
            // Theme Toggle Pill (Bright Pearl vs Aurora Glow)
            Surface(
                onClick = {
                    val next = !isLightTheme
                    isLightTheme = next
                    routineStore.setSavedLightTheme(next)
                },
                shape = RoundedCornerShape(50),
                color = palette.cardBg,
                border = BorderStroke(1.dp, palette.cardBorder)
            ) {
                Text(
                    text = if (isLightTheme) "☀️" else "✨",
                    color = palette.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
            Surface(
                onClick = onClose,
                shape = RoundedCornerShape(50),
                color = palette.cardBg,
                border = BorderStroke(1.dp, palette.cardBorder)
            ) {
                Text(
                    text = AppI18n.rescanBtn(lang),
                    color = palette.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        // ================= 1. HERO DASHBOARD CARD =================
        Card(
            colors = CardDefaults.cardColors(containerColor = palette.cardBg),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.5.dp, palette.accent.copy(alpha = 0.65f)),
            elevation = CardDefaults.cardElevation(defaultElevation = if (palette.isLight) 3.dp else 0.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Left: Circular Score Ring
                    Box(
                        modifier = Modifier.size(96.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularScoreRing(
                            score = overallDisplayScore,
                            trackColor = palette.ringTrack,
                            progressColor = palette.accent,
                            modifier = Modifier.fillMaxSize()
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$overallDisplayScore",
                                color = palette.textPrimary,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = AppI18n.skinScoreLabel(lang),
                                color = palette.textMuted,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Center: Interactive Face + Mask Thumbnail with Scanner Brackets
                    Box(
                        modifier = Modifier
                            .size(116.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black)
                            .border(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                    ) {
                        Image(
                            bitmap = result.faceBitmap.asImageBitmap(),
                            contentDescription = "Captured face",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        val maskBmp = activeMask?.let { result.masks[it] }
                        if (maskBmp != null) {
                            Image(
                                bitmap = maskBmp.asImageBitmap(),
                                contentDescription = "Skin AI mask",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                alpha = 0.85f
                            )
                        }
                        ScannerCornerBrackets(color = palette.accent)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color(0x99000000))
                                .padding(vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = AppI18n.maskBadge(activeMask != null, result.masks.size, lang),
                                color = Color(0xFF00E6D2),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Right: 3 Clinical KPI Pills
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        KpiStatPill(
                            label = AppI18n.kpiSkinAgeLabel(lang),
                            value = AppI18n.kpiSkinAgeValue(result.skinAge, lang),
                            valueColor = palette.warn,
                            bgColor = palette.warnSoftBg,
                            borderColor = palette.warn.copy(alpha = 0.45f),
                            labelColor = palette.textMuted
                        )
                        val sleepDeficit = result.sleepHours != null && result.sleepHours < 6.5
                        KpiStatPill(
                            label = AppI18n.kpiSleepLabel(lang),
                            value = AppI18n.kpiSleepValue(result.sleepHours, lang),
                            valueColor = if (sleepDeficit) palette.danger else palette.good,
                            bgColor = if (sleepDeficit) palette.dangerSoftBg else palette.accentSoftBg,
                            borderColor = (if (sleepDeficit) palette.danger else palette.good).copy(alpha = 0.45f),
                            labelColor = palette.textMuted
                        )
                        KpiStatPill(
                            label = AppI18n.kpiDeltaLabel(lang),
                            value = if (result.isSample) AppI18n.demoModeBadge(lang) else AppI18n.kpiDeltaValue(avgDelta, lang),
                            valueColor = palette.accent,
                            bgColor = palette.accentSoftBg,
                            borderColor = palette.accent.copy(alpha = 0.45f),
                            labelColor = palette.textMuted
                        )
                    }
                }

                // Mask Filter Chip Bar inside Hero Card
                val maskable = result.metrics.filter { it.maskKey != null && result.masks.containsKey(it.maskKey) }
                Spacer(Modifier.height(10.dp))
                if (maskable.isEmpty()) {
                    Text(AppI18n.noMaskLayers(lang), color = palette.textMuted, fontSize = 11.sp)
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MaskPillChip(
                            text = AppI18n.originalPhotoChip(lang),
                            selected = activeMask == null,
                            palette = palette,
                            onClick = { activeMask = null }
                        )
                        maskable.forEach { m ->
                            val mName = AppI18n.metricLabel(m.key, lang)
                            MaskPillChip(
                                text = "$mName (${m.uiScore})",
                                selected = activeMask == m.maskKey,
                                palette = palette,
                                onClick = { activeMask = if (activeMask == m.maskKey) null else m.maskKey }
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // ================= 2. RADAR CHART + TODAY'S CLINICAL FOCUS =================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left Half: Skin Balance Radar Chart
            Card(
                modifier = Modifier.weight(0.95f),
                colors = CardDefaults.cardColors(containerColor = palette.cardBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, palette.cardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = if (palette.isLight) 2.dp else 0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = AppI18n.radarTitle(lang),
                        color = palette.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(Modifier.height(6.dp))
                    SkinRadarChart(
                        metrics = result.metrics,
                        palette = palette,
                        modifier = Modifier.size(148.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = AppI18n.radarSubtitle(result.metrics.size, lang),
                        color = palette.textMuted,
                        fontSize = 10.sp
                    )
                }
            }

            // Right Half: Today's Clinical Focus + Spatial Ratio Bar
            Card(
                modifier = Modifier.weight(1.05f),
                colors = CardDefaults.cardColors(containerColor = palette.cardBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, palette.cardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = if (palette.isLight) 2.dp else 0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                ) {
                    val isHealthyBalance = belowThresholdMetrics.isEmpty()
                    Text(
                        text = AppI18n.focusHeader(isHealthyBalance, lang),
                        color = if (isHealthyBalance) palette.good else palette.warn,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    if (focus != null) {
                        val focusName = AppI18n.metricLabel(focus.key, lang)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isHealthyBalance) palette.accentSoftBg else palette.dangerSoftBg,
                                    RoundedCornerShape(10.dp)
                                )
                                .border(
                                    1.dp,
                                    (if (isHealthyBalance) palette.accent else palette.danger).copy(alpha = 0.4f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = "$focusName · ${focus.uiScore}/100",
                                    color = if (isHealthyBalance) palette.good else palette.danger,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = AppI18n.focusSubtext(isHealthyBalance, result.metrics.size, focus.threshold, lang),
                                    color = palette.textSecondary,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                        if (focus.maskKey != null && result.masks.containsKey(focus.maskKey)) {
                            Text(
                                text = AppI18n.showMaskLink(focusName, lang),
                                color = palette.accent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clickable { activeMask = focus.maskKey }
                            )
                        }
                    }

                    // Spatial Mask Ratio Mini Bar
                    displayedZone?.let { zone ->
                        val zoneMetricName = if (zone.metricKey.isNotBlank()) {
                            AppI18n.metricLabel(zone.metricKey, lang)
                        } else zone.metricLabel
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = AppI18n.spatialGridTitle(zoneMetricName, lang),
                                color = palette.textMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        val tShare = zone.tZoneSharePercent.coerceIn(0, 100)
                        val uShare = zone.uZoneSharePercent.coerceIn(0, 100)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(palette.ringTrack)
                        ) {
                            if (tShare > 0) {
                                Box(
                                    Modifier
                                        .fillMaxHeight()
                                        .weight(tShare.toFloat())
                                        .background(palette.danger)
                                )
                            }
                            if (uShare > 0) {
                                Box(
                                    Modifier
                                        .fillMaxHeight()
                                        .weight(uShare.toFloat())
                                        .background(palette.accent)
                                )
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = AppI18n.spatialGridRatioText(tShare, uShare, lang),
                            color = palette.textSecondary,
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // ================= 3. 9-VECTOR METRIC BENTO GRID =================
        Card(
            colors = CardDefaults.cardColors(containerColor = palette.cardBg),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, palette.cardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = if (palette.isLight) 2.dp else 0.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Text(
                    text = AppI18n.bentoTitle(result.metrics.size, lang),
                    color = palette.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                if (result.previousScores.isNotEmpty()) {
                    val prevTimeStr = result.previousScanMillis?.let {
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(it))
                    } ?: "last scan"
                    Text(
                        text = AppI18n.bentoDeltaHint(prevTimeStr, lang),
                        color = palette.textMuted,
                        fontSize = 10.sp
                    )
                }
                Spacer(Modifier.height(8.dp))

                val rows = result.metrics.chunked(2)
                rows.forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { m ->
                            val prev = result.previousScores[m.key]
                            val delta = if (prev != null) m.uiScore - prev else null
                            val barColor = if (m.isBelowThreshold) palette.danger else palette.accent
                            val canTapMask = m.maskKey != null && result.masks.containsKey(m.maskKey)
                            val localizedLabel = AppI18n.metricLabel(m.key, lang)

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(palette.subCardBg)
                                    .border(
                                        1.dp,
                                        if (activeMask != null && activeMask == m.maskKey) palette.accent else palette.subCardBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable(enabled = canTapMask) {
                                        activeMask = if (activeMask == m.maskKey) null else m.maskKey
                                    }
                                    .padding(9.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = localizedLabel,
                                        color = palette.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (delta != null) {
                                        val deltaText = when {
                                            delta > 0 -> "+$delta▲"
                                            delta < 0 -> "$delta▼"
                                            else -> "0─"
                                        }
                                        val deltaCol = when {
                                            delta > 0 -> palette.good
                                            delta < 0 -> palette.danger
                                            else -> palette.textMuted
                                        }
                                        Text(
                                            text = deltaText,
                                            color = deltaCol,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = "${m.uiScore}",
                                        color = barColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Spacer(Modifier.height(5.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(palette.ringTrack)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth((m.uiScore / 100f).coerceIn(0.04f, 1f))
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(barColor)
                                    )
                                }
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // ================= 4. INGREDIENT SAFETY GUARD (O/X SCANNER) =================
        Card(
            colors = CardDefaults.cardColors(containerColor = palette.cardBg),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, palette.cardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = if (palette.isLight) 2.dp else 0.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Text(
                    text = AppI18n.safetyGuardTitle(lang),
                    color = palette.accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = AppI18n.safetyGuardSub(lang),
                    color = palette.textMuted,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PresetPillButton(
                        label = AppI18n.presetCica(lang),
                        palette = palette,
                        enabled = !isCheckingCosmetic,
                        onClick = {
                            productNameInput = "Cica Barrier Soothing Serum"
                            ingredientsInput = "Water, Centella Asiatica Extract, Panthenol, Sodium Hyaluronate, Ceramide NP, Niacinamide"
                            cosmeticResult = null
                            cosmeticError = null
                            showAllCosmeticMatches = false
                        }
                    )
                    PresetPillButton(
                        label = AppI18n.presetAlcohol(lang),
                        palette = palette,
                        enabled = !isCheckingCosmetic,
                        onClick = {
                            productNameInput = "Astringent Alcohol Toner"
                            ingredientsInput = "Water, Alcohol Denat, Menthol, Fragrance, Glycolic Acid, Limonene"
                            cosmeticResult = null
                            cosmeticError = null
                            showAllCosmeticMatches = false
                        }
                    )
                    PresetPillButton(
                        label = AppI18n.presetOilBalm(lang),
                        palette = palette,
                        enabled = !isCheckingCosmetic,
                        onClick = {
                            productNameInput = "Rich Coconut Night Balm"
                            ingredientsInput = "Cocos Nucifera (Coconut) Oil, Shea Butter, Isopropyl Myristate, Tocopherol"
                            cosmeticResult = null
                            cosmeticError = null
                            showAllCosmeticMatches = false
                        }
                    )
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = productNameInput,
                    onValueChange = {
                        productNameInput = it
                        cosmeticResult = null
                        cosmeticError = null
                        showAllCosmeticMatches = false
                    },
                    enabled = !isCheckingCosmetic,
                    placeholder = { Text(AppI18n.productPlaceholder(lang), fontSize = 12.sp, color = palette.textMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(color = palette.textPrimary, fontSize = 12.sp),
                    singleLine = true
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = ingredientsInput,
                    onValueChange = {
                        ingredientsInput = it
                        cosmeticResult = null
                        cosmeticError = null
                        showAllCosmeticMatches = false
                    },
                    enabled = !isCheckingCosmetic,
                    placeholder = { Text(AppI18n.inciPlaceholder(lang), fontSize = 12.sp, color = palette.textMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(color = palette.textPrimary, fontSize = 12.sp),
                    maxLines = 3
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val reqName = productNameInput.trim()
                        val reqIng = ingredientsInput.trim()
                        if (isCheckingCosmetic || reqIng.isEmpty()) return@Button
                        isCheckingCosmetic = true
                        cosmeticResult = null
                        cosmeticError = null
                        showAllCosmeticMatches = false
                        scope.launch {
                            try {
                                val forRules = result.metrics.associate { m ->
                                    (if (m.key == "dark_circle_v2") "dark_circle" else m.key) to m.uiScore
                                }
                                val resp = api.checkProduct(reqName, reqIng, forRules)
                                val parsed = ApiClient.parseCosmeticCheck(resp)
                                if (ingredientsInput.trim() == reqIng && productNameInput.trim() == reqName) {
                                    cosmeticResult = parsed
                                }
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                cosmeticError = e.message
                            } finally {
                                isCheckingCosmetic = false
                            }
                        }
                    },
                    enabled = !isCheckingCosmetic && ingredientsInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent)
                ) {
                    Text(
                        text = AppI18n.analyzeCompatBtn(isCheckingCosmetic, lang),
                        color = if (palette.isLight) Color.White else Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                cosmeticError?.let { err ->
                    Spacer(Modifier.height(6.dp))
                    Text(err, color = palette.danger, fontSize = 11.sp)
                }

                cosmeticResult?.let { cr ->
                    Spacer(Modifier.height(10.dp))
                    val (verdictColor, verdictBg) = when (cr.verdict) {
                        "GOOD_MATCH" -> palette.good to palette.accentSoftBg
                        "MIXED_CAUTION" -> palette.warn to palette.warnSoftBg
                        "CONFLICT" -> palette.danger to palette.dangerSoftBg
                        "CAUTION" -> palette.warn to palette.warnSoftBg
                        else -> palette.textSecondary to palette.subCardBg
                    }
                    val verdictLabel = AppI18n.verdictBadgeText(cr.verdict, lang)
                    val allEntries = buildList {
                        cr.conflicts.forEach { add(Triple("AVOID", palette.danger, it)) }
                        cr.cautions.forEach { add(Triple("CAUTION", palette.warn, it)) }
                        cr.benefits.forEach { add(Triple("GOOD", palette.good, it)) }
                    }
                    val previewLimit = 2
                    val visibleEntries = if (showAllCosmeticMatches) allEntries else allEntries.take(previewLimit)
                    val hiddenCount = (allEntries.size - previewLimit).coerceAtLeast(0)

                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.subCardBg)
                            .border(1.dp, verdictColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = cr.productName,
                                color = palette.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .background(verdictBg, RoundedCornerShape(50))
                                    .border(1.dp, verdictColor, RoundedCornerShape(50))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = verdictLabel,
                                    color = verdictColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = AppI18n.cosmeticSummaryLocalized(cr, lang),
                            color = palette.textSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                        visibleEntries.forEach { (rawTag, tagColor, item) ->
                            Spacer(Modifier.height(5.dp))
                            val localizedTag = AppI18n.itemTagLocalized(rawTag, lang)
                            val localizedReason = AppI18n.localizeItemReason(item, rawTag, lang)
                            Text(
                                text = "[$localizedTag] ${item.ingredient} — $localizedReason",
                                color = tagColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 15.sp
                            )
                        }
                        if (hiddenCount > 0) {
                            Spacer(Modifier.height(4.dp))
                            TextButton(
                                onClick = { showAllCosmeticMatches = !showAllCosmeticMatches },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = AppI18n.expandCollapseItems(showAllCosmeticMatches, hiddenCount, lang),
                                    color = palette.accent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // ================= 5. SUGGESTED INGREDIENTS & ACTION ROUTINE =================
        val guide = result.guide
        Card(
            colors = CardDefaults.cardColors(containerColor = palette.cardBg),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, palette.cardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = if (palette.isLight) 2.dp else 0.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Text(
                    text = AppI18n.suggestedSectionTitle(lang),
                    color = palette.accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                when {
                    guide == null -> {
                        Text(
                            result.guideError ?: AppI18n.dataSourceLabel(DataSource.NOT_AVAILABLE, lang),
                            color = palette.textMuted, fontSize = 12.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(onClick = onRetryGuide, enabled = !isRetryingGuide) {
                            Text(AppI18n.retryBtn(isRetryingGuide, lang), color = palette.accent, fontSize = 12.sp)
                        }
                    }
                    else -> {
                        if (guide.suggestions.isEmpty()) {
                            Text(
                                AppI18n.allThresholdsMetMsg(lang),
                                color = palette.good, fontSize = 12.sp
                            )
                        } else {
                            if (guide.assessmentStatus == "optimal_balance") {
                                Text(
                                    AppI18n.allThresholdsMetMsg(lang),
                                    color = palette.good, fontSize = 11.sp
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                            guide.suggestions.forEach { s ->
                                val localizedTag = AppI18n.localizeSuggestionTag(s.metricKey, s.tag, lang)
                                val localizedActive = AppI18n.localizeSuggestionIngredients(s.metricKey, s.activeIngredients, lang)
                                val localizedProduct = AppI18n.localizeSuggestionProduct(s.metricKey, s.brand, s.productName, lang)
                                val localizedPurpose = AppI18n.localizeSuggestionPurpose(s.metricKey, s.purpose, lang)
                                val localizedSearchUrl = AppI18n.localizeSuggestionSearchUrl(s.metricKey, s.buyUrl, lang)
                                val reason = AppI18n.suggestionScoreReason(localizedTag, s.score, s.threshold, lang)
                                val exPrefix = AppI18n.examplePrefix(lang)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(palette.subCardBg, RoundedCornerShape(10.dp))
                                        .border(1.dp, palette.cardBorder, RoundedCornerShape(10.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = reason,
                                                color = palette.accent,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = localizedActive,
                                                color = palette.textPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "$exPrefix $localizedProduct",
                                                color = palette.textSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (localizedPurpose.isNotBlank()) {
                                                Text(
                                                    text = localizedPurpose,
                                                    color = palette.textMuted,
                                                    fontSize = 10.sp,
                                                    lineHeight = 14.sp
                                                )
                                            }
                                        }
                                        val safeUrl = localizedSearchUrl.trim()
                                        if (safeUrl.startsWith("https://") || safeUrl.startsWith("http://")) {
                                            Spacer(Modifier.width(8.dp))
                                            Surface(
                                                onClick = {
                                                    runCatching {
                                                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(safeUrl))
                                                        context.startActivity(intent)
                                                    }
                                                },
                                                shape = RoundedCornerShape(50),
                                                color = palette.accentSoftBg,
                                                border = BorderStroke(1.dp, palette.accent)
                                            ) {
                                                Text(
                                                    text = AppI18n.exploreProductBtn(lang),
                                                    color = palette.accent,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        val steps = listOf(
                            "AM" to AppI18n.localizeRoutineStep("AM", guide.assessmentStatus, guide.morning, lang),
                            "PM" to AppI18n.localizeRoutineStep("PM", guide.assessmentStatus, guide.evening, lang),
                            "Life" to AppI18n.localizeRoutineStep("Life", guide.assessmentStatus, guide.lifestyle, lang)
                        ).filter { it.second.isNotBlank() }
                        val sampleDoneMap = remember(result.analyzedAtMillis) { mutableStateMapOf<String, Boolean>() }
                        val doneCount = remember(routineTick, today, steps, result.isSample) {
                            steps.count { (tag, _) ->
                                if (result.isSample) sampleDoneMap[tag] == true else routineStore.isDone(tag, today)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = AppI18n.routineChecklistTitle(today.toString(), lang),
                                color = palette.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = AppI18n.routineDoneCounter(doneCount, steps.size, lang),
                                color = if (doneCount == steps.size && steps.isNotEmpty()) palette.good else palette.accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        steps.forEach { (tag, text) ->
                            RoutineRow(
                                tag = tag,
                                displayTag = AppI18n.routineTagDisplay(tag, lang),
                                text = text,
                                isSample = result.isSample,
                                sampleDone = sampleDoneMap[tag] == true,
                                onSampleToggle = { next ->
                                    sampleDoneMap[tag] = next
                                    routineTick++
                                },
                                today = today,
                                store = routineStore,
                                palette = palette,
                                onToggled = { routineTick++ }
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // ================= 6. RESULT PASSPORT & EXPORT =================
        Card(
            colors = CardDefaults.cardColors(containerColor = palette.cardBg),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, palette.cardBorder),
            modifier = Modifier.clickable { showPassport = !showPassport }
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Text(
                    text = AppI18n.passportTitle(showPassport, lang),
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (showPassport) {
                    Spacer(Modifier.height(6.dp))
                    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                    PassportLine(AppI18n.passportSkinScores(lang), DataSource.YOUCAM, palette.textPrimary, lang)
                    PassportLine(AppI18n.passportSleep(lang), result.sleepSource, palette.textPrimary, lang)
                    PassportLine(
                        AppI18n.passportIngredients(lang),
                        if (guide != null) DataSource.RULE_TABLE else DataSource.NOT_AVAILABLE,
                        palette.textPrimary,
                        lang
                    )
                    Text(AppI18n.passportMetaLine(fmt.format(Date(result.analyzedAtMillis)), result.masks.size, lang), color = palette.textMuted, fontSize = 11.sp)
                    Text(
                        AppI18n.passportPrivacy(lang),
                        color = palette.textMuted, fontSize = 11.sp, lineHeight = 15.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        DisclaimerBanner(palette, lang)
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val reportText = buildFullMarkdownReport(result, displayedZone, cosmeticResult, lang)
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "AuraMirror Skin Wellness Report")
                        putExtra(Intent.EXTRA_TEXT, reportText)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, AppI18n.exportShareBtn(lang)))
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, palette.accent)
            ) {
                Text(AppI18n.exportShareBtn(lang), color = palette.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = {
                    val reportText = buildFullMarkdownReport(result, displayedZone, cosmeticResult, lang)
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    if (clipboard != null) {
                        val clip = ClipData.newPlainText("AuraMirror Report", reportText)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            clip.description.extras = PersistableBundle().apply {
                                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                            }
                        }
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, AppI18n.copiedToast(lang), Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, palette.cardBorder)
            ) {
                Text(AppI18n.copyMarkdownBtn(lang), color = palette.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = palette.accent)
        ) {
            Text(
                text = AppI18n.closeRescanBtn(lang),
                color = if (palette.isLight) Color.White else Color(0xFF0F172A),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CircularScoreRing(
    score: Int,
    trackColor: Color,
    progressColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.padding(6.dp)) {
        val strokeWidth = 9.dp.toPx()
        val diameter = min(size.width, size.height) - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)

        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        val sweep = (score.coerceIn(0, 100) / 100f) * 360f
        drawArc(
            color = progressColor,
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun BoxScope.ScannerCornerBrackets(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
        val len = 14.dp.toPx()
        val stroke = 2.dp.toPx()
        val w = size.width
        val h = size.height
        drawLine(color, Offset(0f, 0f), Offset(len, 0f), stroke)
        drawLine(color, Offset(0f, 0f), Offset(0f, len), stroke)
        drawLine(color, Offset(w, 0f), Offset(w - len, 0f), stroke)
        drawLine(color, Offset(w, 0f), Offset(w, len), stroke)
        drawLine(color, Offset(0f, h), Offset(len, h), stroke)
        drawLine(color, Offset(0f, h), Offset(0f, h - len), stroke)
        drawLine(color, Offset(w, h), Offset(w - len, h), stroke)
        drawLine(color, Offset(w, h), Offset(w, h - len), stroke)
    }
}

@Composable
private fun KpiStatPill(
    label: String,
    value: String,
    valueColor: Color,
    bgColor: Color,
    borderColor: Color,
    labelColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = label, color = labelColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(text = value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun MaskPillChip(
    text: String,
    selected: Boolean,
    palette: AuraThemePalette,
    onClick: () -> Unit
) {
    val bg = if (selected) palette.accent else palette.subCardBg
    val fg = if (selected) {
        if (palette.isLight) Color.White else Color(0xFF0F172A)
    } else palette.textSecondary
    val border = if (selected) palette.accent else palette.subCardBorder

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 5.dp)
    ) {
        Text(text = text, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PresetPillButton(
    label: String,
    palette: AuraThemePalette,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(palette.accentSoftBg)
            .border(1.dp, palette.accent.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text = label, color = palette.accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SkinRadarChart(
    metrics: List<SkinMetric>,
    palette: AuraThemePalette,
    modifier: Modifier = Modifier
) {
    val radarItems = remember(metrics) { metrics.take(6) }
    if (radarItems.size < 3) return
    Canvas(modifier = modifier) {
        val count = radarItems.size
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = min(cx, cy) * 0.78f

        listOf(0.35f, 0.68f, 1.0f).forEach { scale ->
            val ringPath = Path()
            for (i in 0 until count) {
                val angle = Math.toRadians((-90.0 + i * (360.0 / count)))
                val px = cx + (radius * scale * cos(angle)).toFloat()
                val py = cy + (radius * scale * sin(angle)).toFloat()
                if (i == 0) ringPath.moveTo(px, py) else ringPath.lineTo(px, py)
            }
            ringPath.close()
            drawPath(ringPath, color = palette.radarGrid, style = Stroke(width = 1.dp.toPx()))
        }

        for (i in 0 until count) {
            val angle = Math.toRadians((-90.0 + i * (360.0 / count)))
            val ex = cx + (radius * cos(angle)).toFloat()
            val ey = cy + (radius * sin(angle)).toFloat()
            drawLine(palette.radarGrid, Offset(cx, cy), Offset(ex, ey), strokeWidth = 1.dp.toPx())
        }

        val dataPath = Path()
        val points = mutableListOf<Offset>()
        for (i in 0 until count) {
            val v = (radarItems[i].uiScore.coerceIn(10, 100) / 100f)
            val angle = Math.toRadians((-90.0 + i * (360.0 / count)))
            val px = cx + (radius * v * cos(angle)).toFloat()
            val py = cy + (radius * v * sin(angle)).toFloat()
            points.add(Offset(px, py))
            if (i == 0) dataPath.moveTo(px, py) else dataPath.lineTo(px, py)
        }
        dataPath.close()
        drawPath(dataPath, color = palette.radarFill)
        drawPath(dataPath, color = palette.accent, style = Stroke(width = 2.dp.toPx()))

        points.forEachIndexed { idx, pt ->
            val dotColor = if (radarItems[idx].isBelowThreshold) palette.danger else palette.accent
            drawCircle(color = dotColor, radius = 3.5.dp.toPx(), center = pt)
        }
    }
}

private fun buildFullMarkdownReport(
    result: ScanResult,
    zone: ZoneBalanceInsight?,
    cosmetic: CosmeticCheckResult?,
    lang: AppLang
): String = buildString {
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    appendLine("# AuraMirror Skin Wellness Report (${lang.buttonLabel})")
    appendLine("- Analyzed: ${fmt.format(Date(result.analyzedAtMillis))}")
    result.overallScore?.let { appendLine("- ${AppI18n.skinScoreLabel(lang)}: $it/100") }
    result.skinAge?.let { appendLine("- ${AppI18n.kpiSkinAgeLabel(lang)}: ${AppI18n.kpiSkinAgeValue(it, lang)}") }
    appendLine("- ${AppI18n.kpiSleepLabel(lang)}: ${AppI18n.kpiSleepValue(result.sleepHours, lang)}")
    appendLine()
    appendLine("## 1. ${AppI18n.bentoTitle(result.metrics.size, lang)}")
    result.metrics.forEach { m ->
        val prev = result.previousScores[m.key]
        val deltaStr = if (prev != null) {
            val d = m.uiScore - prev
            if (d > 0) " (+$d)" else if (d < 0) " ($d)" else " (0)"
        } else ""
        appendLine("- ${AppI18n.metricLabel(m.key, lang)}: ${m.uiScore}/100 (≥${m.threshold})$deltaStr")
    }
    if (zone != null) {
        val zLabel = if (zone.metricKey.isNotBlank()) AppI18n.metricLabel(zone.metricKey, lang) else zone.metricLabel
        appendLine()
        appendLine("## 2. ${AppI18n.spatialGridTitle(zLabel, lang)}")
        appendLine("- ${AppI18n.spatialGridRatioText(zone.tZoneSharePercent, zone.uZoneSharePercent, lang)}")
    }
    result.guide?.let { g ->
        appendLine()
        appendLine("## 3. ${AppI18n.suggestedSectionTitle(lang)}")
        if (g.suggestions.isEmpty()) {
            appendLine("- ${AppI18n.allThresholdsMetMsg(lang)}")
        } else {
            g.suggestions.forEach { s ->
                val tagLoc = AppI18n.localizeSuggestionTag(s.metricKey, s.tag, lang)
                val activeLoc = AppI18n.localizeSuggestionIngredients(s.metricKey, s.activeIngredients, lang)
                val prodLoc = AppI18n.localizeSuggestionProduct(s.metricKey, s.brand, s.productName, lang)
                val urlLoc = AppI18n.localizeSuggestionSearchUrl(s.metricKey, s.buyUrl, lang)
                val linkSuffix = if (urlLoc.isNotBlank()) " — $urlLoc" else ""
                appendLine("- $activeLoc: $tagLoc (${AppI18n.examplePrefix(lang)} $prodLoc)$linkSuffix")
            }
        }
        appendLine("- AM: ${AppI18n.localizeRoutineStep("AM", g.assessmentStatus, g.morning, lang)}")
        appendLine("- PM: ${AppI18n.localizeRoutineStep("PM", g.assessmentStatus, g.evening, lang)}")
        appendLine("- Life: ${AppI18n.localizeRoutineStep("Life", g.assessmentStatus, g.lifestyle, lang)}")
    }
    if (cosmetic != null) {
        appendLine()
        appendLine("## 4. ${AppI18n.safetyGuardTitle(lang)}: ${cosmetic.productName}")
        appendLine("- Verdict: ${AppI18n.verdictBadgeText(cosmetic.verdict, lang)}")
        appendLine("- Summary: ${AppI18n.cosmeticSummaryLocalized(cosmetic, lang)}")
        cosmetic.conflicts.forEach {
            appendLine("  * [${AppI18n.itemTagLocalized("AVOID", lang)}] ${it.ingredient}: ${AppI18n.localizeItemReason(it, "AVOID", lang)}")
        }
        cosmetic.cautions.forEach {
            appendLine("  * [${AppI18n.itemTagLocalized("CAUTION", lang)}] ${it.ingredient}: ${AppI18n.localizeItemReason(it, "CAUTION", lang)}")
        }
        cosmetic.benefits.forEach {
            appendLine("  * [${AppI18n.itemTagLocalized("GOOD", lang)}] ${it.ingredient}: ${AppI18n.localizeItemReason(it, "GOOD", lang)}")
        }
    }
    appendLine()
    appendLine("---")
    appendLine(AppI18n.disclaimerText(lang))
}

@Composable
private fun RoutineRow(
    tag: String,
    displayTag: String,
    text: String,
    isSample: Boolean,
    sampleDone: Boolean,
    onSampleToggle: (Boolean) -> Unit,
    today: LocalDate,
    store: RoutineStore,
    palette: AuraThemePalette,
    onToggled: () -> Unit
) {
    var persistedDone by remember(tag, today) { mutableStateOf(store.isDone(tag, today)) }
    val done = if (isSample) sampleDone else persistedDone
    val toggle = {
        val next = !done
        if (isSample) {
            onSampleToggle(next)
        } else {
            persistedDone = next
            store.setDone(tag, next, today)
            onToggled()
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { toggle() }
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = done,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = palette.accent,
                uncheckedColor = palette.textMuted
            )
        )
        Spacer(Modifier.width(8.dp))
        Text("$displayTag  ", color = palette.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(text, color = palette.textPrimary, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun PassportLine(what: String, source: DataSource, textColor: Color, lang: AppLang) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(what, color = textColor, fontSize = 12.sp, modifier = Modifier.weight(1f))
        SourceBadge(source, lang)
    }
}

@Composable
fun SourceBadge(source: DataSource, lang: AppLang = AppLang.EN) {
    val color = when (source) {
        DataSource.YOUCAM -> Color(0xFF7E57C2)
        DataSource.HEALTH_CONNECT -> Color(0xFF10B981)
        DataSource.RULE_TABLE -> Color(0xFF5C6BC0)
        DataSource.MASK_GRID -> Color(0xFF0284C7)
        DataSource.LOCAL_DELTA -> Color(0xFF0D9488)
        DataSource.NOT_AVAILABLE -> Color(0xFF64748B)
    }
    Text(
        AppI18n.dataSourceLabel(source, lang),
        color = Color.White,
        fontSize = 9.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(color, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun DisclaimerBanner(palette: AuraThemePalette, lang: AppLang) {
    Text(
        AppI18n.disclaimerText(lang),
        color = palette.textMuted, fontSize = 11.sp, lineHeight = 15.sp,
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.subCardBg, RoundedCornerShape(10.dp))
            .border(1.dp, palette.subCardBorder, RoundedCornerShape(10.dp))
            .padding(10.dp)
    )
}
