package com.example.flashcards

import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import com.example.flashcards.data.MediaLocator
import com.example.flashcards.domain.SwipeResult
import com.example.flashcards.ui.CardsViewModel
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    private val vm by viewModels<CardsViewModel>()

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
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
                            onSpoiler = vm::openSpoiler,
                        )
                    } else {
                        SettingsScreen(interval = ui.settings.reminderIntervalMinutes)
                    }
                }
            }
        }
    }
}

@Composable
private fun CardScreen(
    state: com.example.flashcards.ui.CardsUiState,
    onSwipe: (SwipeResult) -> Unit,
    onReveal: () -> Unit,
    onPlayAudio: (Boolean) -> Unit,
    onSpoiler: () -> Unit,
) {
    if (state.currentCard == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(state.message ?: "Нет карточек")
        }
        return
    }

    var offsetX by remember { mutableFloatStateOf(0f) }
    val animated by animateFloatAsState(offsetX, label = "offset")
    var audioModeSlow by remember { mutableStateOf(false) }

    val textColor = when {
        state.cardWeight >= 1.0 -> Color.Red
        state.cardWeight <= -0.8 && state.cardWeight > -1.0 -> Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount -> offsetX += dragAmount },
                    onDragEnd = {
                        when {
                            animated > 180f -> onSwipe(SwipeResult.KNOW)
                            animated < -180f -> onSwipe(SwipeResult.DONT_KNOW)
                        }
                        offsetX = 0f
                    }
                )
            }
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = state.currentCard.front, color = textColor, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        if (state.currentCard.back != null) {
            Button(onClick = onReveal) { Text("Показать перевод") }
        }
        if (state.showTranslation && state.currentCard.back != null) {
            Spacer(Modifier.height(8.dp))
            Text(state.currentCard.back)
        }
        Spacer(Modifier.height(8.dp))
        if (state.audioPath != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { onPlayAudio(audioModeSlow) }) { Text("Озвучить") }
                Spacer(Modifier.size(8.dp))
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = !audioModeSlow,
                        onClick = { audioModeSlow = false },
                        shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("Normal") }
                    SegmentedButton(
                        selected = audioModeSlow,
                        onClick = { audioModeSlow = true },
                        shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("Slow") }
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
        Text("Свайп влево = не знаю, вправо = знаю")
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
private fun SettingsScreen(interval: Long) {
    var rawInterval by remember(interval) { mutableStateOf(interval.toString()) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("MVP настройки")
        OutlinedTextField(
            value = rawInterval,
            onValueChange = { if (it.all(Char::isDigit)) rawInterval = it },
            label = { Text("Интервал напоминания (мин)") },
            modifier = Modifier.fillMaxWidth()
        )
        Text("В этом MVP настройки хранятся в DataStore и расширяются в SettingsRepository.")
    }
}
