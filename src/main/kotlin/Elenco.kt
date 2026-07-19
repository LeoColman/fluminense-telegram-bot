package br.com.colman.bot

import okhttp3.Request
import java.io.IOException
import java.util.stream.Collectors

// Elenco vem do site oficial: a listagem tem nº+nome, e cada /jogador/{slug} tem a posição.
// Melhor que o TheSportsDB gratuito (que só devolvia ~10 jogadores).
private const val SQUAD_URL = "https://www.fluminense.com.br/o-time/futebol/profissional"
private const val PLAYER_URL_BASE = "https://www.fluminense.com.br/jogador/"

// TTL de 6h: elenco muda pouco (janela de transferência), cache longo evita 32 requests por comando.
private const val ELENCO_CACHE_TTL_MS = 6 * 60 * 60 * 1000L

private const val BROWSER_UA = "Mozilla/5.0"

// Modelo interno usado por formatElenco (independente do formato do site).
data class Player(val name: String, val position: String, val number: Int?)

// Um jogador da listagem, antes de buscar a posição na página dele.
data class RawPlayer(val slug: String, val name: String, val number: Int?)

private val elencoCacheLock = Any()
private var cachedSquad: List<Player>? = null
private var elencoCachedAtMillis: Long = 0L

/**
 * Elenco profissional do Fluminense, raspado do site oficial.
 * Retorna lista vazia quando não há dados. Lança IOException em erro de rede (na listagem).
 * Posição de cada jogador é buscada em paralelo; se a página do jogador falhar, posição vira "?".
 * Usa cache em memória (TTL 6h).
 */
fun squad(): List<Player> = synchronized(elencoCacheLock) {
    val now = System.currentTimeMillis()
    if (now - elencoCachedAtMillis < ELENCO_CACHE_TTL_MS && elencoCachedAtMillis != 0L) {
        return cachedSquad ?: emptyList()
    }

    val raw = parseSquadListing(fetchHtml(SQUAD_URL))
    val squad = raw.parallelStream().map { rp ->
        val position = try {
            parsePosition(fetchHtml(PLAYER_URL_BASE + rp.slug))
        } catch (e: Exception) {
            "?"
        }
        Player(name = rp.name, position = position, number = rp.number)
    }.collect(Collectors.toList())

    cachedSquad = squad
    elencoCachedAtMillis = now
    squad
}

private fun fetchHtml(url: String): String {
    val request = Request.Builder().url(url).header("User-Agent", BROWSER_UA).build()
    // sportsDbHttp é um OkHttpClient genérico compartilhado (nome legado do primeiro uso).
    sportsDbHttp.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("fluminense.com.br retornou HTTP ${response.code}")
        return response.body?.string() ?: ""
    }
}

private val listingRegex =
    Regex("<a href=\"/jogador/([^\"]+)\">\\s*<div class=\"player-name text-center\">([^<]+)</div>")
private val positionRegex = Regex("class=\"[^\"]*position[^\"]*\">\\s*([^<]+)")

/**
 * Extrai (slug, nome, número) da página de listagem do elenco.
 * Cada item vem como `<a href="/jogador/{slug}"><div class="player-name ...">{nº} - {NOME}</div>`.
 * Função pura para ser testável.
 */
fun parseSquadListing(html: String): List<RawPlayer> = listingRegex.findAll(html).map { match ->
    val slug = match.groupValues[1]
    val texto = match.groupValues[2].trim()
    val partes = texto.split(" - ", limit = 2)
    val numero = partes[0].trim().toIntOrNull()
    val nome = if (partes.size == 2 && numero != null) partes[1].trim() else texto
    RawPlayer(slug, nome, numero)
}.toList()

/**
 * Extrai a posição da página de um jogador (`class="...position">Volante`). "?" se não achar.
 * Função pura para ser testável.
 */
fun parsePosition(html: String): String =
    positionRegex.find(html)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() } ?: "?"

/**
 * Formata o elenco em PT-BR, ordenado por número de camisa, ex.:
 * "👥 Elenco do Fluminense\n\n#1 FÁBIO (Goleiro)\n...".
 * Função pura para ser testável.
 */
fun formatElenco(players: List<Player>): String {
    if (players.isEmpty()) return "Elenco indisponível no momento 😕"

    val linhas = players
        .sortedWith(compareBy(nullsLast()) { it.number })
        .joinToString("\n") { player ->
            val numero = player.number?.let { "#$it " } ?: ""
            val posicao = player.position.takeIf { it.isNotBlank() && it != "?" }?.let { " ($it)" } ?: ""
            "$numero${player.name}$posicao"
        }

    return "👥 Elenco do Fluminense\n\n$linhas"
}
