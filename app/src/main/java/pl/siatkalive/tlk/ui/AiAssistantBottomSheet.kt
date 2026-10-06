package pl.siatkalive.tlk.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class AiChatMessage(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val isUser: Boolean,
    val durationMs: Int? = null,
    val engine: String? = null,
    val isError: Boolean = false
)

private val QUICK_QUESTIONS = listOf(
    "Kiedy kolejny mecz Chemika?" to "Kiedy Chemik Police gra następny mecz?",
    "Ostatni wynik Chemika" to "Jak poszedł ostatni mecz Chemika Police?",
    "Miejsce w tabeli i lider" to "Na którym miejscu w tabeli jest Chemik i kto jest liderem ligi?",
    "Punktujące w Chemiku" to "Kto najlepiej punktuje w Chemiku Police?",
    "Blok w Chemiku" to "Kto najlepiej blokuje w Chemiku Police?",
    "MVP Chemika" to "Kto zdobył statuetkę MVP w Chemiku?",
    "Najlepsze blokujące ligi" to "Kto najlepiej blokuje w całej lidze?",
    "Trener i skład Chemika" to "Kto jest trenerem Chemika Police i jak wygląda skład?"
)

object TauronAiClient {
    private const val AI_ENDPOINT = "https://zukowski87.duckdns.org/api/v1/ai/ask"

    suspend fun askQuestion(question: String): AiChatMessage = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(AI_ENDPOINT)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 8000
                readTimeout = 10000
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("question", question)
            }.toString()

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload)
                writer.flush()
            }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val rawResponse = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }

            val json = JSONObject(rawResponse)
            if (json.optBoolean("ok", false)) {
                val answer = json.optString("answer", "Brak odpowiedzi.")
                val metrics = json.optJSONObject("metrics")
                val durationMs = metrics?.optInt("durationMs")
                val engine = metrics?.optString("engine")

                AiChatMessage(
                    text = answer,
                    isUser = false,
                    durationMs = durationMs,
                    engine = engine
                )
            } else {
                AiChatMessage(
                    text = "Błąd serwera: ${json.optString("error", "Nieznany błąd")}",
                    isUser = false,
                    isError = true
                )
            }
        } catch (e: Exception) {
            AiChatMessage(
                text = "Brak połączenia z asystentem (${e.localizedMessage ?: "błąd sieci"}).",
                isUser = false,
                isError = true
            )
        } finally {
            connection?.disconnect()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantBottomSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf(
            AiChatMessage(
                text = "Cześć! Jestem asystentem TAURON Ligi. Wybierz jedno z szybkich pytań poniżej lub wpisz własne (o tabelę, mecze, punktujące, blok lub zawodniczki).",
                isUser = false
            )
        )
    }

    fun sendQuestion(queryText: String) {
        val trimmed = queryText.trim()
        if (trimmed.isEmpty() || isLoading) return

        inputText = ""
        focusManager.clearFocus()
        messages.add(AiChatMessage(text = trimmed, isUser = true))
        isLoading = true

        coroutineScope.launch {
            listState.animateScrollToItem(messages.size - 1)
            val reply = TauronAiClient.askQuestion(trimmed)
            messages.add(reply)
            isLoading = false
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF005BAC).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFF005BAC),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Asystent Siatkarski AI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "TAURON Liga • Dane na żywo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Zamknij")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QUICK_QUESTIONS.forEach { (chipLabel, fullQuery) ->
                    SuggestionChip(
                        onClick = { sendQuestion(fullQuery) },
                        enabled = !isLoading,
                        label = {
                            Text(
                                text = chipLabel,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubble(message = msg)
                }

                if (isLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Pobieranie danych...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Zapytaj o mecz, tabelę, zawodniczkę...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = { sendQuestion(inputText) }
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilledIconButton(
                    onClick = { sendQuestion(inputText) },
                    enabled = inputText.isNotBlank() && !isLoading,
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFF005BAC)
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Wyślij",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: AiChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bgColor = when {
        message.isUser -> Color(0xFF005BAC)
        message.isError -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val textColor = when {
        message.isUser -> Color.White
        message.isError -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = bgColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = message.text,
                    color = textColor,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                AnimatedVisibility(visible = !message.isUser && message.durationMs != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = textColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Czas odpowiedzi: ${message.durationMs} ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
