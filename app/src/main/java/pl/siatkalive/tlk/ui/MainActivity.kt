package pl.siatkalive.tlk.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pl.siatkalive.tlk.BuildConfig
import pl.siatkalive.tlk.data.*
import pl.siatkalive.tlk.widget.TauronGlanceWidget

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                TauronAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TauronAppScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("siatka_prefs", Context.MODE_PRIVATE) }

    var selectedTab by remember { mutableIntStateOf(0) }
    var favTeam by remember { mutableStateOf(prefs.getString("fav_team", "Chemik") ?: "Chemik") }
    var serverUrl by remember { mutableStateOf(prefs.getString("bff_url", BuildConfig.BFF_BASE_URL) ?: BuildConfig.BFF_BASE_URL) }

    var widgetState by remember { mutableStateOf<WidgetResponse?>(null) }
    var standings by remember { mutableStateOf<List<StandingRowDto>>(emptyList()) }
    var matches by remember { mutableStateOf<List<MatchDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun syncAll() {
        try {
            errorMsg = null
            val api = ApiClient.getApi(serverUrl)
            widgetState = api.getWidgetData(favTeam)
            matches = api.getMatches().matches
            standings = api.getStandings().standings
            TauronGlanceWidget().updateAll(context)
        } catch (e: Exception) {
            errorMsg = "Brak połączenia z serwerem ($serverUrl): ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(favTeam, serverUrl) {
        while (true) {
            syncAll()
            val delayMs = if (widgetState?.liveMatchesCount ?: 0 > 0) 10_000L else 30_000L
            delay(delayMs)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🏐 Siatka Kobiet Live", fontWeight = FontWeight.ExtraBold) },
                actions = {
                    TextButton(onClick = { scope.launch { syncAll() } }) {
                        Text("↻ Odśwież", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("Mecze & Live") },
                    icon = { Text("⚡") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text("Tabela") },
                    icon = { Text("📊") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    label = { Text("Ustawienia") },
                    icon = { Text("⚙️") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
        ) {
            if (errorMsg != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                ) {
                    Text(errorMsg!!, color = Color(0xFFFCA5A5), modifier = Modifier.padding(10.dp), fontSize = 12.sp)
                }
            }

            if (isLoading && matches.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> MatchesTab(widgetState?.featuredMatch, matches, favTeam)
                    1 -> StandingsTab(standings, favTeam)
                    2 -> SettingsTab(
                        favTeam = favTeam,
                        serverUrl = serverUrl,
                        onSave = { newTeam, newUrl ->
                            prefs.edit().putString("fav_team", newTeam).putString("bff_url", newUrl).apply()
                            favTeam = newTeam
                            serverUrl = newUrl
                            selectedTab = 0
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MatchesTab(featured: MatchDto?, matches: List<MatchDto>, favTeam: String) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        if (featured != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = if (featured.isLive) "● TRWA NA ŻYWO – SET ${featured.currentSet ?: 1}" else "CENTRUM MECZOWE: ${featured.dateLabel}",
                            color = if (featured.isLive) Color(0xFFEF4444) else Color(0xFF38BDF8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(featured.homeTeam.name, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                text = if (featured.status == "PLANNED") "VS" else "${featured.homeSets} : ${featured.awaySets}",
                                color = Color(0xFFFACC15),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            Text(featured.awayTeam.name, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        if (featured.sets.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = featured.sets.joinToString("   |   ") { "S${it.setNumber}: ${it.homePoints}:${it.awayPoints}" },
                                color = Color(0xFFE2E8F0),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        items(matches, key = { it.id }) { m ->
            val isFav = m.homeTeam.name.contains(favTeam, true) || m.awayTeam.name.contains(favTeam, true)
            val cardBg = if (isFav) Color(0xFF172554) else Color(0xFF0F172A)
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = if (m.isLive) "● LIVE (SET ${m.currentSet ?: 1})" else m.dateLabel,
                            color = if (m.isLive) Color(0xFFEF4444) else Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(m.sets.joinToString(" | ") { "${it.homePoints}:${it.awayPoints}" }, color = Color(0xFFCBD5E1), fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(m.homeTeam.name, Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = if (m.status == "PLANNED") "vs" else "${m.homeSets}:${m.awaySets}",
                            color = Color(0xFFFACC15),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                        Text(m.awayTeam.name, Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun StandingsTab(rows: List<StandingRowDto>, favTeam: String) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text("#", Modifier.width(28.dp), fontWeight = FontWeight.Bold, color = Color.Gray)
                Text("Drużyna", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color.Gray)
                Text("M", Modifier.width(32.dp), fontWeight = FontWeight.Bold, color = Color.Gray)
                Text("Sety", Modifier.width(48.dp), fontWeight = FontWeight.Bold, color = Color.Gray)
                Text("Pkt", Modifier.width(36.dp), fontWeight = FontWeight.Bold, color = Color.Gray)
            }
        }
        items(rows, key = { it.position }) { r ->
            val isFav = r.name.contains(favTeam, true)
            val rowBg = if (isFav) Color(0xFF1E3A8A) else Color(0xFF0F172A)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(rowBg)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${r.position}.", Modifier.width(28.dp), fontWeight = FontWeight.Bold)
                Text(r.name, Modifier.weight(1f), fontWeight = if (isFav) FontWeight.Bold else FontWeight.Normal)
                Text("${r.matchesPlayed}", Modifier.width(32.dp))
                Text("${r.setsWon}:${r.setsLost}", Modifier.width(48.dp))
                Text("${r.points}", Modifier.width(36.dp), fontWeight = FontWeight.Bold, color = Color(0xFFFACC15))
            }
        }
    }
}

@Composable
fun SettingsTab(favTeam: String, serverUrl: String, onSave: (String, String) -> Unit) {
    var teamInput by remember { mutableStateOf(favTeam) }
    var urlInput by remember { mutableStateOf(serverUrl) }

    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Konfiguracja Aplikacji i Widżetu", fontWeight = FontWeight.Bold, fontSize = 18.sp)

        OutlinedTextField(
            value = teamInput,
            onValueChange = { teamInput = it },
            label = { Text("Ulubiona drużyna na widżecie (np. Chemik)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            label = { Text("Adres serwera BFF (np. http://192.168.1.x:3000/)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { onSave(teamInput.trim(), urlInput.trim()) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Zapisz i zaktualizuj widżet")
        }
    }
}
