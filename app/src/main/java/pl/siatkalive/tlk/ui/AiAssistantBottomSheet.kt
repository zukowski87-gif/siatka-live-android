package pl.siatkalive.tlk.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pl.siatkalive.tlk.data.ApiClient
import pl.siatkalive.tlk.data.PlayerSearchItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSearchBottomSheet(
    onDismiss: () -> Unit,
    onPlayerSelected: (Int) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<PlayerSearchItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Szukaj zawodniczki", style = MaterialTheme.typography.titleLarge)
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { query ->
                    searchQuery = query
                    if (query.length >= 2) {
                        coroutineScope.launch {
                            isLoading = true
                            try {
                                val response = ApiClient.api.searchPlayers(query)
                                searchResults = response.results
                            } catch (e: Exception) {
                                searchResults = emptyList()
                            } finally {
                                isLoading = false
                            }
                        }
                    } else {
                        searchResults = emptyList()
                    }
                },
                label = { Text("Wpisz imię lub nazwisko...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    items(searchResults) { player ->
                        ListItem(
                            headlineContent = { Text(player.name) },
                            supportingContent = { Text("${player.teamName ?: ""} • ${player.position ?: player.value ?: ""}") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    player.playerId?.let { id ->
                                        if (id > 0) {
                                            onPlayerSelected(id)
                                            onDismiss()
                                        }
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}
