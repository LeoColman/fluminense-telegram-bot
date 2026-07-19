package br.com.colman.bot

import okhttp3.Request
import java.io.IOException
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// TTL de 30min: resultado só muda depois de um jogo, não precisa bater na API a cada /resultado.
private const val RESULTADO_CACHE_TTL_MS = 30 * 60 * 1000L

private val resultadoDayMonthFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")

// Modelo interno usado por formatResultado (independente do formato da API).
data class MatchResult(
    val playedAt: String,
    val competition: Competition,
    val homeTeam: Team,
    val awayTeam: Team,
    val homeScore: Int,
    val awayScore: Int,
)

// Mapeamento da resposta do TheSportsDB (só os campos que usamos).
private data class ResultsResponse(val results: List<ResultEvent>? = null)
private data class ResultEvent(
    val strTimestamp: String?,
    val dateEvent: String?,
    val strTime: String?,
    val strLeague: String?,
    val strHomeTeam: String?,
    val strAwayTeam: String?,
    val intHomeScore: String?,
    val intAwayScore: String?,
)

private val resultadoCacheLock = Any()
private var cachedResult: MatchResult? = null
private var resultadoCachedAtMillis: Long = 0L

/**
 * Último jogo do Fluminense com placar, via endpoint `eventslast.php`.
 * Retorna null quando não há resultado disponível. Lança IOException em erro de rede/API.
 * Usa cache em memória (TTL 30min).
 */
fun lastResult(apiKey: String): MatchResult? = synchronized(resultadoCacheLock) {
    val now = System.currentTimeMillis()
    if (now - resultadoCachedAtMillis < RESULTADO_CACHE_TTL_MS && resultadoCachedAtMillis != 0L) return cachedResult

    val result = fetchLastResult(apiKey)
    cachedResult = result
    resultadoCachedAtMillis = now
    result
}

private fun fetchLastResult(apiKey: String): MatchResult? {
    val url = "https://www.thesportsdb.com/api/v1/json/$apiKey/eventslast.php?id=$FLUMINENSE_ID"
    val request = Request.Builder().url(url).build()

    sportsDbHttp.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("TheSportsDB retornou HTTP ${response.code}")
        val body = response.body?.string() ?: return null

        val parsed = sportsDbGson.fromJson(body, ResultsResponse::class.java)
        val event = parsed.results
            ?.filter { resultadoUtcIso(it) != null && it.intHomeScore != null && it.intAwayScore != null }
            ?.maxByOrNull { resultadoUtcIso(it)!! }
            ?: return null

        return MatchResult(
            playedAt = resultadoUtcIso(event)!!,
            competition = Competition(prettyLiga(event.strLeague)),
            homeTeam = Team(event.strHomeTeam ?: "?"),
            awayTeam = Team(event.strAwayTeam ?: "?"),
            homeScore = event.intHomeScore!!.toInt(),
            awayScore = event.intAwayScore!!.toInt(),
        )
    }
}

// TheSportsDB devolve horário em UTC sem offset ("2026-07-17T23:00:00"); acrescenta o Z.
private fun resultadoUtcIso(event: ResultEvent): String? {
    val ts = event.strTimestamp?.takeIf { it.isNotBlank() }
    if (ts != null) return if (ts.endsWith("Z")) ts else "${ts}Z"
    val date = event.dateEvent?.takeIf { it.isNotBlank() } ?: return null
    val time = event.strTime?.takeIf { it.isNotBlank() } ?: return null
    return "${date}T${time}Z"
}

/**
 * Formata o resultado em PT-BR relativo ao momento [now], ex.:
 * "Fluminense 1 x 1 Bragantino (Brasileirão) — ontem 17/07".
 * Função pura (sem relógio interno) para ser testável.
 */
fun formatResultado(result: MatchResult, now: ZonedDateTime): String {
    val matchZdt = OffsetDateTime.parse(result.playedAt).toInstant().atZone(sportsDbZone)
    val placar = "${result.homeTeam.name} ${result.homeScore} x ${result.awayScore} ${result.awayTeam.name}"
    val dias = ChronoUnit.DAYS.between(matchZdt.toLocalDate(), now.toLocalDate())

    val quando = when {
        dias <= 0L -> "hoje"
        dias == 1L -> "ontem"
        else -> "há $dias dias"
    }

    return "$placar (${result.competition.name}) — $quando ${matchZdt.format(resultadoDayMonthFormat)}"
}
