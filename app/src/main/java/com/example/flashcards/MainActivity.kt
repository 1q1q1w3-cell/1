package com.example.flashcards

import android.graphics.BitmapFactory
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flashcards.data.MediaLocator
import com.example.flashcards.domain.AppSettings
import com.example.flashcards.domain.SwipeResult
import com.example.flashcards.ui.CardsViewModel
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val vm by viewModels<CardsViewModel>()

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        requestLegacyStoragePermissionsIfNeeded()
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                val ui by vm.uiState.collectAsState()
                var tabIndex by remember { mutableIntStateOf(0) }

                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    TabRow(selectedTabIndex = tabIndex) {
                        Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Карточки") })
                        Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Настройки") })
                    }
                    Spacer(Modifier.height(12.dp))
                    if (tabIndex == 0) {
                        CardScreen(
                            state = ui,
                            onSwipe = vm::onSwipe,
                            onReveal = vm::revealTranslation,
                            onPlayAudio = vm::playAudio,
                            onSetSegment = vm::setSegment,
                            onSetPinSegment = vm::setPinSegment,
                            onSpeak = vm::speakText,
                            onSpoiler = vm::openSpoiler,
                        )
                    } else {
                        SettingsScreen(
                            current = ui.settings,
                            onSave = vm::saveSettings,
                        )
                    }
                }
            }
        }
    }

    private fun requestLegacyStoragePermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT > 28) return
        val permissions = arrayOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )
        val needRequest = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (needRequest) {
            ActivityCompat.requestPermissions(this, permissions, 1001)
        }
    }
}

@Composable
private fun CardScreen(
    state: com.example.flashcards.ui.CardsUiState,
    onSwipe: (SwipeResult) -> Unit,
    onReveal: () -> Unit,
    onPlayAudio: (Boolean) -> Unit,
    onSetSegment: (Int, Int) -> Unit,
    onSetPinSegment: (Boolean) -> Unit,
    onSpeak: (String) -> Unit,
    onSpoiler: () -> Unit,
) {
    if (state.currentCard == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(state.message ?: "Нет карточек")
        }
        return
    }

    var audioModeSlow by remember { mutableStateOf(false) }
    var localRange by remember(state.currentCard.id, state.segmentDurationMs) {
        mutableStateOf(state.segmentStartMs.toFloat()..state.segmentEndMs.toFloat())
    }
    var dragX by remember(state.currentCard.id) { mutableFloatStateOf(0f) }
    val gestureModifier = if (state.settings.useSwipeMode) {
        Modifier.pointerInput(state.currentCard.id) {
            detectHorizontalDragGestures(
                onHorizontalDrag = { _, amount -> dragX += amount },
                onDragEnd = {
                    when {
                        dragX >= 120f -> onSwipe(SwipeResult.KNOW)
                        dragX <= -120f -> onSwipe(SwipeResult.DONT_KNOW)
                    }
                    dragX = 0f
                },
                onDragCancel = { dragX = 0f },
            )
        }
    } else {
        Modifier
    }

    val textColor = when {
        state.cardWeight >= 1.0 -> Color.Red
        state.cardWeight <= -0.8 && state.cardWeight > -1.0 -> Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(gestureModifier)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (state.audioPath != null && state.segmentDurationMs > 0) {
                Switch(
                    checked = state.pinSegmentForNextShow,
                    onCheckedChange = onSetPinSegment
                )
            }
        }
        if (state.settings.showCardWeight) {
            Text(
                text = "Вес: ${"%.2f".format(state.cardWeight)}",
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = (12f * state.settings.cardTextScale).sp,
            )
            Spacer(Modifier.height(6.dp))
        }
        if (state.audioPath != null && state.segmentDurationMs > 0) {
            RangeSlider(
                value = localRange,
                onValueChange = {
                    localRange = it
                    onSetSegment(it.start.roundToInt(), it.endInclusive.roundToInt())
                },
                valueRange = 0f..state.segmentDurationMs.toFloat(),
            )
            Spacer(Modifier.height(8.dp))
        }
        Text(
            text = state.currentCard.front,
            color = textColor,
            style = MaterialTheme.typography.headlineSmall,
            fontSize = (30f * state.settings.cardTextScale).sp,
        )
        Spacer(Modifier.height(16.dp))
        if (state.currentCard.back != null) {
            Button(onClick = onReveal) { Text("Показать перевод") }
        }
        if (state.showTranslation && state.currentCard.back != null) {
            Spacer(Modifier.height(8.dp))
            Text(state.currentCard.back, fontSize = (22f * state.settings.cardTextScale).sp)
        }
        Spacer(Modifier.height(8.dp))
        if (state.settings.showAndroidTtsButton) {
            Button(onClick = { onSpeak(state.currentCard.front) }) {
                Text("Озвучить текст")
            }
            Spacer(Modifier.height(8.dp))
        }
        if (state.audioPath != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { onPlayAudio(audioModeSlow) }) { Text("Озвучить") }
                Spacer(Modifier.size(8.dp))
                TextButton(onClick = { audioModeSlow = false }) {
                    Text(if (!audioModeSlow) "● Нормально" else "Нормально")
                }
                TextButton(onClick = { audioModeSlow = true }) {
                    Text(if (audioModeSlow) "● Медленно" else "Медленно")
                }
            }
        }

        if (state.imagePath != null) {
            Spacer(Modifier.height(8.dp))
            Button(onClick = onSpoiler, enabled = !state.spoilerOpened) { Text("Спойлер") }
            if (state.spoilerOpened) {
                Spacer(Modifier.height(8.dp))
                SpoilerImage(path = state.imagePath, sizePx = state.settings.spoilerSizePx)
            }
        }

        Spacer(Modifier.height(16.dp))
        if (state.settings.useSwipeMode) {
            Text("Свайп влево = Не знаю, вправо = Знаю")
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onSwipe(SwipeResult.DONT_KNOW) }) { Text("Не знаю") }
                Button(onClick = { onSwipe(SwipeResult.KNOW) }) { Text("Знаю") }
            }
        }
        state.message?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(8.dp))
            Text(it)
        }
    }
}

@Composable
private fun SpoilerImage(path: String, sizePx: Int) {
    val context = LocalContext.current
    val locator = remember { MediaLocator(context) }
    var bitmap by remember(path) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(path, sizePx) {
        runCatching {
            val bytes = locator.loadImageBytes(path)
            val source = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return@runCatching null
            val w = minOf(sizePx, source.width)
            val h = minOf(sizePx, source.height)
            android.graphics.Bitmap.createBitmap(source, 0, 0, w, h)
        }.onSuccess { bitmap = it }
    }

    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "spoiler",
            modifier = Modifier.fillMaxWidth().height(200.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun SettingsScreen(
    current: AppSettings,
    onSave: (AppSettings) -> Unit,
) {
    var rawStepBad by remember(current.stepBad) { mutableStateOf(current.stepBad.toString()) }
    var rawStepGood by remember(current.stepGood) { mutableStateOf(current.stepGood.toString()) }
    var rawChancePower by remember(current.chancePower) { mutableStateOf(current.chancePower.toString()) }
    var rawMinChance by remember(current.minChance) { mutableStateOf(current.minChance.toString()) }
    var rawSlowSpeed by remember(current.slowAudioSpeed) { mutableStateOf(current.slowAudioSpeed.toString()) }
    var showCardWeight by remember(current.showCardWeight) { mutableStateOf(current.showCardWeight) }
    var useSwipeMode by remember(current.useSwipeMode) { mutableStateOf(current.useSwipeMode) }
    var showAndroidTts by remember(current.showAndroidTtsButton) { mutableStateOf(current.showAndroidTtsButton) }
    var cardTextScale by remember(current.cardTextScale) { mutableStateOf(current.cardTextScale) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Настройки")
        OutlinedTextField(
            value = rawStepBad,
            onValueChange = { rawStepBad = it },
            label = { Text("Шаг при ответе «Не знаю»") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = rawStepGood,
            onValueChange = { rawStepGood = it },
            label = { Text("Шаг при ответе «Знаю»") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = rawChancePower,
            onValueChange = { rawChancePower = it },
            label = { Text("Сила влияния приоритета") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = rawMinChance,
            onValueChange = { rawMinChance = it },
            label = { Text("Минимальный шанс показа") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = rawSlowSpeed,
            onValueChange = { rawSlowSpeed = it },
            label = { Text("Скорость медленной озвучки") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Показывать вес карточки")
            Switch(checked = showCardWeight, onCheckedChange = { showCardWeight = it })
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Режим свайпов (иначе кнопки)")
            Switch(checked = useSwipeMode, onCheckedChange = { useSwipeMode = it })
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Показывать кнопку озвучки Android")
            Switch(checked = showAndroidTts, onCheckedChange = { showAndroidTts = it })
        }
        Text("Размер текста карточки: x${"%.1f".format(cardTextScale)}")
        Slider(
            value = cardTextScale,
            onValueChange = { cardTextScale = it },
            valueRange = 0.8f..1.8f
        )
        Button(onClick = {
            val updated = current.copy(
                stepBad = rawStepBad.toDoubleOrNull() ?: current.stepBad,
                stepGood = rawStepGood.toDoubleOrNull() ?: current.stepGood,
                chancePower = rawChancePower.toDoubleOrNull() ?: current.chancePower,
                minChance = rawMinChance.toDoubleOrNull() ?: current.minChance,
                slowAudioSpeed = rawSlowSpeed.toFloatOrNull() ?: current.slowAudioSpeed,
                showCardWeight = showCardWeight,
                useSwipeMode = useSwipeMode,
                showAndroidTtsButton = showAndroidTts,
                cardTextScale = cardTextScale,
            )
            onSave(updated)
        }) {
            Text("Сохранить")
        }
    }
}
