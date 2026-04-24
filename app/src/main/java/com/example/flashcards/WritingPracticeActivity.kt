package com.example.flashcards

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PlatformImeOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flashcards.domain.AppSettings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.flashcards.domain.SwipeResult
import com.example.flashcards.ui.CardsViewModel

class WritingPracticeActivity : ComponentActivity() {
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
                        WritingScreen(
                            state = ui,
                            onSwipe = vm::onSwipe,
                            onSpeak = vm::speakText,
                        )
                    } else {
                        WritingSettingsScreen(
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
private fun WritingScreen(
    state: com.example.flashcards.ui.CardsUiState,
    onSwipe: (SwipeResult) -> Unit,
    onSpeak: (String) -> Unit,
) {
    if (state.currentCard == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(state.message ?: "Нет карточек")
        }
        return
    }

    var answer by remember(state.currentCard.id) { mutableStateOf("") }
    var isAnswerCorrect by remember(state.currentCard.id) { mutableStateOf<Boolean?>(null) }
    var checkMessage by remember(state.currentCard.id) { mutableStateOf<String?>(null) }
    var isInputLocked by remember(state.currentCard.id) { mutableStateOf(false) }
    var voiceError by remember(state.currentCard.id) { mutableStateOf<String?>(null) }
    var dragX by remember(state.currentCard.id) { mutableFloatStateOf(0f) }

    val context = LocalContext.current
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            if (spoken.isNotBlank() && !isInputLocked) {
                answer = spoken
                isAnswerCorrect = null
                checkMessage = null
                voiceError = null
            }
        }
    }

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

    val phraseColor = when (isAnswerCorrect) {
        true -> Color(0xFF2E7D32)
        false -> Color.Red
        null -> MaterialTheme.colorScheme.onSurface
    }

    val russianPhrase = state.currentCard.back ?: state.currentCard.front

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(gestureModifier)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.settings.showCardWeight) {
            Text(
                text = "Вес: ${"%.2f".format(state.cardWeight)}",
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = (12f * state.settings.cardTextScale).sp,
            )
            Spacer(Modifier.height(6.dp))
        }
        Text(
            text = russianPhrase,
            color = phraseColor,
            style = MaterialTheme.typography.headlineSmall,
            fontSize = (30f * state.settings.cardTextScale).sp,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = answer,
            onValueChange = {
                if (!isInputLocked) {
                    answer = it
                    isAnswerCorrect = null
                    checkMessage = null
                    voiceError = null
                }
            },
            label = { Text("Введите английскую фразу") },
            keyboardOptions = KeyboardOptions(
                autoCorrect = false,
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Password,
                platformImeOptions = PlatformImeOptions(privateImeOptions = "nm"),
            ),
            enabled = !isInputLocked,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak the English phrase")
            }
            val canHandle = intent.resolveActivity(context.packageManager) != null
            if (canHandle && !isInputLocked) {
                speechLauncher.launch(intent)
            } else if (!canHandle) {
                voiceError = "Голосовой ввод недоступен на устройстве"
            }
        }, enabled = !isInputLocked) {
            Text("Ввести голосом")
        }
        voiceError?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = Color.Red)
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            val correct = matchesIgnoringCaseAndPunctuation(
                typed = answer,
                expected = state.currentCard.front,
            )
            isAnswerCorrect = correct
            isInputLocked = true
            checkMessage = if (correct) {
                "Верно"
            } else {
                state.currentCard.front
            }
        }) {
            Text("Проверка")
        }
        checkMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it)
        }

        Spacer(Modifier.height(20.dp))

        if (state.settings.showAndroidTtsButton) {
            Spacer(Modifier.height(8.dp))
            Button(onClick = { onSpeak(state.currentCard.front) }) {
                Text("Озвучить")
            }
        }

        if (state.settings.useSwipeMode) {
            Text("Свайп влево = Не знаю, вправо = Знаю")
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onSwipe(SwipeResult.DONT_KNOW) }) { Text("Не знаю") }
                Button(onClick = { onSwipe(SwipeResult.KNOW) }) { Text("Знаю") }
            }
        }
    }
}


@Composable
private fun WritingSettingsScreen(
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

private fun matchesIgnoringCaseAndPunctuation(typed: String, expected: String): Boolean {
    fun normalize(text: String): String {
        val cleaned = buildString {
            text.forEach { ch ->
                when {
                    ch.isLetterOrDigit() -> append(ch.lowercaseChar())
                    ch.isWhitespace() -> append(' ')
                    else -> append(' ')
                }
            }
        }

        return cleaned
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    return normalize(typed) == normalize(expected)
}
