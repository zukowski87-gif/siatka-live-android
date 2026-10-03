package pl.siatkalive.tlk.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import pl.siatkalive.tlk.BuildConfig
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@Serializable
data class TeamColors(
    val primary: String = "#005BAC",
    val secondary: String = "#FACC15"
)

@Serializable
data class TeamInfo(
    val name: String,
    val shortName: String,
    val colors: TeamColors = TeamColors()
)

@Serializable
data class SetScore(
    val setNumber: Int,
    val homePoints: Int,
    val awayPoints: Int,
    val isLive: Boolean = false,
    val isComplete: Boolean = false
)

@Serializable
data class MatchAnalytics(
    val servingTeam: String? = null,
    val currentRunTeam: String? = null,
    val currentRunCount: Int = 0,
    val maxRunHome: Int = 0,
    val maxRunAway: Int = 0,
    val homeSideOuts: Int = 0,
    val awaySideOuts: Int = 0,
    val homeBreakPoints: Int = 0,
    val awayBreakPoints: Int = 0,
    val totalHomePoints: Int = 0,
    val totalAwayPoints: Int = 0,
    val alertType: String? = null,
    val alertTeam: String? = null
)

@Serializable
data class MatchDto(
    val id: String,
    val round: Int = 0,
    val dateLabel: String = "",
    val phase: String = "",
    val homeTeam: TeamInfo,
    val awayTeam: TeamInfo,
    val status: String = "PLANNED",
    val isLive: Boolean = false,
    val currentSet: Int? = null,
    val homeSets: Int = 0,
    val awaySets: Int = 0,
    val sets: List<SetScore> = emptyList(),
    val analytics: MatchAnalytics = MatchAnalytics()
)

@Serializable
data class StandingRowDto(
    val position: Int,
    val teamId: Int? = null,
    val name: String,
    val shortName: String,
    val matchesPlayed: Int = 0,
    val points: Int = 0,
    val matchesWon: Int = 0,
    val matchesLost: Int = 0,
    val setsWon: Int = 0,
    val setsLost: Int = 0
)

@Serializable
data class WidgetResponse(
    val updatedAt: String = "",
    val liveMatchesCount: Int = 0,
    val featuredMatch: MatchDto? = null,
    val topStandings: List<StandingRowDto> = emptyList()
)

@Serializable
data class MatchesResponse(
    val updatedAt: String = "",
    val count: Int = 0,
    val matches: List<MatchDto> = emptyList()
)

@Serializable
data class StandingsResponse(
    val updatedAt: String = "",
    val standings: List<StandingRowDto> = emptyList()
)

@Serializable
data class SetDurationDto(
    val setNumber: Int = 0,
    val duration: String = "",
    val score: String = "",
    val matchScoreAfter: String = ""
)

@Serializable
data class TeamDetailedStatsDto(
    val teamName: String = "",
    val shortName: String = "",
    val matchesPlayed: Int = 0,
    val setsPlayed: Int = 0,
    val totalPoints: Int = 0,
    val serveTotal: Int = 0,
    val serveAces: Int = 0,
    val serveErrors: Int = 0,
    val acesPerSet: String = "0,00",
    val receptionTotal: Int = 0,
    val receptionErrors: Int = 0,
    val receptionPosPct: String = "0%",
    val receptionPerfPct: String = "0%",
    val attackTotal: Int = 0,
    val attackErrors: Int = 0,
    val attackBlocked: Int = 0,
    val attackPoints: Int = 0,
    val attackPct: String = "0%",
    val blockPoints: Int = 0,
    val blocksPerSet: String = "0,00"
)

@Serializable
data class PlayerRankItemDto(
    val rank: Int = 0,
    val name: String = "",
    val teamName: String = "",
    val matches: Int = 0,
    val sets: Int = 0,
    val value: String = "",
    val subValue: String = ""
)

@Serializable
data class PlayerRankingsDto(
    val mvp: List<PlayerRankItemDto> = emptyList(),
    val scorers: List<PlayerRankItemDto> = emptyList(),
    val attackers: List<PlayerRankItemDto> = emptyList(),
    val blockers: List<PlayerRankItemDto> = emptyList(),
    val servers: List<PlayerRankItemDto> = emptyList(),
    val receivers: List<PlayerRankItemDto> = emptyList(),
    val defenders: List<PlayerRankItemDto> = emptyList()
)

@Serializable
data class MatchDetailsResponse(
    val matchId: String = "",
    val totalDuration: String = "Brak danych",
    val totalPointsRatio: String = "",
    val attendance: String = "Brak danych",
    val matchNumber: String = "",
    val roundNumber: String = "",
    val refereeFirst: String = "Brak danych",
    val refereeSecond: String = "Brak danych",
    val commissioner: String = "",
    val hallName: String = "Brak danych",
    val hallAddress: String = "",
    val hallCity: String = "",
    val hallCapacity: String = "",
    val setDurations: List<SetDurationDto> = emptyList(),
    val analytics: MatchAnalytics? = null,
    val homeTeamStats: TeamDetailedStatsDto? = null,
    val awayTeamStats: TeamDetailedStatsDto? = null,
    val playerRankings: PlayerRankingsDto = PlayerRankingsDto()
)

@Serializable
data class StatsOverviewResponse(
    val teamStats: List<TeamDetailedStatsDto> = emptyList(),
    val playerRankings: PlayerRankingsDto = PlayerRankingsDto()
)

@Serializable
data class AppVersionResponse(
    val versionCode: Int = 1,
    val versionName: String = "1.0.0",
    val apkUrl: String = "https://zukowski87.duckdns.org/download/siatka-live.apk",
    val portalUrl: String = "https://zukowski87.duckdns.org/pobierz",
    val sha256: String = "",
    val fileSizeBytes: Long = 0L,
    val legalNotice: String = "",
    val changelog: String = ""
)

@Serializable
data class CoachDto(
    val role: String = "",
    val name: String = ""
)

@Serializable
data class RosterPlayerDto(
    val playerId: Int = 0,
    val number: String = "",
    val name: String = "",
    val position: String = "",
    val matches: String = "0",
    val sets: String = "0",
    val totalPoints: String = "0",
    val attackPct: String = "-"
)

@Serializable
data class PlayerProfileResponse(
    val playerId: Int = 0,
    val height: String = "Brak danych",
    val attackReach: String = "Brak danych",
    val blockReach: String = "Brak danych",
    val birthDate: String = "Brak danych"
)

@Serializable
data class TeamProfileResponse(
    val teamId: Int = 0,
    val teamName: String = "",
    val hallName: String = "",
    val hallAddress: String = "",
    val website: String = "",
    val coaches: List<CoachDto> = emptyList(),
    val roster: List<RosterPlayerDto> = emptyList()
)

interface TauronApi {
    @GET("api/v1/widget")
    suspend fun getWidgetData(@Query("team") team: String? = null): WidgetResponse

    @GET("api/v1/matches")
    suspend fun getMatches(@Query("team") team: String? = null): MatchesResponse

    @GET("api/v1/standings")
    suspend fun getStandings(): StandingsResponse

    @GET("api/v1/matches/{id}/details")
    suspend fun getMatchDetails(@Path("id") matchId: String): MatchDetailsResponse

    @GET("api/v1/stats/overview")
    suspend fun getStatsOverview(): StatsOverviewResponse

    @GET("api/v1/app/version")
    suspend fun getAppVersion(): AppVersionResponse

    @GET("api/v1/teams/{id}/details")
    suspend fun getTeamDetails(@Path("id") id: Int): TeamProfileResponse

    @GET("api/v1/players/{id}/details")
    suspend fun getPlayerDetails(@Path("id") id: Int): PlayerProfileResponse
}

object ApiClient {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    val api: TauronApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BFF_BASE_URL)
            .client(okHttp)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TauronApi::class.java)
    }
}
