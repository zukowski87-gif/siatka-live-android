package pl.siatkalive.tlk.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import pl.siatkalive.tlk.BuildConfig
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pl.siatkalive.tlk.data.*
import pl.siatkalive.tlk.widget.TauronGlanceWidget
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    var openedMatchId by remember { mutableStateOf<String?>(null) }
    var favTeam by remember { mutableStateOf(prefs.getString("fav_team", "Chemik") ?: "Chemik") }

    var widgetState by remember { mutableStateOf<WidgetResponse?>(null) }
    var standings by remember { mutableStateOf<List<StandingRowDto>>(emptyList()) }
    var matches by remember { mutableStateOf<List<MatchDto>>(emptyList()) }
    var statsOverview by remember { mutableStateOf<StatsOverviewResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isOffline by remember { mutableStateOf(false) }
    var lastSyncTime by remember { mutableStateOf("--:--:--") }
    val scope = rememberCoroutineScope()

    suspend fun syncAll() {
        try {
            isOffline = false
            widgetState = ApiClient.api.getWidgetData(favTeam)
            matches = ApiClient.api.getMatches().matches
            standings = ApiClient.api.getStandings().standings
            if (statsOverview == null) {
                runCatching { statsOverview = ApiClient.api.getStatsOverview() }
            }
            lastSyncTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            TauronGlanceWidget().updateAll(context)
        } catch (e: Exception) {
            isOffline = true
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(favTeam) {
        while (true) {
            syncAll()
            val isAnyLive = (widgetState?.liveMatchesCount ?: 0) > 0 || (widgetState?.featuredMatch?.isLive == true)
            delay(if (isAnyLive) 10_000L else 30_000L)
        }
    }

    val isLiveMode = (widgetState?.liveMatchesCount ?: 0) > 0 || (widgetState?.featuredMatch?.isLive == true)

    val openedMatch = remember(openedMatchId, widgetState, matches) {
        if (openedMatchId == null) null
        else if (widgetState?.featuredMatch?.id == openedMatchId) widgetState?.featuredMatch
        else matches.find { it.id == openedMatchId } ?: widgetState?.featuredMatch
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (openedMatch != null) "⬅ Raport i Statystyki Meczu" else "🏐 Siatka Kobiet Live",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            modifier = Modifier.clickable(enabled = openedMatch != null) { openedMatchId = null }
                        )
                        Text(
                            text = if (isLiveMode) "● Auto-sync LIVE (10s) • Akt.: $lastSyncTime"
                                   else "Auto-sync (30s) • Akt.: $lastSyncTime",
                            color = if (isLiveMode) Color(0xFFEF4444) else Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                actions = {
                    if (openedMatch != null) {
                        TextButton(onClick = { openedMatchId = null }) {
                            Text("✕ Zamknij", color = Color(0xFFFACC15), fontWeight = FontWeight.Bold)
                        }
                    } else {
                        TextButton(onClick = {
                            scope.launch {
                                syncAll()
                                runCatching { statsOverview = ApiClient.api.getStatsOverview() }
                            }
                        }) {
                            Text("↻ Odśwież", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (openedMatch == null) {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("Mecze") },
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
                        label = { Text("Statystyki") },
                        icon = { Text("🏆") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        label = { Text("Mój Klub") },
                        icon = { Text("💙") }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
        ) {
            if (openedMatch != null) {
                BackHandler { openedMatchId = null }
                MatchCenterDetailScreen(
                    match = openedMatch,
                    standings = standings,
                    onBack = { openedMatchId = null }
                )
            } else {
                if (isOffline) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    ) {
                        Text(
                            "Brak połączenia z serwerem wyników. Sprawdź Wi-Fi / Internet.",
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(12.dp),
                            fontSize = 12.sp
                        )
                    }
                }

                if (isLoading && matches.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    when (selectedTab) {
                        0 -> MatchesTab(
                            featured = widgetState?.featuredMatch,
                            matches = matches,
                            favTeam = favTeam,
                            onOpenMatch = { matchId -> openedMatchId = matchId }
                        )
                        1 -> StandingsTab(standings, favTeam)
                        2 -> LeagueStatsTab(statsOverview, favTeam)
                        3 -> FavoriteTeamTab(
                            currentFav = favTeam,
                            standings = standings,
                            onSelectTeam = { newTeam ->
                                prefs.edit().putString("fav_team", newTeam).apply()
                                favTeam = newTeam
                                selectedTab = 0
                            }
                        )
                    }
                }
            }
        }
    }
}

fun detectCriticalPointAlert(match: MatchDto): Pair<String, String>? {
    val activeSet = match.sets.find { it.isLive } ?: match.sets.lastOrNull() ?: return null
    if (!match.isLive && activeSet.isComplete) return null
    val target = if (activeSet.setNumber == 5) 15 else 25
    val h = activeSet.homePoints
    val a = activeSet.awayPoints

    if (h >= target - 1 && h - a >= 1) {
        val isMatchPt = match.homeSets == 2
        val title = if (isMatchPt) "🏆 PIŁKA MECZOWA!" else "⚡ PIŁKA SETOWA (SET ${activeSet.setNumber})!"
        return title to "${match.homeTeam.name} ($h:$a)"
    }
    if (a >= target - 1 && a - h >= 1) {
        val isMatchPt = match.awaySets == 2
        val title = if (isMatchPt) "🏆 PIŁKA MECZOWA!" else "⚡ PIŁKA SETOWA (SET ${activeSet.setNumber})!"
        return title to "${match.awayTeam.name} ($h:$a)"
    }
    return null
}

@Composable
fun MatchCenterDetailScreen(
    match: MatchDto,
    standings: List<StandingRowDto>,
    onBack: () -> Unit
) {
    val alertInfo = detectCriticalPointAlert(match)
    val a = match.analytics
    val homeRank = standings.find { it.shortName == match.homeTeam.shortName }
    val awayRank = standings.find { it.shortName == match.awayTeam.shortName }

    var details by remember { mutableStateOf<MatchDetailsResponse?>(null) }
    var loadingDetails by remember { mutableStateOf(true) }

    LaunchedEffect(match.id, match.homeSets, match.awaySets) {
        loadingDetails = true
        runCatching {
            details = ApiClient.api.getMatchDetails(match.id)
        }
        loadingDetails = false
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        if (alertInfo != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                    border = BorderStroke(2.dp, Color(0xFFFACC15)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(alertInfo.first, color = Color(0xFFFACC15), fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(alertInfo.second, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        // KARTA GŁÓWNA WYNIKU
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(2.dp, if (match.isLive) Color(0xFFEF4444) else Color(0xFF38BDF8)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = if (match.isLive) "🔴 MECZ NA ŻYWO (SET ${match.currentSet ?: 1})" else match.dateLabel.uppercase(),
                            color = if (match.isLive) Color(0xFFEF4444) else Color(0xFF38BDF8),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                        Text(match.phase, color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(match.homeTeam.shortName, color = Color(0xFF38BDF8), fontWeight = FontWeight.Black, fontSize = 13.sp)
                                if (a.servingTeam == "HOME") {
                                    Text("  🏐 SERWUJE", color = Color(0xFFFACC15), fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                                }
                            }
                            Text(match.homeTeam.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            if (homeRank != null) {
                                Text("#${homeRank.position} w tabeli (${homeRank.points} pkt)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }

                        Surface(
                            color = Color(0xFF090D16),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = if (match.status == "PLANNED") "VS" else "${match.homeSets} : ${match.awaySets}",
                                color = Color(0xFFFACC15),
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                            )
                        }

                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (a.servingTeam == "AWAY") {
                                    Text("🏐 SERWUJE  ", color = Color(0xFFFACC15), fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                                }
                                Text(match.awayTeam.shortName, color = Color(0xFF38BDF8), fontWeight = FontWeight.Black, fontSize = 13.sp)
                            }
                            Text(match.awayTeam.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White, textAlign = TextAlign.End)
                            if (awayRank != null) {
                                Text("#${awayRank.position} w tabeli (${awayRank.points} pkt)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // 1. PRZEBIEG SETÓW + CZAS TRWANIA SETÓW Z PROTOKOŁU
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("1. PRZEBIEG I CZAS TRWANIA SETÓW", color = Color(0xFF38BDF8), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        if (details?.totalDuration != null && details?.totalDuration != "Brak danych") {
                            Text("⏱ Łącznie: ${details?.totalDuration}", color = Color(0xFFFACC15), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    if (match.sets.isEmpty() && (details?.setDurations.isNullOrEmpty())) {
                        Text(
                            "Mecz jeszcze się nie rozpoczął. Punktacja i czas poszczególnych setów pojawią się po pierwszej piłce.",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                    } else {
                        val durationsMap = details?.setDurations?.associateBy { it.setNumber } ?: emptyMap()

                        match.sets.forEach { s ->
                            val totalSetPts = (s.homePoints + s.awayPoints).coerceAtLeast(1)
                            val homeRatio = s.homePoints.toFloat() / totalSetPts.toFloat()
                            val setBg = if (s.isLive) Color(0xFF1E293B) else Color(0xFF090D16)
                            val durInfo = durationsMap[s.setNumber]?.duration

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(setBg)
                                    .padding(10.dp)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (s.isLive) "🔴 SET ${s.setNumber} (W TRAKCIE)" else "SET ${s.setNumber}",
                                            color = if (s.isLive) Color(0xFFEF4444) else Color(0xFF94A3B8),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        if (!durInfo.isNullOrBlank()) {
                                            Text(
                                                text = "  •  ⏱ $durInfo",
                                                color = Color(0xFFCBD5E1),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${s.homePoints} : ${s.awayPoints}",
                                        color = Color(0xFFFACC15),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                ) {
                                    Box(Modifier.weight(homeRatio.coerceAtLeast(0.05f)).fillMaxHeight().background(Color(0xFF38BDF8)))
                                    Box(Modifier.weight((1f - homeRatio).coerceAtLeast(0.05f)).fillMaxHeight().background(Color(0xFFFACC15)))
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFF1E293B))
                        val sumH = match.sets.sumOf { it.homePoints }
                        val sumA = match.sets.sumOf { it.awayPoints }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Suma małych punktów w meczu:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            Text("$sumH : $sumA", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // 2. STATYSTYKI SIATKARSKIE DRUŻYN (ATAK / BLOK / ZAGRYWKA / PRZYJĘCIE)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "2. STATYSTYKI SIATKARSKIE (ATAK / BLOK / ZAGRYWKA / PRZYJĘCIE)",
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )

                    val hs = details?.homeTeamStats
                    val asStat = details?.awayTeamStats

                    if (loadingDetails) {
                        Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    } else if (hs != null && asStat != null && (hs.matchesPlayed > 0 || asStat.matchesPlayed > 0)) {
                        Text(
                            "Porównanie oficjalnych statystyk zespołowych (${match.homeTeam.shortName} vs ${match.awayTeam.shortName}):",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )

                        StatBarRow(
                            label = "💥 Punkty atakiem (Skuteczność %)",
                            leftText = "${hs.attackPoints} pkt (${hs.attackPct})",
                            rightText = "${asStat.attackPoints} pkt (${asStat.attackPct})",
                            leftVal = hs.attackPoints.toFloat(),
                            rightVal = asStat.attackPoints.toFloat()
                        )

                        StatBarRow(
                            label = "🧱 Punkty blokiem (Średnia na set)",
                            leftText = "${hs.blockPoints} blk (${hs.blocksPerSet}/set)",
                            rightText = "${asStat.blockPoints} blk (${asStat.blocksPerSet}/set)",
                            leftVal = hs.blockPoints.toFloat(),
                            rightVal = asStat.blockPoints.toFloat()
                        )

                        StatBarRow(
                            label = "🎯 Asy serwisowe (Średnia na set)",
                            leftText = "${hs.serveAces} asów (${hs.acesPerSet}/set)",
                            rightText = "${asStat.serveAces} asów (${asStat.acesPerSet}/set)",
                            leftVal = hs.serveAces.toFloat(),
                            rightVal = asStat.serveAces.toFloat()
                        )

                        StatBarRow(
                            label = "🛡️ Przyjęcie pozytywne (Perfekcyjne %)",
                            leftText = "${hs.receptionPosPct} (perf. ${hs.receptionPerfPct})",
                            rightText = "${asStat.receptionPosPct} (perf. ${asStat.receptionPerfPct})",
                            leftVal = hs.receptionPosPct.replace("%", "").replace(",", ".").toFloatOrNull() ?: 50f,
                            rightVal = asStat.receptionPosPct.replace("%", "").replace(",", ".").toFloatOrNull() ?: 50f
                        )

                        StatBarRow(
                            label = "⚠️ Błędy własne w ataku i zagrywce",
                            leftText = "${hs.attackErrors + hs.serveErrors} bł. (${hs.serveErrors} zagr.)",
                            rightText = "${asStat.attackErrors + asStat.serveErrors} bł. (${asStat.serveErrors} zagr.)",
                            leftVal = (hs.attackErrors + hs.serveErrors).toFloat(),
                            rightVal = (asStat.attackErrors + asStat.serveErrors).toFloat()
                        )
                    } else {
                        Text(
                            "Szczegółowe statystyki ataku, bloku i przyjęcia pojawią się po zaktualizowaniu oficjalnego raportu ligowego.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // 3. ANALITYKA LIVE: SIDE-OUT vs BREAK POINT
        item {
            val effA = if ((details?.analytics?.homeSideOuts ?: 0) > 0 || (details?.analytics?.awaySideOuts ?: 0) > 0) {
                details!!.analytics!!
            } else {
                a
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("3. ANALITYKA LIVE: SIDE-OUT vs BREAK POINT", color = Color(0xFF38BDF8), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    Text(
                        "Side-out = punkt zdobyty po przyjęciu zagrywki rywalek. Break Point = punkt zdobyty przy własnej zagrywce (przełamanie).",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )

                    if (effA.homeSideOuts == 0 && effA.awaySideOuts == 0 && match.status == "PLANNED") {
                        Text(
                            "Analityka przejść (Side-out), przełamań (Break Point) i serii punktowych pojawi się automatycznie wraz z pierwszymi akcjami meczu.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    } else {
                        StatBarRow(
                            label = "🔥 Przełamania przy własnej zagrywce (Break Points)",
                            leftText = "${match.homeTeam.shortName}: ${effA.homeBreakPoints} pkt",
                            rightText = "${effA.awayBreakPoints} pkt :${match.awayTeam.shortName}",
                            leftVal = effA.homeBreakPoints.toFloat().coerceAtLeast(1f),
                            rightVal = effA.awayBreakPoints.toFloat().coerceAtLeast(1f)
                        )

                        StatBarRow(
                            label = "🔄 Przejścia po przyjęciu zagrywki (Side-outs)",
                            leftText = "${match.homeTeam.shortName}: ${effA.homeSideOuts} pkt",
                            rightText = "${effA.awaySideOuts} pkt :${match.awayTeam.shortName}",
                            leftVal = effA.homeSideOuts.toFloat().coerceAtLeast(1f),
                            rightVal = effA.awaySideOuts.toFloat().coerceAtLeast(1f)
                        )

                        StatBarRow(
                            label = "⚡ Najdłuższa seria punktowa z rzędu",
                            leftText = "${match.homeTeam.shortName}: ${effA.maxRunHome} pkt z rzędu",
                            rightText = "${effA.maxRunAway} pkt z rzędu :${match.awayTeam.shortName}",
                            leftVal = effA.maxRunHome.toFloat().coerceAtLeast(1f),
                            rightVal = effA.maxRunAway.toFloat().coerceAtLeast(1f)
                        )
                    }
                }
            }
        }

        // 4. OFICJALNA METRYCZKA PROTOKOŁU MECZOWEGO (HALA, WIDZOWIE, SĘDZIOWIE)
        if (details != null) {
            item {
                val d = details!!
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("4. PROTOKÓŁ MECZOWY (HALA, FREKWENCJA, SĘDZIOWIE)", color = Color(0xFF38BDF8), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)

                        ProtocolInfoRow("🏟️ Obiekt sportowy:", "${d.hallName}${if (d.hallCity.isNotBlank()) " (${d.hallCity})" else ""}")
                        if (d.hallAddress.isNotBlank()) {
                            ProtocolInfoRow("📍 Adres hali:", "${d.hallAddress}${if (d.hallCapacity.isNotBlank()) " • Pojemność: ${d.hallCapacity}" else ""}")
                        }
                        ProtocolInfoRow("👥 Liczba widzów:", d.attendance)
                        ProtocolInfoRow("⚖️ Sędzia pierwszy:", d.refereeFirst)
                        ProtocolInfoRow("⚖️️ Sędzia drugi:", d.refereeSecond)
                        if (d.commissioner.isNotBlank()) {
                            ProtocolInfoRow("📋 Komisarz PLS:", d.commissioner)
                        }
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("⬅ Wróć do pełnego terminarza")
            }
        }
    }
}

@Composable
fun ProtocolInfoRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF090D16))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFF94A3B8), fontSize = 12.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
    }
}

@Composable
fun StatBarRow(label: String, leftText: String, rightText: String, leftVal: Float, rightVal: Float) {
    val sum = (leftVal + rightVal).coerceAtLeast(1f)
    val leftRatio = (leftVal / sum).coerceIn(0.08f, 0.92f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF090D16))
            .padding(10.dp)
    ) {
        Text(label, color = Color(0xFFCBD5E1), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(leftText, color = Color(0xFF38BDF8), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            Text(rightText, color = Color(0xFFFACC15), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        ) {
            Box(Modifier.weight(leftRatio).fillMaxHeight().background(Color(0xFF38BDF8)))
            Box(Modifier.weight(1f - leftRatio).fillMaxHeight().background(Color(0xFFFACC15)))
        }
    }
}

@Composable
fun StatComparisonRow(label: String, homeVal: Int, awayVal: Int, homeShort: String, awayShort: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF090D16))
            .padding(10.dp)
    ) {
        Text(label, color = Color(0xFFCBD5E1), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$homeShort: $homeVal", color = Color(0xFF38BDF8), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            Text("$awayVal :$awayShort", color = Color(0xFFFACC15), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
    }
}

@Composable
fun LeagueStatsTab(overview: StatsOverviewResponse?, favTeam: String) {
    if (overview == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val rk = overview.playerRankings

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Text("🏆 Liderki Statystyk Zawodniczek", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.White)
            Text("Oficjalne rankingi indywidualne TAURON Ligi 2026/2027", color = Color(0xFF94A3B8), fontSize = 12.sp)
        }

        item { PlayerRankingCard("🔥 Najlepiej Punktujące", "Średnia pkt/set", rk.scorers) }
        item { PlayerRankingCard("🏆 Ranking Nagrody MVP", "Liczba statuetek", rk.mvp) }
        item { PlayerRankingCard("💥 Najlepiej Atakujące", "Efektywność Eff%", rk.attackers) }
        item { PlayerRankingCard("🧱 Najlepiej Blokujące", "Bloki/set", rk.blockers) }
        item { PlayerRankingCard("🎯 Najlepiej Zagrywające (Asy)", "Efektywność %", rk.servers) }
        item { PlayerRankingCard("🛡️ Najlepiej Przyjmujące (Poz%)", "Perfekcyjne %", rk.receivers) }
        item { PlayerRankingCard("🏐 Najlepiej Broniące", "Obrony/set", rk.defenders) }
    }
}

@Composable
fun PlayerRankingCard(title: String, subLabel: String, items: List<PlayerRankItemDto>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = Color(0xFFFACC15), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)

            if (items.isEmpty()) {
                Text("Brak danych w tej kategorii.", color = Color(0xFF94A3B8), fontSize = 12.sp)
            } else {
                items.forEach { p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF090D16))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${p.rank}.",
                                color = if (p.rank == 1) Color(0xFFFACC15) else Color(0xFF38BDF8),
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                modifier = Modifier.width(26.dp)
                            )
                            Column {
                                Text(p.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (p.teamName.isNotBlank()) {
                                    Text(
                                        text = p.teamName.lowercase(),
                                        color = Color(0xFF38BDF8),
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = if (p.sets > 0) "Mecze: ${p.matches} • Sety: ${p.sets}" else "Rozegrane mecze: ${p.matches}",
                                    color = Color(0xFF64748B),
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(p.value, color = Color(0xFFFACC15), fontWeight = FontWeight.Black, fontSize = 15.sp)
                            if (p.subValue.isNotBlank()) {
                                Text("$subLabel: ${p.subValue}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun extractMonthHeader(dateLabel: String): String {
    val lower = dateLabel.lowercase()
    return when {
        lower.contains(".09.") || lower.contains("wrz") -> "WRZESIEŃ 2026"
        lower.contains(".10.") || lower.contains("paź") || lower.contains("paz") -> "PAŹDZIERNIK 2026"
        lower.contains(".11.") || lower.contains("lis") -> "LISTOPAD 2026"
        lower.contains(".12.") || lower.contains("gru") -> "GRUDZIEŃ 2026"
        lower.contains(".01.") || lower.contains("sty") -> "STYCZEŃ 2027"
        lower.contains(".02.") || lower.contains("lut") -> "LUTY 2027"
        lower.contains(".03.") || lower.contains("mar") -> "MARZEC 2027"
        lower.contains(".04.") || lower.contains("kwi") -> "KWIECIEŃ 2027"
        lower.contains(".05.") || lower.contains("maj") -> "MAJ 2027"
        else -> "POZOSTAŁE TERMINY SEZONU"
    }
}

@Composable
fun MatchesTab(
    featured: MatchDto?,
    matches: List<MatchDto>,
    favTeam: String,
    onOpenMatch: (String) -> Unit
) {
    val groupedMatches = remember(matches) {
        matches.groupBy { extractMonthHeader(it.dateLabel) }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        if (featured != null) {
            item {
                val criticalAlert = detectCriticalPointAlert(featured)

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp, start = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (featured.isLive) "🔴 CENTRUM MECZOWE NA ŻYWO" else "🎯 CENTRUM MECZOWE (TWÓJ KLUB)",
                            color = if (featured.isLive) Color(0xFFEF4444) else Color(0xFF38BDF8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Dotknij po statystyki ➔",
                            color = Color(0xFFFACC15),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (criticalAlert != null) {
                        Surface(
                            color = Color(0xFF7F1D1D),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFFACC15)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "${criticalAlert.first} • ${criticalAlert.second}",
                                color = Color(0xFFFACC15),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    val borderColor = if (featured.isLive) Color(0xFFEF4444) else Color(0xFF0284C7)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = BorderStroke(2.dp, borderColor),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenMatch(featured.id) }
                    ) {
                        Column(
                            modifier = Modifier
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                    )
                                )
                                .padding(18.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = if (featured.isLive) Color(0xFFEF4444) else Color(0xFF0369A1),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (featured.isLive) "● TRWA SET ${featured.currentSet ?: 1}" else featured.dateLabel.uppercase(),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = "Pełny raport ➔",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(Modifier.height(14.dp))

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(featured.homeTeam.shortName, color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                        if (featured.analytics.servingTeam == "HOME") {
                                            Text(" 🏐", fontSize = 12.sp)
                                        }
                                    }
                                    Text(featured.homeTeam.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                }

                                Surface(
                                    color = Color(0xFF090D16),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                ) {
                                    Text(
                                        text = if (featured.status == "PLANNED") "VS" else "${featured.homeSets} : ${featured.awaySets}",
                                        color = Color(0xFFFACC15),
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }

                                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (featured.analytics.servingTeam == "AWAY") {
                                            Text("🏐 ", fontSize = 12.sp)
                                        }
                                        Text(featured.awayTeam.shortName, color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                    Text(featured.awayTeam.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White, textAlign = TextAlign.End)
                                }
                            }

                            if (featured.sets.isNotEmpty()) {
                                Spacer(Modifier.height(14.dp))
                                HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = featured.sets.joinToString("   •   ") { "Set ${it.setNumber}: ${it.homePoints}:${it.awayPoints}" },
                                        color = Color(0xFFF8FAFC),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = Color(0xFF334155), thickness = 2.dp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "CAŁY SEZON 2026/2027 (PODZIAŁ NA MIESIĄCE)",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        groupedMatches.forEach { (monthTitle, monthMatches) ->
            item(key = "header_$monthTitle") {
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "📅 $monthTitle",
                            color = Color(0xFFFACC15),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = "Spotkań: ${monthMatches.size}",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            items(monthMatches, key = { it.id }) { m ->
                val isFav = m.homeTeam.name.contains(favTeam, true) || m.awayTeam.name.contains(favTeam, true) ||
                            m.homeTeam.shortName.equals(favTeam, true) || m.awayTeam.shortName.equals(favTeam, true)
                val cardBg = if (isFav) Color(0xFF172554) else Color(0xFF0F172A)
                val cardBorder = if (isFav) BorderStroke(1.dp, Color(0xFF2563EB)) else BorderStroke(1.dp, Color(0xFF1E293B))

                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = cardBorder,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenMatch(m.id) }
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = if (m.isLive) "● LIVE (SET ${m.currentSet ?: 1})" else m.dateLabel,
                                color = if (m.isLive) Color(0xFFEF4444) else Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (m.sets.isNotEmpty()) m.sets.joinToString(" | ") { "${it.homePoints}:${it.awayPoints}" }
                                       else "Raport ➔",
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = m.homeTeam.name,
                                modifier = Modifier.weight(1f),
                                fontSize = 14.sp,
                                fontWeight = if (isFav) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = if (m.status == "PLANNED") "vs" else "${m.homeSets}:${m.awaySets}",
                                color = Color(0xFFFACC15),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
                            Text(
                                text = m.awayTeam.name,
                                modifier = Modifier.weight(1f),
                                fontSize = 14.sp,
                                fontWeight = if (isFav) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.End
                            )
                        }
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
            val isFav = r.name.contains(favTeam, true) || r.shortName.equals(favTeam, true)
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
fun FavoriteTeamTab(currentFav: String, standings: List<StandingRowDto>, onSelectTeam: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var appVersionInfo by remember { mutableStateOf<AppVersionResponse?>(null) }
    var checkingUpdate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        checkingUpdate = true
        runCatching {
            appVersionInfo = ApiClient.api.getAppVersion()
        }
        checkingUpdate = false
    }

    val defaultTeams = listOf(
        "LOTTO Chemik Police", "KS DevelopRes Rzeszów", "ŁKS Commercecon Łódź",
        "PGE Budowlani Łódź", "BKS ZGO Bielsko-Biała", "MOYA Radomka Radom",
        "UNI Opole", "#VolleyWrocław", "Metalkas Pałac Bydgoszcz",
        "ITA TOOLS STAL Mielec", "NETLAND MKS Kalisz", "Sokół & Hagric Mogilno"
    )
    val teamsList = if (standings.isNotEmpty()) standings.map { it.name } else defaultTeams

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        item {
            Text("Wybierz swój klub na Widżet", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Wybrana drużyna będzie wyróżniona w Centrum Meczowym, w tabeli oraz na widżecie ekranu głównego.",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp
            )
            Spacer(Modifier.height(8.dp))
        }
        items(teamsList) { teamName ->
            val isSelected = teamName.contains(currentFav, true)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF1E3A8A) else Color(0xFF0F172A)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectTeam(teamName) }
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(teamName, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, fontSize = 15.sp)
                    if (isSelected) {
                        Text("✓ Wybrano", color = Color(0xFFFACC15), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // SEKCJA USTAWIENIA I AKTUALIZACJE NA DOLE ZAKŁADKI MÓJ KLUB
        item {
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)
            Spacer(Modifier.height(14.dp))

            Text("⚙️ Ustawienia i Aktualizacje", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color.White)
            Spacer(Modifier.height(8.dp))

            val installedCode = BuildConfig.VERSION_CODE
            val installedName = BuildConfig.VERSION_NAME
            val remoteCode = appVersionInfo?.versionCode ?: installedCode
            val remoteName = appVersionInfo?.versionName ?: installedName
            val hasUpdate = remoteCode > installedCode

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (hasUpdate) Color(0xFF1E293B) else Color(0xFF0F172A)
                ),
                border = BorderStroke(
                    width = if (hasUpdate) 2.dp else 1.dp,
                    color = if (hasUpdate) Color(0xFFFACC15) else Color(0xFF1E293B)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Zainstalowana wersja:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text("v$installedName (build $installedCode)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Wersja na serwerze:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text(
                                text = if (checkingUpdate) "Sprawdzanie..." else "v$remoteName (build $remoteCode)",
                                color = if (hasUpdate) Color(0xFFFACC15) else Color(0xFF38BDF8),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    if (hasUpdate) {
                        Surface(
                            color = Color(0xFF422006),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFFACC15)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Text(
                                    text = "🚀 Dostępna jest nowa wersja v$remoteName!",
                                    color = Color(0xFFFACC15),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                                if (!appVersionInfo?.changelog.isNullOrBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = appVersionInfo?.changelog ?: "",
                                        color = Color(0xFFFEF08A),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "✅ Korzystasz z najnowszej wersji aplikacji.",
                            color = Color(0xFF4ADE80),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val apkUrl = appVersionInfo?.apkUrl ?: "https://zukowski87.duckdns.org/download/siatka-live.apk"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)))
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasUpdate) Color(0xFFFACC15) else Color(0xFF0284C7),
                                contentColor = if (hasUpdate) Color(0xFF090D16) else Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (hasUpdate) "⬇ Pobierz v$remoteName (.APK)" else "⬇ Pobierz plik .APK",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val portalUrl = appVersionInfo?.portalUrl ?: "https://zukowski87.duckdns.org/pobierz"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(portalUrl)))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🌐 Strona /pobierz", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(
                        onClick = {
                            scope.launch {
                                checkingUpdate = true
                                runCatching { appVersionInfo = ApiClient.api.getAppVersion() }
                                checkingUpdate = false
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("↻ Sprawdź dostępność aktualizacji", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
