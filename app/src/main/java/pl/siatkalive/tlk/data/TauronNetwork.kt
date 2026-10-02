package pl.siatkalive.tlk.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import pl.siatkalive.tlk.BuildConfig
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@Serializable
data class TeamColors(
    val primary: String = "#005BAC",
    val secondary: String = "#FFFFFF"
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
    val syncKey: String = "",
    val dateLabel: String = "",
    val phase: String = "",
    val homeTeam: TeamInfo,
    val awayTeam: TeamInfo,
    val status: String,
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
    val colors: TeamColors = TeamColors(),
    val points: Int,
    val matchesPlayed: Int,
    val matchesWon: Int,
    val matchesLost: Int,
    val setsWon: Int,
    val setsLost: Int,
    val smallPointsWon: Int = 0,
    val smallPointsLost: Int = 0
)

@Serializable
data class StandingsResponse(
    val updatedAt: String? = null,
    val count: Int = 0,
    val standings: List<StandingRowDto> = emptyList()
)

@Serializable
data class MatchesResponse(
    val updatedAt: String? = null,
    val hasActiveLiveMatch: Boolean = false,
    val count: Int = 0,
    val matches: List<MatchDto> = emptyList()
)

@Serializable
data class WidgetResponse(
    val updatedAt: String? = null,
    val recommendedRefreshSec: Int = 60,
    val featuredMatch: MatchDto? = null,
    val liveMatchesCount: Int = 0,
    val liveMatches: List<MatchDto> = emptyList()
)

interface TauronBffApi {
    @GET("api/v1/standings")
    suspend fun getStandings(): StandingsResponse

    @GET("api/v1/matches")
    suspend fun getMatches(@Query("team") team: String? = null): MatchesResponse

    @GET("api/v1/widget")
    suspend fun getWidgetData(@Query("team") team: String = "Chemik"): WidgetResponse
}

object ApiClient {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    val api: TauronBffApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BFF_BASE_URL)
            .client(okHttp)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TauronBffApi::class.java)
    }
}
