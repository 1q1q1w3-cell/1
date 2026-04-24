package com.example.flashcards

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.flashcards.domain.SwipeResult
import com.example.flashcards.ui.CardsViewModel
import java.util.Locale

class WritingPracticeActivity : ComponentActivity() {
    private val vm by viewModels<CardsViewModel>()

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        requestLegacyStoragePermissionsIfNeeded()
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                val ui by vm.uiState.collectAsState()
                WritingScreen(
                    state = ui,
                    onSwipe = vm::onSwipe,
                )
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
) {
    if (state.currentCard == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(state.message ?: "Нет карточек")
        }
        return
    }

    var answer by remember(state.currentCard.id) { mutableStateOf("") }
    var isAnswerCorrect by remember(state.currentCard.id) { mutableStateOf<Boolean?>(null) }
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
                answer = it
                isAnswerCorrect = null
            },
            label = { Text("Введите английскую фразу") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            isAnswerCorrect = matchesIgnoringCaseAndPunctuation(
                typed = answer,
                expected = state.currentCard.front,
            )
        }) {
            Text("Проверка")
        }

        Spacer(Modifier.height(20.dp))
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

private fun matchesIgnoringCaseAndPunctuation(typed: String, expected: String): Boolean {
    fun normalize(text: String): String {
        return text
            .lowercase(Locale.ROOT)
            .replace(Regex("[\\p{Punct}]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    return normalize(typed) == normalize(expected)
}
