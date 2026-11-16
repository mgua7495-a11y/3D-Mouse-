package com.example.a3dmausecad.ui.screen

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.example.a3dmausecad.utils.ScriptExporter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a3dmausecad.network.Network3DMouseManager
import com.example.a3dmausecad.network.NetworkStatus
import com.example.a3dmausecad.network.TransportMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TouchpadScreen(
    networkManager: Network3DMouseManager,
    hasPermissions: Boolean,
    keepScreenOn: Boolean,
    onKeepScreenOnChanged: (Boolean) -> Unit,
    onRequestPermissions: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val netStatus by networkManager.status.collectAsState()
    val currentPreset by networkManager.activePresetFlow.collectAsState()

    var pcIpInput by remember(networkManager.pcIpAddress) { mutableStateOf(networkManager.pcIpAddress) }
    var pcPortInput by remember(networkManager.pcPort) { mutableStateOf(networkManager.pcPort.toString()) }

    var showHelpDialog by remember { mutableStateOf(false) }
    var showSettingsPanel by remember { mutableStateOf(false) }
    var isScreenLocked by remember { mutableStateOf(false) }

    // Intercept and absorb Android Back gesture when locked to prevent app exit/minimization
    BackHandler(enabled = isScreenLocked) {
        // App is locked — Back gesture ignored
    }

    val prefs = remember { context.getSharedPreferences("3d_mouse_prefs", android.content.Context.MODE_PRIVATE) }

    // Sensitivity sliders & Target FPS & XYZ Buttons & Haptics & AMOLED Dimmer (Persisted in SharedPreferences)
    var sensitivity by remember { mutableFloatStateOf(prefs.getFloat("sensitivity", 12f)) }
    var zoomSensitivity by remember { mutableFloatStateOf(prefs.getFloat("zoom_sensitivity", 3f)) }
    var targetFps by remember { mutableIntStateOf(prefs.getInt("target_fps", 120)) }
    var lastSendTime by remember { mutableLongStateOf(0L) }
    var hapticsEnabled by remember { mutableStateOf(prefs.getBoolean("haptics_enabled", true)) }
    var hapticStrength by remember { mutableIntStateOf(prefs.getInt("haptic_strength", 5)) }

    // Touch Feedback & Values
    var touchPosition by remember { mutableStateOf<Offset?>(null) }
    var activeTouchPositions by remember { mutableStateOf<List<Offset>>(emptyList()) }

    // Smart AI Gesture Engine & Precision Mode State
    var aiGestureEngineEnabled by remember { mutableStateOf(prefs.getBoolean("ai_engine_enabled", true)) }
    var isPrecisionModeActive by remember { mutableStateOf(false) }

    // Multi-Touch Gesture Session Tracking
    var gestureSessionMaxPointers by remember { mutableIntStateOf(0) }
    var gestureSessionTotalDrag by remember { mutableFloatStateOf(0f) }
    var gestureSessionStartTime by remember { mutableLongStateOf(0L) }
    var gestureSessionSwipeVector by remember { mutableStateOf(Offset.Zero) }

    // Double-Tap Gesture Timers
    var lastOneFingerTapTime by remember { mutableLongStateOf(0L) }

    // AI Non-Linear Velocity Scaling
    fun applyAiVelocityScaling(delta: Offset, baseSensitivity: Float): Offset {
        if (!aiGestureEngineEnabled && !isPrecisionModeActive) return delta * baseSensitivity

        val distance = delta.getDistance()
        val speedMultiplier = when {
            isPrecisionModeActive -> 0.25f // Precision Mode Locked: 0.25x
            distance < 1.5f       -> 0.35f // Micro-Movement: Ultra-fine 0.35x
            distance > 15f        -> 1.65f // High-Speed Swipe: 1.65x
            else                  -> 1.00f // Standard Drag
        }
        return delta * (baseSensitivity * speedMultiplier)
    }

    // AI Axis Snapping (Snaps trajectory to pure X or Y axis if within ±8°)
    fun applyAiAxisSnapping(delta: Offset): Offset {
        if (!aiGestureEngineEnabled) return delta

        val absX = kotlin.math.abs(delta.x)
        val absY = kotlin.math.abs(delta.y)

        if (absX > 0f && (absY / absX) < 0.14f) {
            return Offset(delta.x, 0f)
        }
        if (absY > 0f && (absX / absY) < 0.14f) {
            return Offset(0f, delta.y)
        }
        return delta
    }

    // X, Y, Z Plane Buttons Visibility State
    var showXyzButtons by remember { mutableStateOf(prefs.getBoolean("show_xyz_buttons", false)) }

    // Touch Feedback & Values
    var currentRx by remember { mutableStateOf(0) }
    var currentRz by remember { mutableStateOf(0) }

    // Smoothing filters for buttery-smooth motion (Low-pass filter)
    var smoothedRx by remember { mutableFloatStateOf(0f) }
    var smoothedRz by remember { mutableFloatStateOf(0f) }
    var smoothedX by remember { mutableFloatStateOf(0f) }
    var smoothedY by remember { mutableFloatStateOf(0f) }
    var smoothedZ by remember { mutableFloatStateOf(0f) }

    // Border Customization Color Mode (0..9 for top 10 colors, 10 for RGB Rainbow)
    var borderColorMode by remember { mutableIntStateOf(prefs.getInt("border_color_mode", 0)) }

    // Animated Cyber HUD & Holographic Shockwave Transitions
    val infiniteTransition = rememberInfiniteTransition(label = "CyberHudTransition")
    val hudRotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "HudRotationAngle"
    )

    val shockwaveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShockwaveProgress"
    )

    val beamPulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "BeamPulseProgress"
    )

    val rgbHueAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RgbHueAnimation"
    )

    val activeBorderColor = when (borderColorMode) {
        0 -> Color(0xFFFF6D00) // Orange (Default)
        1 -> Color(0xFF00E5FF) // Cyan
        2 -> Color(0xFF00E676) // Green
        3 -> Color(0xFFD500F9) // Purple
        4 -> Color(0xFFFF1744) // Pink
        5 -> Color(0xFFFFEA00) // Yellow
        6 -> Color(0xFF2979FF) // Blue
        7 -> Color(0xFFFF3D00) // Red
        8 -> Color(0xFFFFFFFF) // White
        9 -> Color(0xFFB0BEC5) // Silver
        10 -> Color.hsv(rgbHueAnimation, 1f, 1f) // 🌈 RGB Rainbow
        else -> Color(0xFFFF6D00)
    }

    // Auto-discover PC on launch
    LaunchedEffect(Unit) {
        networkManager.activeMode = TransportMode.WIFI_UDP
        networkManager.startAutoDiscovery()
    }

    // Tactile Haptic Vibration Feedback Engine
    @Suppress("MissingPermission")
    fun performHaptic(extraMs: Long = 0L) {
        if (!hapticsEnabled) return
        try {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val manager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                val durationMs = ((hapticStrength * 3L) + extraMs).coerceIn(4L, 40L)
                val amplitude = (hapticStrength * 25).coerceIn(1, 255)
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(durationMs, amplitude))
            }
        } catch (_: Exception) {}
    }

    // Helper to send data over Wi-Fi UDP with configurable Target FPS rate-limiting
    fun send3DData(x: Int = 0, y: Int = 0, z: Int = 0, rx: Int = 0, rz: Int = 0, touchDown: Boolean = true) {
        val now = android.os.SystemClock.uptimeMillis()
        val minIntervalMs = (1000f / targetFps).toLong()

        if (!touchDown || (now - lastSendTime >= minIntervalMs)) {
            lastSendTime = now
            currentRx = rx
            currentRz = rz

            networkManager.send3DData(
                x = x.toShort(),
                y = y.toShort(),
                z = z.toShort(),
                rx = rx.toShort(),
                ry = 0,
                rz = rz.toShort(),
                isTouchDown = touchDown
            )
        }
    }

    fun sendZero() {
        smoothedRx = 0f
        smoothedRz = 0f
        smoothedX = 0f
        smoothedY = 0f
        smoothedZ = 0f
        send3DData(0, 0, 0, 0, 0, touchDown = false)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .border(width = 2.dp, color = activeBorderColor)
    ) {
        // Touchpad Surface with Multi-Touch Support (1 finger = Orbit, 2 fingers = Pan & Pinch Zoom)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(netStatus) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            activeTouchPositions = pressed.map { it.position }

                            val now = android.os.SystemClock.uptimeMillis()

                            if (pressed.isNotEmpty()) {
                                if (gestureSessionMaxPointers == 0) {
                                    gestureSessionStartTime = now
                                    gestureSessionTotalDrag = 0f
                                    gestureSessionSwipeVector = Offset.Zero
                                }
                                gestureSessionMaxPointers = maxOf(gestureSessionMaxPointers, pressed.size)

                                // Track pointer down for 1-finger double tap (Precision Mode)
                                if (event.changes.any { it.changedToDown() } && pressed.size == 1) {
                                    if (now - lastOneFingerTapTime < 280L) {
                                        isPrecisionModeActive = !isPrecisionModeActive
                                        performHaptic(extraMs = 12L)
                                    }
                                    lastOneFingerTapTime = now
                                }
                            }

                            if (pressed.isEmpty()) {
                                touchPosition = null
                                activeTouchPositions = emptyList()

                                // Gesture Classifier Execution upon finger release
                                val duration = now - gestureSessionStartTime
                                val maxPointers = gestureSessionMaxPointers
                                val totalDrag = gestureSessionTotalDrag
                                val swipeVec = gestureSessionSwipeVector

                                gestureSessionMaxPointers = 0 // Reset gesture session

                                if (maxPointers > 0) {
                                    if (totalDrag < 40f && duration < 400L) {
                                        // Tap Gestures
                                        when (maxPointers) {
                                            2 -> {
                                                networkManager.sendHomeCommand()
                                                performHaptic(extraMs = 15L)
                                            }
                                            3 -> {
                                                networkManager.sendUndoCommand()
                                                performHaptic(extraMs = 18L)
                                            }
                                            4 -> {
                                                networkManager.sendRedoCommand()
                                                performHaptic(extraMs = 20L)
                                            }
                                        }
                                    } else if (maxPointers == 3 && totalDrag >= 40f) {
                                        // 3-Finger Swipes for Orthogonal View Planes
                                        if (kotlin.math.abs(swipeVec.y) > kotlin.math.abs(swipeVec.x)) {
                                            if (swipeVec.y < 0) {
                                                networkManager.sendViewPlaneCommand("y") // Top View
                                            } else {
                                                networkManager.sendViewPlaneCommand("z") // Front View
                                            }
                                        } else {
                                            networkManager.sendViewPlaneCommand("x") // Right View
                                        }
                                        performHaptic(extraMs = 12L)
                                    }
                                }
                                sendZero()
                            } else if (pressed.size == 1) {
                                // 1 Finger: Orbit / Rotation (Rx, Rz) with AI Velocity Scaling & Axis Snapping
                                val change = pressed[0]
                                val rawDrag = change.position - change.previousPosition
                                if (rawDrag != Offset.Zero) {
                                    change.consume()
                                    touchPosition = change.position
                                    gestureSessionTotalDrag += rawDrag.getDistance()

                                    val snappedDrag = applyAiAxisSnapping(rawDrag)
                                    val scaledDrag = applyAiVelocityScaling(snappedDrag, sensitivity)

                                    val targetRx = scaledDrag.y * 10f
                                    val targetRz = scaledDrag.x * 10f

                                    smoothedRx = smoothedRx * 0.4f + targetRx * 0.6f
                                    smoothedRz = smoothedRz * 0.4f + targetRz * 0.6f

                                    send3DData(
                                        rx = smoothedRx.coerceIn(-32767f, 32767f).toInt(),
                                        rz = smoothedRz.coerceIn(-32767f, 32767f).toInt(),
                                        touchDown = true
                                    )
                                }
                            } else if (pressed.size == 2) {
                                // 2 Fingers: Pinch Zoom or 2-Finger Pan (X, Y)
                                val p1 = pressed[0]
                                val p2 = pressed[1]

                                val center = (p1.position + p2.position) / 2f
                                touchPosition = center

                                val prevCenter = (p1.previousPosition + p2.previousPosition) / 2f
                                val panDelta = center - prevCenter

                                val currentDistance = (p1.position - p2.position).getDistance()
                                val prevDistance = (p1.previousPosition - p2.previousPosition).getDistance()
                                val zoomDelta = if (prevDistance > 0f) currentDistance - prevDistance else 0f

                                p1.consume()
                                p2.consume()
                                gestureSessionTotalDrag += panDelta.getDistance() + kotlin.math.abs(zoomDelta)

                                if (kotlin.math.abs(zoomDelta) > 0.6f) {
                                    // Pinch Zoom
                                    val targetZ = zoomDelta * zoomSensitivity * 0.8f
                                    smoothedZ = smoothedZ * 0.35f + targetZ * 0.65f
                                    performHaptic()
                                    send3DData(z = smoothedZ.coerceIn(-32767f, 32767f).toInt(), touchDown = true)
                                } else if (panDelta.getDistance() > 0.5f) {
                                    // 2-Finger Pan (X, Y) with AI Velocity Scaling & Axis Snapping
                                    val snappedPan = applyAiAxisSnapping(panDelta)
                                    val scaledPan = applyAiVelocityScaling(snappedPan, sensitivity)

                                    val targetX = scaledPan.x * 8f
                                    val targetY = scaledPan.y * 8f
                                    smoothedX = smoothedX * 0.4f + targetX * 0.6f
                                    smoothedY = smoothedY * 0.4f + targetY * 0.6f
                                    send3DData(
                                        x = smoothedX.coerceIn(-32767f, 32767f).toInt(),
                                        y = smoothedY.coerceIn(-32767f, 32767f).toInt(),
                                        touchDown = true
                                    )
                                }
                            } else if (pressed.size >= 3) {
                                // 3+ Fingers: Trackpad Swipes for Orthogonal View Plane Snapping
                                val p1 = pressed[0]
                                val rawDrag = p1.position - p1.previousPosition
                                gestureSessionTotalDrag += rawDrag.getDistance()
                                gestureSessionSwipeVector += rawDrag
                                pressed.forEach { it.consume() }
                            }
                        }
                    }
                }
        ) {
            // Touchpad Surface (Pure AMOLED Black background with Animated Cyber HUD Touch Target)
            Canvas(modifier = Modifier.fillMaxSize()) {
                // 1. Draw 2-finger connecting Cyber Laser Beam & Center Midpoint HUD if 2 pointers are active
                if (activeTouchPositions.size >= 2) {
                    val p1 = activeTouchPositions[0]
                    val p2 = activeTouchPositions[1]
                    val mid = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)

                    // Outer Laser Glow
                    drawLine(
                        color = Color(0x44FF6D00),
                        start = p1,
                        end = p2,
                        strokeWidth = 6.dp.toPx()
                    )
                    // Inner Laser Beam
                    drawLine(
                        color = Color(0xFFFF8F00),
                        start = p1,
                        end = p2,
                        strokeWidth = 2.dp.toPx()
                    )
                    // Core White Laser Line
                    drawLine(
                        color = Color.White.copy(alpha = 0.8f),
                        start = p1,
                        end = p2,
                        strokeWidth = 1.dp.toPx()
                    )

                    // Traveling Energy Pulses along Beam
                    val pulsePos = Offset(
                        p1.x + (p2.x - p1.x) * beamPulseProgress,
                        p1.y + (p2.y - p1.y) * beamPulseProgress
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.5.dp.toPx(),
                        center = pulsePos
                    )
                    drawCircle(
                        color = Color(0xFFFF6D00),
                        radius = 8.dp.toPx(),
                        center = pulsePos
                    )

                    // Midpoint Reticle Target Ring
                    rotate(degrees = -hudRotationAngle * 1.5f, pivot = mid) {
                        val midRadius = 16.dp.toPx()
                        drawCircle(
                            color = Color(0xFFFFAB40),
                            radius = midRadius,
                            center = mid,
                            style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                        )
                    }
                    drawCircle(
                        color = Color(0xFFFF6D00),
                        radius = 5.dp.toPx(),
                        center = mid
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = mid
                    )
                }

                // 2. Animated Holographic HUD Target Reticles & Radar Shockwaves for all active touch pointers
                activeTouchPositions.forEach { pos ->
                    val cx = pos.x
                    val cy = pos.y

                    // Layer A: Radar Sonar Energy Shockwaves
                    val maxShockwaveRadius = 110.dp.toPx()
                    val shockwaveRadius = 25.dp.toPx() + (shockwaveProgress * (maxShockwaveRadius - 25.dp.toPx()))
                    val shockwaveAlpha = (1f - shockwaveProgress) * 0.45f

                    drawCircle(
                        color = Color(0xFFFF6D00).copy(alpha = shockwaveAlpha),
                        radius = shockwaveRadius,
                        center = pos,
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // Offset Secondary Radar Shockwave
                    val shockwave2Progress = (shockwaveProgress + 0.5f) % 1.0f
                    val shockwave2Radius = 25.dp.toPx() + (shockwave2Progress * (maxShockwaveRadius - 25.dp.toPx()))
                    val shockwave2Alpha = (1f - shockwave2Progress) * 0.35f
                    drawCircle(
                        color = Color(0xFFFF8F00).copy(alpha = shockwave2Alpha),
                        radius = shockwave2Radius,
                        center = pos,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // Layer B: Rotating Cyber HUD Segmented Tech Ring & 4 Cardinal Crosshairs
                    rotate(degrees = hudRotationAngle, pivot = pos) {
                        val ringRadius = 45.dp.toPx()

                        // Segmented Tech Arcs (0°, 90°, 180°, 270°)
                        val arcSize = androidx.compose.ui.geometry.Size(ringRadius * 2, ringRadius * 2)
                        val arcTopLeft = Offset(cx - ringRadius, cy - ringRadius)
                        val arcStyle = Stroke(width = 2.dp.toPx())

                        drawArc(color = Color(0xFFFF6D00), startAngle = 0f, sweepAngle = 60f, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = arcStyle)
                        drawArc(color = Color(0xFFFF6D00), startAngle = 90f, sweepAngle = 60f, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = arcStyle)
                        drawArc(color = Color(0xFFFF6D00), startAngle = 180f, sweepAngle = 60f, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = arcStyle)
                        drawArc(color = Color(0xFFFF6D00), startAngle = 270f, sweepAngle = 60f, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = arcStyle)

                        // Cardinal Crosshair Ticks
                        val tickLen = 8.dp.toPx()
                        val gap = ringRadius + 4.dp.toPx()
                        val strokeW = 2.dp.toPx()

                        drawLine(Color(0xFFFFAB40), Offset(cx, cy - gap), Offset(cx, cy - gap - tickLen), strokeWidth = strokeW)
                        drawLine(Color(0xFFFFAB40), Offset(cx, cy + gap), Offset(cx, cy + gap + tickLen), strokeWidth = strokeW)
                        drawLine(Color(0xFFFFAB40), Offset(cx - gap, cy), Offset(cx - gap - tickLen, cy), strokeWidth = strokeW)
                        drawLine(Color(0xFFFFAB40), Offset(cx + gap, cy), Offset(cx + gap + tickLen, cy), strokeWidth = strokeW)
                    }

                    // Layer C: Core Glow Disk
                    drawCircle(
                        color = Color(0x33FF6D00),
                        radius = 20.dp.toPx(),
                        center = pos
                    )

                    // Layer D: Core Dot (Bright Orange)
                    drawCircle(
                        color = Color(0xFFFF6D00),
                        radius = 10.dp.toPx(),
                        center = pos
                    )

                    // Layer E: Center White Precision Diamond Core
                    val d = 4.dp.toPx()
                    val diamondPath = Path().apply {
                        moveTo(cx, cy - d)
                        lineTo(cx + d, cy)
                        lineTo(cx, cy + d)
                        lineTo(cx - d, cy)
                        close()
                    }
                    drawPath(path = diamondPath, color = Color.White)
                }
            }

            // Central Touchpad Info / Status
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (touchPosition != null) {
                    Surface(
                        color = Color(0xAA181C24),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "3D Orbiting  |  Rx: $currentRx  Rz: $currentRz",
                            color = Color(0xFFFF8F00),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                } else if (isPrecisionModeActive) {
                    Surface(
                        color = Color(0xAA003040),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎯 AI PRECISION MODE (0.25x)",
                                color = Color(0xFF00E5FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (isScreenLocked) {
                    Surface(
                        color = Color(0xAA2A1515),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFFF6D00),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "APP EXIT PROTECTED",
                                color = Color(0xFFFF6D00),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Top Control & Header Area (Wi-Fi status, auto-discover, settings, help)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            color = Color.Black,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(top = 10.dp, bottom = 8.dp, start = 12.dp, end = 12.dp)
            ) {
                // Connection Status Bar with Lock Icon & Status Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val isConnected = netStatus == NetworkStatus.CONNECTED

                        // Round Connection Status Indicator Circle (Untouched)
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    color = if (isConnected) Color(0xFF00E676) else Color(0xFFFF5252),
                                    shape = CircleShape
                                )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Lock Gesture & App Exit Protection Button (White when open, Bright Orange when closed)
                        IconButton(
                            onClick = {
                                isScreenLocked = !isScreenLocked
                                performHaptic(extraMs = 8L)
                            }
                        ) {
                            Icon(
                                imageVector = if (isScreenLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = if (isScreenLocked) "Unlock Touchpad" else "Lock Touchpad",
                                tint = if (isScreenLocked) Color(0xFFFF6D00) else Color.White
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = {
                            networkManager.startAutoDiscovery()
                            performHaptic()
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Auto Discover", tint = Color(0xFFFF6D00))
                        }
                        IconButton(onClick = {
                            networkManager.sendHomeCommand()
                            performHaptic(extraMs = 10L)
                        }) {
                            Icon(Icons.Default.Home, contentDescription = "Home / Reset View", tint = Color(0xFFFF6D00))
                        }
                        IconButton(onClick = {
                            showHelpDialog = true
                            performHaptic()
                        }) {
                            Icon(Icons.Default.Info, contentDescription = "Help", tint = Color(0xFFFF6D00))
                        }
                        IconButton(onClick = {
                            showSettingsPanel = !showSettingsPanel
                            performHaptic()
                        }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color(0xFFFF6D00))
                        }
                    }
                }

                // X, Y, Z View Plane Buttons on Top Header (Not available for Fusion 360)
                AnimatedVisibility(
                    visible = showXyzButtons && currentPreset != "fusion360",
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                networkManager.sendViewPlaneCommand("x")
                                performHaptic()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF334B)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "X Plane",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Button(
                            onClick = {
                                networkManager.sendViewPlaneCommand("y")
                                performHaptic()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Y Plane",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }

                        Button(
                            onClick = {
                                networkManager.sendViewPlaneCommand("z")
                                performHaptic()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Z Plane",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Expandable Settings Panel
                AnimatedVisibility(
                    visible = showSettingsPanel,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(Color.Black, RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF222222), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Keep Screen Awake:", color = Color.White, fontSize = 13.sp)
                            Switch(
                                checked = keepScreenOn,
                                onCheckedChange = onKeepScreenOnChanged
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val isFusionPreset = currentPreset == "fusion360"
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Show X, Y, Z Plane Buttons:",
                                    color = if (isFusionPreset) Color.Gray else Color.White,
                                    fontSize = 13.sp
                                )
                                if (isFusionPreset) {
                                    Text(
                                        text = "(Not available for Fusion 360)",
                                        color = Color(0xFF888888),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Switch(
                                checked = showXyzButtons && !isFusionPreset,
                                enabled = !isFusionPreset,
                                onCheckedChange = {
                                    showXyzButtons = it
                                    prefs.edit().putBoolean("show_xyz_buttons", it).apply()
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("CAD App Preset:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val presets = listOf("fusion360" to "Fusion 360", "blender" to "Blender", "solidworks" to "SolidWorks")
                            presets.forEach { (key, label) ->
                                Button(
                                    onClick = { networkManager.activePreset = key },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (currentPreset == key) Color(0xFFFF6D00) else Color(0xFF2A3142)
                                    )
                                ) {
                                    Text(label, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Manual PC IP Address & Port:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = pcIpInput,
                                onValueChange = {
                                    pcIpInput = it
                                    networkManager.pcIpAddress = it.trim()
                                },
                                label = { Text("PC IP") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = pcPortInput,
                                onValueChange = {
                                    pcPortInput = it
                                    it.toIntOrNull()?.let { port -> networkManager.pcPort = port }
                                },
                                label = { Text("Port") },
                                modifier = Modifier.width(85.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { networkManager.connect() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6D00))
                        ) {
                            Text("Connect to IP", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Smart AI Gesture Engine:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("(Auto-Velocity, Axis Snapping, Double-Taps)", color = Color(0xFF888888), fontSize = 10.sp)
                            }
                            Switch(
                                checked = aiGestureEngineEnabled,
                                onCheckedChange = {
                                    aiGestureEngineEnabled = it
                                    prefs.edit().putBoolean("ai_engine_enabled", it).apply()
                                    performHaptic()
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Orbit Sensitivity: ${sensitivity.toInt()}x", color = Color.White, fontSize = 13.sp)
                        Slider(
                            value = sensitivity,
                            onValueChange = {
                                sensitivity = it
                                prefs.edit().putFloat("sensitivity", it).apply()
                            },
                            valueRange = 1f..30f,
                            steps = 29
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Zoom Sensitivity: ${zoomSensitivity.toInt()}x", color = Color.White, fontSize = 13.sp)
                        Slider(
                            value = zoomSensitivity,
                            onValueChange = {
                                zoomSensitivity = it
                                prefs.edit().putFloat("zoom_sensitivity", it).apply()
                            },
                            valueRange = 1f..30f,
                            steps = 29
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Data Update Rate (FPS): $targetFps Hz", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val fpsOptions = listOf(30, 60, 90, 120, 144, 165)
                            fpsOptions.forEach { fps ->
                                Button(
                                    onClick = {
                                        targetFps = fps
                                        prefs.edit().putInt("target_fps", fps).apply()
                                        performHaptic()
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (targetFps == fps) Color(0xFFFF6D00) else Color(0xFF2A3142)
                                    )
                                ) {
                                    Text(
                                        text = "$fps",
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        fontWeight = if (targetFps == fps) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Screen Border Customization:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val colorPresets = listOf(
                                Color(0xFFFF6D00), // Orange
                                Color(0xFF00E5FF), // Cyan
                                Color(0xFF00E676), // Green
                                Color(0xFFD500F9), // Purple
                                Color(0xFFFF1744), // Pink
                                Color(0xFFFFEA00), // Yellow
                                Color(0xFF2979FF), // Blue
                                Color(0xFFFF3D00), // Red
                                Color(0xFFFFFFFF), // White
                                Color(0xFFB0BEC5)  // Silver
                            )

                            colorPresets.forEachIndexed { index, color ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(color, CircleShape)
                                        .border(
                                            width = if (borderColorMode == index) 3.dp else 1.dp,
                                            color = if (borderColorMode == index) Color.White else Color(0xFF444444),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            borderColorMode = index
                                            prefs.edit().putInt("border_color_mode", index).apply()
                                            performHaptic()
                                        }
                                )
                            }

                            // 11th Circle: RGB Rainbow Mode Button
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color.hsv(rgbHueAnimation, 1f, 1f), CircleShape)
                                    .border(
                                        width = if (borderColorMode == 10) 3.dp else 1.dp,
                                        color = if (borderColorMode == 10) Color.White else Color(0xFF444444),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        borderColorMode = 10
                                        prefs.edit().putInt("border_color_mode", 10).apply()
                                        performHaptic()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🌈", fontSize = 10.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Haptic Vibration Feedback:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = hapticsEnabled,
                                onCheckedChange = {
                                    hapticsEnabled = it
                                    prefs.edit().putBoolean("haptics_enabled", it).apply()
                                    if (it) performHaptic()
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Vibration Strength: ${hapticStrength}x",
                            color = if (hapticsEnabled) Color.White else Color.Gray,
                            fontSize = 13.sp
                        )
                        Slider(
                            value = hapticStrength.toFloat(),
                            onValueChange = {
                                val newStr = it.toInt()
                                if (newStr != hapticStrength) {
                                    hapticStrength = newStr
                                    prefs.edit().putInt("haptic_strength", newStr).apply()
                                    if (hapticsEnabled) performHaptic()
                                }
                            },
                            valueRange = 1f..10f,
                            steps = 8,
                            enabled = hapticsEnabled
                        )
                    }
                }
            }
        }
    }

    // Help & PC Setup Dialog (English Guide with Direct Script Exporter Button)
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            containerColor = Color(0xFF14171E),
            shape = RoundedCornerShape(16.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFFF6D00),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "3D Mouse CAD — Setup Guide",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Step 1: Download Receiver Script
                    Text("1. Download PC Receiver Scripts", color = Color(0xFFFF8F00), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap the button below to save the PC receiver files (start_pc_receiver.bat & space_mouse_receiver.py) directly into your phone's Downloads folder. Transfer them to your PC.",
                        color = Color(0xFFCCCCCC),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val success = ScriptExporter.exportReceiverScriptsToDownloads(context)
                            if (success) {
                                Toast.makeText(context, "Saved start_pc_receiver.bat & space_mouse_receiver.py to Downloads!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Failed to save files to Downloads.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6D00)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Download PC Receiver Files (.bat & .py)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Step 2: Install Python
                    Text("2. Install Python on Computer (~50 MB)", color = Color(0xFFFF8F00), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Download and install Python from the official website (https://www.python.org). It takes only ~50 MB and has zero impact on your PC performance. Python is required to establish communication between your Android smartphone and PC to transmit your 3D touch gestures in real time.\n\nImportant during installation: Check the box 'Add Python to PATH'.",
                        color = Color(0xFFCCCCCC),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            try {
                                uriHandler.openUri("https://www.python.org")
                            } catch (_: Exception) {
                                Toast.makeText(context, "Official website: https://www.python.org", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A3142)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Open Official Python Website (python.org)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Step 3: Wi-Fi Router or PC Hotspot Connection
                    Text("3. Choose PC IP Address (Wi-Fi Router vs PC Hotspot)", color = Color(0xFFFF8F00), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "When you launch 'start_pc_receiver.bat', the PC console window displays all your active IP addresses:\n\n" +
                                "• Wi-Fi Router Connection: Enter your router IP (e.g., 192.168.X.X) in app Settings if both phone and PC are connected to the same Wi-Fi router.\n\n" +
                                "• PC Mobile Hotspot Connection: Enter your PC Hotspot IP (e.g., 192.168.137.X) in app Settings if your phone is connected directly to your PC's Wi-Fi Hotspot.\n\n" +
                                "Note: Router IP and Hotspot IP are two different IP addresses! You can also tap the Refresh Auto-Discover icon in the top header to connect automatically.",
                        color = Color(0xFFCCCCCC),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Step 4: Run Script
                    Text("4. Launch Receiver Script on PC", color = Color(0xFFFF8F00), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Double-click 'start_pc_receiver.bat' on your PC to launch the receiver server. Keep the console window running in the background while modeling.",
                        color = Color(0xFFCCCCCC),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Step 5: Verify Connection
                    Text("5. Verify Connection & Start Modeling", color = Color(0xFFFF8F00), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Look for the Green Status Circle indicator in the top-left corner of the app. Open Fusion 360, Blender, SolidWorks, or FreeCAD and enjoy 3D viewport control!",
                        color = Color(0xFFCCCCCC),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Step 6: Smart AI Gestures Reference
                    Text("6. Smart Multi-Touch Gestures Reference", color = Color(0xFFFF8F00), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• 1-Finger Drag: 3D Orbit / Model Rotation\n" +
                                "• 1-Finger Double-Tap: Toggle Precision Mode / Slowdown (0.25x)\n" +
                                "• 2-Finger Pinch / Stretch: Zoom 3D Object In / Out\n" +
                                "• 2-Finger Drag: 3D Pan / Move Viewport\n" +
                                "• 2-Finger Double-Tap: Fit to Screen / Reset Home View\n" +
                                "• 3-Finger Tap: Undo Last Action (Ctrl + Z)\n" +
                                "• 4-Finger Tap: Redo Action (Ctrl + Y / Ctrl + Shift + Z)\n" +
                                "• 3-Finger Swipes: Instant View Plane Snap (Top, Front, Side Views)",
                        color = Color(0xFFCCCCCC),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Creator Instagram Credit at the very bottom, centered
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Created by: instagram m4ximys",
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Got it!", color = Color(0xFFFF6D00), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
