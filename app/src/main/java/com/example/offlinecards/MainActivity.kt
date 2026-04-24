package com.example.offlinecards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.offlinecards.ui.CardViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val vm: CardViewModel = viewModel()
                    val state by vm.state.collectAsState()
                    CardScreen(
                        russianText = state.currentCard?.russian ?: "Нет карточек",
                        userInput = state.userInput,
                        checkResult = state.checkResult,
                        onInputChanged = vm::onInputChanged,
                        onCheckClick = vm::checkAnswer,
                        onKnowClick = { vm.onKnowResult(true) },
                        onDontKnowClick = { vm.onKnowResult(false) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CardScreen(
    russianText: String,
    userInput: String,
    checkResult: Boolean?,
    onInputChanged: (String) -> Unit,
    onCheckClick: () -> Unit,
    onKnowClick: () -> Unit,
    onDontKnowClick: () -> Unit
) {
    val phraseColor = when (checkResult) {
        true -> Color(0xFF1B8A3A)
        false -> Color(0xFFB3261E)
        null -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = russianText,
            style = MaterialTheme.typography.headlineSmall,
            color = phraseColor
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = userInput,
            onValueChange = onInputChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Введите английский перевод") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onCheckClick) {
            Text("Проверка")
        }

        if (checkResult != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onKnowClick) {
                Text("Знаю")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onDontKnowClick) {
                Text("Не знаю")
            }
        }
    }
}
