package br.com.colman.bot

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.DayOfWeek
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// TheSportsDB team id do Fluminense. Confirmado via searchteams.php?t=Fluminense.
const val FLUMINENSE_ID = 134296

// TheSportsDB free key ("123") também é limitado; cache de 30min evita bater na API a cada /quando.
// A parte "daqui Xh" segue recalculada por request (formatQuando recebe o `now` fresco).
private const val CACHE_TTL_MS = 30 * 60 * 1000L

private val zone: ZoneId = ZoneId.of("America/Sao_Paulo")
private val httpClient = OkHttpClient()
private val gson = Gson()

private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dayMonthFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")

private val diasDaSemana = mapOf(
    DayOfWeek.MONDAY to "Segunda",
    DayOfWeek.TUESDAY to "Terça",
    DayOfWeek.WEDNESDAY to "Quarta",
    DayOfWeek.THURSDAY to "Quinta",
    DayOfWeek.FRIDAY to "Sexta",
    DayOfWeek.SATURDAY to "Sábado",
    DayOfWeek.SUNDAY to "Domingo",
)

// Nomes de liga em PT-BR (TheSportsDB devolve em inglês). Nomes não mapeados ficam como vêm.
private val ligaPtBr = mapOf(
    "Brazilian Serie A" to "Brasileirão",
)

// Modelo interno usado por formatQuando (independente do formato da API).
data class Match(val startsAt: String, val competition: Competition, val homeTeam: Team, val awayTeam: Team)
data class Competition(val name: String)
data class Team(val name: String)

// Mapeamento da resposta do TheSportsDB (só os campos que usamos).
private data class EventsResponse(val events: List<Event>? = null)
private data class Event(
    val strTimestamp: String?,
    val dateEvent: String?,
    val strTime: String?,
    val strLeague: String?,
    val strHomeTeam: String?,
    val strAwayTeam: String?,
)

private val cacheLock = Any()
private var cachedMatch: Match? = null
private var cachedAtMillis: Long = 0L

/**
 * Próximo jogo do Fluminense em qualquer competição que o TheSportsDB acompanhar,
 * via endpoint `eventsnext.php`. Retorna null quando não há jogo agendado.
 * Lança IOException em erro de rede/API. Usa cache em memória (TTL 30min).
 */
fun nextMatch(apiKey: String): Match? = synchronized(cacheLock) {
    val now = System.currentTimeMillis()
    if (now - cachedAtMillis < CACHE_TTL_MS && cachedAtMillis != 0L) return cachedMatch

    val match = fetchNextMatch(apiKey)
    cachedMatch = match
    cachedAtMillis = now
    match
}

private fun fetchNextMatch(apiKey: String): Match? {
    val url = "https://www.thesportsdb.com/api/v1/json/$apiKey/eventsnext.php?id=$FLUMINENSE_ID"
    val request = Request.Builder().url(url).build()

    httpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("TheSportsDB retornou HTTP ${response.code}")
        val body = response.body?.string() ?: return null

        val parsed = gson.fromJson(body, EventsResponse::class.java)
        val event = parsed.events
            ?.filter { utcIso(it) != null }
            ?.minByOrNull { utcIso(it)!! }
            ?: return null

        return Match(
            startsAt = utcIso(event)!!,
            competition = Competition(prettyLeague(event.strLeague)),
            homeTeam = Team(event.strHomeTeam ?: "?"),
            awayTeam = Team(event.strAwayTeam ?: "?"),
        )
    }
}

// TheSportsDB devolve horário em UTC sem offset ("2026-07-26T21:30:00"); acrescenta o Z.
private fun utcIso(event: Event): String? {
    val ts = event.strTimestamp?.takeIf { it.isNotBlank() }
    if (ts != null) return if (ts.endsWith("Z")) ts else "${ts}Z"
    val date = event.dateEvent?.takeIf { it.isNotBlank() } ?: return null
    val time = event.strTime?.takeIf { it.isNotBlank() } ?: return null
    return "${date}T${time}Z"
}

private fun prettyLeague(name: String?): String = name?.let { ligaPtBr[it] ?: it } ?: "Jogo"

/**
 * Formata a partida em PT-BR relativo ao momento [now], ex.:
 * "Hoje daqui 2h às 13:00 vs Grêmio (Brasileirão)".
 * Função pura (sem relógio interno) para ser testável.
 */
fun formatQuando(match: Match, now: ZonedDateTime): String {
    val matchZdt = OffsetDateTime.parse(match.startsAt).toInstant().atZone(zone)
    val hora = "às " + matchZdt.format(timeFormat)
    val opponent = if (match.homeTeam.name.contains("Fluminense")) match.awayTeam.name else match.homeTeam.name
    val dias = ChronoUnit.DAYS.between(now.toLocalDate(), matchZdt.toLocalDate())

    val prefix = when {
        dias == 0L -> "Hoje daqui ${relativa(Duration.between(now, matchZdt))} $hora"
        dias == 1L -> "Amanhã $hora"
        dias in 2..6 -> "${diasDaSemana.getValue(matchZdt.dayOfWeek)} ${matchZdt.format(dayMonthFormat)} $hora"
        else -> "${matchZdt.format(dayMonthFormat)} (daqui $dias dias) $hora"
    }

    return "$prefix vs $opponent (${match.competition.name})"
}

private fun relativa(duration: Duration): String {
    val totalMin = duration.toMinutes().coerceAtLeast(0)
    val h = totalMin / 60
    val m = totalMin % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}min"
        h > 0 -> "${h}h"
        else -> "${m}min"
    }
}
