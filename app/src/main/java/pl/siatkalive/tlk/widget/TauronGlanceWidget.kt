package pl.siatkalive.tlk.widget

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import pl.siatkalive.tlk.data.ApiClient
import pl.siatkalive.tlk.data.MatchDto

class TauronWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TauronGlanceWidget()
}

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        TauronGlanceWidget().update(context, glanceId)
    }
}

class TauronGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("siatka_prefs", Context.MODE_PRIVATE)
        val favTeam = prefs.getString("fav_team", "Chemik") ?: "Chemik"
        val customUrl = prefs.getString("bff_url", null)

        val widgetData = try {
            ApiClient.getApi(customUrl).getWidgetData(favTeam)
        } catch (e: Exception) {
            null
        }

        provideContent {
            GlanceTheme {
                WidgetCard(match = widgetData?.featuredMatch)
            }
        }
    }

    @Composable
    private fun WidgetCard(match: MatchDto?) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF0F172A)))
                .cornerRadius(16.dp)
                .padding(12.dp)
                .clickable(actionRunCallback<RefreshWidgetAction>()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (match == null) {
                Text(
                    text = "Siatka Live – Dotknij, aby połączyć z serwerem",
                    style = TextStyle(color = ColorProvider(Color.White), fontSize = 13.sp)
                )
                return@Column
            }

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val badgeText = if (match.isLive) {
                    "● NA ŻYWO (SET ${match.currentSet ?: 1})"
                } else {
                    match.dateLabel.uppercase()
                }
                val badgeColor = if (match.isLive) Color(0xFFEF4444) else Color(0xFF38BDF8)

                Text(
                    text = badgeText,
                    style = TextStyle(
                        color = ColorProvider(badgeColor),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "↻ Odśwież",
                    style = TextStyle(color = ColorProvider(Color(0xFF94A3B8)), fontSize = 11.sp)
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TeamBadge(match.homeTeam.shortName, match.homeTeam.colors.primary)
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = match.homeTeam.shortName,
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                val centerScore = if (match.status == "PLANNED") "VS" else "${match.homeSets} : ${match.awaySets}"
                Text(
                    text = centerScore,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFFACC15)),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                Text(
                    text = match.awayTeam.shortName,
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                TeamBadge(match.awayTeam.shortName, match.awayTeam.colors.primary)
            }

            if (match.sets.isNotEmpty()) {
                Spacer(modifier = GlanceModifier.height(8.dp))
                val setsString = match.sets.joinToString("  |  ") { s ->
                    if (s.isLive) "[${s.homePoints}:${s.awayPoints}*]" else "${s.homePoints}:${s.awayPoints}"
                }
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = setsString,
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFCBD5E1)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }

    @Composable
    private fun TeamBadge(shortName: String, hexColor: String) {
        val parsedColor = try {
            Color(AndroidColor.parseColor(hexColor))
        } catch (e: Exception) {
            Color(0xFF005BAC)
        }
        Box(
            modifier = GlanceModifier
                .size(28.dp)
                .background(ColorProvider(parsedColor))
                .cornerRadius(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = shortName.take(3),
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
