package br.com.colman.bot

import okhttp3.Request
import java.io.IOException
import java.time.ZonedDateTime

// TTL de 30min: a tabela só muda em rodada, cache evita bater na API a cada /tabela.
private const val TABELA_CACHE_TTL_MS = 30 * 60 * 1000L

// Modelo interno usado por formatTabela (independente do formato da API).
data class TableRow(
    val rank: Int,
    val team: String,
    val played: Int,
    val goalDiff: Int,
    val points: Int,
)

// Mapeamento da resposta do TheSportsDB (só os campos que usamos).
private data class TableResponse(val table: List<StandingRow>? = null)
private data class StandingRow(
    val intRank: String?,
    val strTeam: String?,
    val intPlayed: String?,
    val intGoalDifference: String?,
    val intPoints: String?,
)

private val tabelaCacheLock = Any()
private var cachedTable: List<TableRow>? = null
private var tabelaCachedAtMillis: Long = 0L
private var cachedTableSeason: String = ""

/**
 * Classificação do Brasileirão Série A na temporada [season] (ex.: "2026"),
 * via endpoint `lookuptable.php`. Retorna lista vazia quando não há tabela.
 * Lança IOException em erro de rede/API. Usa cache em memória (TTL 30min).
 */
fun leagueTable(apiKey: String, season: String): List<TableRow> = synchronized(tabelaCacheLock) {
    val now = System.currentTimeMillis()
    if (season == cachedTableSeason && now - tabelaCachedAtMillis < TABELA_CACHE_TTL_MS && tabelaCachedAtMillis != 0L) {
        return cachedTable ?: emptyList()
    }

    val table = fetchLeagueTable(apiKey, season)
    cachedTable = table
    cachedTableSeason = season
    tabelaCachedAtMillis = now
    table
}

private fun fetchLeagueTable(apiKey: String, season: String): List<TableRow> {
    val url = "https://www.thesportsdb.com/api/v1/json/$apiKey/lookuptable.php?l=$BRASILEIRAO_ID&s=$season"
    val request = Request.Builder().url(url).build()

    sportsDbHttp.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("TheSportsDB retornou HTTP ${response.code}")
        val body = response.body?.string() ?: return emptyList()

        val parsed = sportsDbGson.fromJson(body, TableResponse::class.java)
        return parsed.table.orEmpty().mapNotNull { row ->
            val rank = row.intRank?.toIntOrNull() ?: return@mapNotNull null
            TableRow(
                rank = rank,
                team = row.strTeam ?: "?",
                played = row.intPlayed?.toIntOrNull() ?: 0,
                goalDiff = row.intGoalDifference?.toIntOrNull() ?: 0,
                points = row.intPoints?.toIntOrNull() ?: 0,
            )
        }
    }
}

/**
 * Formata a tabela completa em PT-BR, destacando o Fluminense com 🇮🇹, ex.:
 * "📊 Brasileirão\n\n 1 Palmeiras — 41 pts (18j, +17)\n...".
 * Função pura para ser testável.
 */
fun formatTabela(rows: List<TableRow>): String {
    if (rows.isEmpty()) return "Tabela indisponível no momento 😕"

    val linhas = rows.joinToString("\n") { row ->
        val destaque = if (row.team.contains("Fluminense")) " 🇮🇹" else ""
        val saldo = if (row.goalDiff >= 0) "+${row.goalDiff}" else "${row.goalDiff}"
        "${row.rank.toString().padStart(2)} ${row.team} — ${row.points} pts (${row.played}j, $saldo)$destaque"
    }

    return "📊 Brasileirão\n\n$linhas"
}
