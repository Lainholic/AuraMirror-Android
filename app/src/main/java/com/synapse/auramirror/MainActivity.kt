package com.synapse.auramirror

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.health.connect.client.PermissionController
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var routineStore: RoutineStore
    private var hasCameraPermission by mutableStateOf(false)
    private var isDemoPreviewMode by mutableStateOf(false)
    private var sleepReloadTick by mutableIntStateOf(0)

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(this, "Camera permission is required for skin analysis.", Toast.LENGTH_SHORT).show()
        }
    }

    private val healthPermissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { _ ->
        sleepReloadTick++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        healthConnectManager = HealthConnectManager(this)
        routineStore = RoutineStore(this)
        isDemoPreviewMode = intent?.getBooleanExtra("demo_preview", false) == true

        hasCameraPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasCameraPermission && !isDemoPreviewMode) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF8FAFC)) {
                    AuraMirrorMainScreen(
                        healthConnectManager = healthConnectManager,
                        routineStore = routineStore,
                        hasCameraPermission = hasCameraPermission,
                        isDemoPreviewMode = isDemoPreviewMode,
                        sleepReloadTick = sleepReloadTick,
                        onRequestHealthPermissions = {
                            healthPermissionLauncher.launch(healthConnectManager.permissions)
                        },
                        onRequestCameraPermission = {
                            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        onOpenAppSettings = {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", packageName, null)
                            )
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.hasExtra("demo_preview")) {
            isDemoPreviewMode = intent.getBooleanExtra("demo_preview", false)
        }
    }

    override fun onResume() {
        super.onResume()
        hasCameraPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        sleepReloadTick++
    }
}

@Composable
fun AuraMirrorMainScreen(
    healthConnectManager: HealthConnectManager,
    routineStore: RoutineStore,
    hasCameraPermission: Boolean,
    isDemoPreviewMode: Boolean = false,
    sleepReloadTick: Int,
    onRequestHealthPermissions: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { ApiClient() }
    var currentLang by remember { mutableStateOf(routineStore.getSavedLang()) }
    var sleepResult by remember { mutableStateOf<SleepReadResult>(SleepReadResult.Loading) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var isRetryingGuide by remember { mutableStateOf(false) }
    var retryJob by remember { mutableStateOf<Job?>(null) }
    var stageStep by remember { mutableIntStateOf(0) }
    var capturedPreview by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var scanResult by remember { mutableStateOf<ScanResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val startAnalysis: (CapturedImage) -> Unit = { captured ->
        if (!isAnalyzing) {
            isAnalyzing = true
            errorMessage = null
            scope.launch {
                try {
                    stageStep = 1
                    val face = api.prepareFace(captured)
                    capturedPreview = face

                    stageStep = 2
                    val analysis = api.analyze(face)
                    val scoreInfo = analysis.optJSONObject("score_info")
                        ?: throw ApiClient.ApiException("Analysis response missing score_info.")
                    val metrics = ApiClient.parseMetrics(scoreInfo)
                    if (metrics.isEmpty()) throw ApiClient.ApiException("No valid skin scores returned.")
                    val masks = ApiClient.decodeMasks(analysis.optJSONObject("masks"))

                    val currentSleep = sleepResult
                    val sleep = (currentSleep as? SleepReadResult.Success)?.sleepData
                    val sleepStatus = when (currentSleep) {
                        is SleepReadResult.Success ->
                            "Recent sleep session duration: ${currentSleep.sleepData.durationHours}h (Health Connect)."
                        is SleepReadResult.PermissionRequired ->
                            "Sleep context: Health Connect permission not granted."
                        is SleepReadResult.NoData ->
                            "Sleep context: no sleep session recorded in the past 24h."
                        is SleepReadResult.Unavailable ->
                            "Sleep context: Health Connect not available on this device."
                        is SleepReadResult.Loading ->
                            "Sleep context: still reading Health Connect..."
                        is SleepReadResult.Error ->
                            "Sleep context unavailable (${currentSleep.message})."
                    }

                    stageStep = 3
                    var guideErr: String? = null
                    val guide = try {
                        val forRules = metrics.associate { m ->
                            (if (m.key == "dark_circle_v2") "dark_circle" else m.key) to m.uiScore
                        }
                        ApiClient.parseGuide(api.prescribe(forRules, sleep?.durationHours))
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        guideErr = e.message ?: "Guidance request failed"
                        null
                    }

                    val rawOverall = scoreInfo.optJSONObject("all")?.optDouble("score")
                    val validOverall = rawOverall?.takeIf { !it.isNaN() && it in 0.0..100.0 }?.toInt()
                    val nowMillis = System.currentTimeMillis()
                    val (prevScores, prevMillis) = if (captured.isSample) {
                        emptyMap<String, Int>() to null
                    } else {
                        routineStore.readLastScanScores()
                    }
                    val zoneInsight = ApiClient.computeZoneBalance(metrics, masks)
                    if (!captured.isSample) {
                        routineStore.saveScanScores(metrics, nowMillis)
                    }

                    scanResult = ScanResult(
                        faceBitmap = face,
                        metrics = metrics,
                        isSample = captured.isSample,
                        previousScores = prevScores,
                        previousScanMillis = prevMillis,
                        zoneInsight = zoneInsight,
                        overallScore = validOverall,
                        skinAge = scoreInfo.optInt("skin_age", -1).takeIf { it in 1..120 },
                        masks = masks,
                        guide = guide,
                        guideError = guideErr,
                        sleepSource = if (sleep != null) DataSource.HEALTH_CONNECT else DataSource.NOT_AVAILABLE,
                        sleepHours = sleep?.durationHours,
                        sleepStatusText = sleepStatus,
                        analyzedAtMillis = nowMillis
                    )
                } catch (e: ApiClient.ApiException) {
                    errorMessage = e.message
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    errorMessage = "Analysis failed: ${e.message}"
                } finally {
                    isAnalyzing = false
                    stageStep = 0
                    capturedPreview = null
                }
            }
        }
    }

    LaunchedEffect(sleepReloadTick) {
        sleepResult = healthConnectManager.readLastNightSleep()
    }

    scanResult?.let { result ->
        ResultScreen(
            result = result,
            api = api,
            routineStore = routineStore,
            lang = currentLang,
            onCycleLang = {
                val next = currentLang.next()
                currentLang = next
                routineStore.setSavedLang(next)
            },
            sleepReloadTick = sleepReloadTick,
            isRetryingGuide = isRetryingGuide,
            onRetryGuide = {
                if (isRetryingGuide) return@ResultScreen
                val targetScanMillis = result.analyzedAtMillis
                isRetryingGuide = true
                retryJob?.cancel()
                retryJob = scope.launch {
                    try {
                        val forRules = result.metrics.associate { m ->
                            (if (m.key == "dark_circle_v2") "dark_circle" else m.key) to m.uiScore
                        }
                        val newGuide = ApiClient.parseGuide(api.prescribe(forRules, result.sleepHours))
                        if (scanResult?.analyzedAtMillis == targetScanMillis) {
                            scanResult = scanResult?.copy(guide = newGuide, guideError = null)
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        if (scanResult?.analyzedAtMillis == targetScanMillis) {
                            scanResult = scanResult?.copy(guideError = e.message ?: "Guidance request failed")
                        }
                    } finally {
                        isRetryingGuide = false
                    }
                }
            },
            onClose = {
                retryJob?.cancel()
                isRetryingGuide = false
                scanResult = null
            }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission || isDemoPreviewMode) {
            CameraCaptureScreen(
                isAnalyzing = isAnalyzing,
                capturedPreview = capturedPreview,
                isDemoPreviewMode = isDemoPreviewMode || !hasCameraPermission,
                lang = currentLang,
                onImageCaptured = startAnalysis,
                onError = { err ->
                    if (!isAnalyzing) {
                        errorMessage = err
                    }
                }
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC)).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = AppI18n.cameraPermRequired(currentLang),
                    color = Color(0xFF1E293B),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onRequestCameraPermission,
                        enabled = !isAnalyzing,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text(AppI18n.grantCameraBtn(currentLang), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = onOpenAppSettings, enabled = !isAnalyzing) {
                        Text(AppI18n.appSettingsBtn(currentLang), color = Color(0xFF334155))
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        if (isAnalyzing) return@OutlinedButton
                        try {
                            val sampleBytes = context.assets.open("sample_face.jpg").use { it.readBytes() }
                            startAnalysis(CapturedImage(sampleBytes, 0, isSample = true))
                        } catch (e: Exception) {
                            errorMessage = "Sample face load failed: ${e.message}"
                        }
                    },
                    enabled = !isAnalyzing
                ) {
                    Text(
                        AppI18n.trySampleNoCam(isAnalyzing, currentLang),
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Top HUD: Bright Clinical Pearl Header + 4-Language Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(Color(0xEFFFFFFF), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("AURA MIRROR", color = Color(0xFF0F172A), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Spacer(Modifier.weight(1f))
                Surface(
                    onClick = {
                        val next = currentLang.next()
                        currentLang = next
                        routineStore.setSavedLang(next)
                    },
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, Color(0xFF10B981))
                ) {
                    Text(
                        text = "🌐 ${currentLang.buttonLabel}",
                        color = Color(0xFF059669),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            when (val res = sleepResult) {
                is SleepReadResult.Loading ->
                    Text(AppI18n.readingSleep(currentLang), color = Color(0xFF64748B), fontSize = 12.sp)
                is SleepReadResult.Success -> {
                    val s = res.sleepData
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            AppI18n.latestSleep(s.durationHours, s.isDeficit, currentLang),
                            color = if (s.isDeficit) Color(0xFFD97706) else Color(0xFF059669),
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(8.dp))
                        SourceBadge(DataSource.HEALTH_CONNECT, currentLang)
                    }
                }
                is SleepReadResult.PermissionRequired -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(AppI18n.sleepAccessNotGranted(currentLang), color = Color(0xFFE11D48), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onRequestHealthPermissions,
                        modifier = Modifier.height(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) { Text(AppI18n.grantBtn(currentLang), fontSize = 11.sp, color = Color.White) }
                }
                is SleepReadResult.NoData ->
                    Text(AppI18n.noSleep24h(currentLang), color = Color(0xFF64748B), fontSize = 12.sp)
                is SleepReadResult.Unavailable ->
                    Text(AppI18n.healthConnectUnavailable(currentLang), color = Color(0xFF64748B), fontSize = 12.sp)
                is SleepReadResult.Error ->
                    Text("Health Connect: ${res.message}", color = Color(0xFFE11D48), fontSize = 12.sp)
            }
            if (stageStep > 0) {
                Spacer(Modifier.height(4.dp))
                Text(AppI18n.stageText(stageStep, currentLang), color = Color(0xFF059669), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        errorMessage?.let { err ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                containerColor = Color(0xFFB00020),
                contentColor = Color.White,
                action = { TextButton(onClick = { errorMessage = null }) { Text("OK", color = Color.White) } }
            ) { Text(err, fontSize = 12.sp) }
        }
    }
}
