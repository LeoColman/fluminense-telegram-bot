package br.com.colman.bot

import okhttp3.Request
import java.io.IOException

// TTL de 6h: elenco muda pouco (janela de transferência), cache longo evita bater na API.
private const val ELENCO_CACHE_TTL_MS = 6 * 60 * 60 * 1000L

// Modelo interno usado por formatElenco (independente do formato da API).
data class Player(val name: String, val position: String, val number: Int?)

// Mapeamento da resposta do TheSportsDB (só os campos que usamos).
private data class PlayersResponse(val player: List<PlayerEntry>? = null)
private data class PlayerEntry(
    val strPlayer: String?,
    val strPosition: String?,
    val strNumber: String?,
)

private val elencoCacheLock = Any()
private var cachedSquad: List<Player>? = null
private var elencoCachedAtMillis: Long = 0L

/**
 * Elenco do Fluminense via endpoint `lookup_all_players.php`.
 * ATENÇÃO: a chave gratuita ("123") devolve só uma amostra do elenco (~10 jogadores).
 * Retorna lista vazia quando não há dados. Lança IOException em erro de rede/API.
 * Usa cache em memória (TTL 6h).
 */
fun squad(apiKey: String): List<Player> = synchronized(elencoCacheLock) {
    val now = System.currentTimeMillis()
    if (now - elencoCachedAtMillis < ELENCO_CACHE_TTL_MS && elencoCachedAtMillis != 0L) {
        return cachedSquad ?: emptyList()
    }

    val squad = fetchSquad(apiKey)
    cachedSquad = squad
    elencoCachedAtMillis = now
    squad
}

private fun fetchSquad(apiKey: String): List<Player> {
    val url = "https://www.thesportsdb.com/api/v1/json/$apiKey/lookup_all_players.php?id=$FLUMINENSE_ID"
    val request = Request.Builder().url(url).build()

    sportsDbHttp.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("TheSportsDB retornou HTTP ${response.code}")
        val body = response.body?.string() ?: return emptyList()

        val parsed = sportsDbGson.fromJson(body, PlayersResponse::class.java)
        return parsed.player.orEmpty().mapNotNull { entry ->
            val name = entry.strPlayer?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            Player(
                name = name,
                position = entry.strPosition?.takeIf { it.isNotBlank() } ?: "?",
                number = entry.strNumber?.toIntOrNull(),
            )
        }
    }
}

/**
 * Formata o elenco em PT-BR, ordenado por número de camisa, ex.:
 * "👥 Elenco do Fluminense\n\n#17 Agustín Canobbio (Right Winger)\n...".
 * Função pura para ser testável.
 */
fun formatElenco(players: List<Player>): String {
    if (players.isEmpty()) return "Elenco indisponível no momento 😕"

    val linhas = players
        .sortedWith(compareBy(nullsLast()) { it.number })
        .joinToString("\n") { player ->
            val numero = player.number?.let { "#$it " } ?: ""
            "$numero${player.name} (${player.position})"
        }

    // Aviso sobre limite da API gratuita.
    return "👥 Elenco do Fluminense (parcial)\n\n$linhas"
}
