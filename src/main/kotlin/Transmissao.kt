package br.com.colman.bot

import okhttp3.Request
import java.io.IOException
import java.time.LocalDate
import java.time.OffsetDateTime

// A agenda do ge é a fonte de canal BR (Premiere, SporTV, Globo...). O subcanal numerado
// ("Premiere 4") NÃO existe no dado gratuito: só a emissora. A agenda server-render cobre
// só uma janela de ~hoje±2 dias, então jogo distante costuma não ter canal ainda.
private const val AGENDA_URL = "https://ge.globo.com/agenda/"

// TTL de 30min: canal muda pouco no dia; evita raspar o ge a cada /quando.
private const val TRANSMISSAO_CACHE_TTL_MS = 30 * 60 * 1000L

// A agenda vem como SPA; forçamos um User-Agent de browser pra receber o HTML com o JSON embutido.
private const val BROWSER_UA = "Mozilla/5.0"

// mediaType do JSON do ge -> nome amigável. Não mapeados viram capitalizados.
private val canalPtBr = mapOf(
    "premiere" to "Premiere",
    "sportv" to "SporTV",
    "globo" to "Globo",
    "cazetv" to "Cazé",
    "caze" to "Cazé",
    "record" to "Record",
    "band" to "Band",
    "espn" to "ESPN",
    "disney" to "Disney+",
    "amazon" to "Amazon Prime",
    "prime" to "Amazon Prime",
    "globoplay" to "Globoplay",
)

private val transmissaoCacheLock = Any()
private var cachedChannels: List<String>? = null
private var cachedForDate: LocalDate? = null
private var transmissaoCachedAtMillis: Long = 0L

/**
 * Canais (emissoras) que passam o próximo jogo [match], via agenda do ge.
 * Retorna lista vazia quando o jogo está fora da janela da agenda ou sem canal anunciado.
 * Lança IOException em erro de rede. Usa cache em memória (TTL 30min) por data do jogo.
 */
fun broadcastFor(match: Match): List<String> = synchronized(transmissaoCacheLock) {
    val date = OffsetDateTime.parse(match.startsAt).toInstant().atZone(sportsDbZone).toLocalDate()
    val now = System.currentTimeMillis()
    val fresh = now - transmissaoCachedAtMillis < TRANSMISSAO_CACHE_TTL_MS && transmissaoCachedAtMillis != 0L
    if (fresh && cachedForDate == date) return cachedChannels ?: emptyList()

    val channels = parseFluBroadcast(fetchAgendaHtml(), date)
    cachedChannels = channels
    cachedForDate = date
    transmissaoCachedAtMillis = now
    channels
}

private fun fetchAgendaHtml(): String {
    val request = Request.Builder().url(AGENDA_URL).header("User-Agent", BROWSER_UA).build()
    sportsDbHttp.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("ge retornou HTTP ${response.code}")
        return response.body?.string() ?: ""
    }
}

private val firstContestantRegex = Regex("\"firstContestant\"")
private val popularNameRegex = Regex("\"popularName\":\"([^\"]+)\"")
private val startDateRegex = Regex("\"startDate\":\"([0-9-]+)\"")
private val mediaTypeRegex = Regex("\"mediaType\":\"([^\"]+)\"")

/**
 * Extrai as emissoras do jogo do Fluminense em [date] do HTML da agenda do ge.
 * O JSON vem embutido no HTML (SoccerEvent). Cada jogo tem firstContestant/secondContestant
 * (popularName do time) e transmission.gameVideos[].mediaType (a emissora).
 * Função pura para ser testável com um HTML de fixture.
 */
fun parseFluBroadcast(html: String, date: LocalDate): List<String> {
    val alvo = date.toString()

    for (contestant in firstContestantRegex.findAll(html)) {
        val window = html.substring(contestant.range.first, minOf(contestant.range.first + 3000, html.length))

        val home = popularNameRegex.find(window)?.groupValues?.get(1) ?: continue
        val secIdx = window.indexOf("\"secondContestant\"")
        val away = if (secIdx >= 0) {
            popularNameRegex.find(window.substring(secIdx, minOf(secIdx + 400, window.length)))?.groupValues?.get(1)
        } else null

        val ehDoFlu = home.contains("Fluminense") || (away?.contains("Fluminense") == true)
        val ehNaData = startDateRegex.find(window)?.groupValues?.get(1) == alvo
        if (!ehDoFlu || !ehNaData) continue

        val transIdx = window.indexOf("\"transmission\"")
        if (transIdx < 0) return emptyList()
        val transBlock = window.substring(transIdx, minOf(transIdx + 600, window.length))

        return mediaTypeRegex.findAll(transBlock)
            .map { prettyChannel(it.groupValues[1]) }
            .distinct()
            .toList()
    }
    return emptyList()
}

private fun prettyChannel(mediaType: String): String {
    val key = mediaType.lowercase()
    return canalPtBr[key] ?: mediaType.replaceFirstChar { it.uppercase() }
}

/**
 * Linha de canal pro /quando, ex.: "📺 Premiere, SporTV".
 * Lista vazia vira aviso honesto (canal só é confirmado uns dias antes do jogo).
 * Função pura para ser testável.
 */
fun formatBroadcastLine(channels: List<String>): String =
    if (channels.isEmpty()) "📺 canal ainda não anunciado"
    else "📺 ${channels.joinToString(", ")}"
