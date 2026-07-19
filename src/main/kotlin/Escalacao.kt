package br.com.colman.bot

import java.text.Normalizer

// Página do time no ge: traz o link do jogo mais recente do Fluminense.
private const val TEAM_URL = "https://ge.globo.com/futebol/times/fluminense/"
private const val GE_BASE = "https://ge.globo.com"

// TTL de 3h: escalação muda só a cada jogo (dias de intervalo).
private const val ESCALACAO_CACHE_TTL_MS = 3 * 60 * 60 * 1000L

// Modelo interno (independente do formato do ge).
data class Starter(val name: String, val position: String, val number: String?)
data class Escalacao(val formation: String?, val starters: List<Starter>)

// Mapeamento do JSON de escalação do ge (só os campos que usamos).
private data class GeLineupEntry(val popularName: String?, val shirtNumber: String?, val position: GePosition?)
private data class GePosition(val description: String?)

private val escalacaoCacheLock = Any()
private var cachedEscalacao: Escalacao? = null
private var escalacaoCachedAtMillis: Long = 0L

// Link do último jogo do Flu na página do time (slug contém "fluminense").
private val jogoUrlRegex = Regex("/[a-z]{2}/futebol/[a-z0-9-]+/jogo/[0-9-]+/[a-z0-9-]*fluminense[a-z0-9-]*\\.ghtml")
private val lineUpRegex = Regex("\"lineUp\":\\[")
private val formationRegex = Regex("\"formation\":\"([^\"]+)\"")

/**
 * Provável time titular = escalação do Fluminense no jogo mais recente, via ge.
 * Retorna null quando não acha o jogo ou a escalação (ex.: jogo futuro sem escalação).
 * Lança IOException em erro de rede. Usa cache em memória (TTL 3h).
 */
fun probableStartingEleven(): Escalacao? = synchronized(escalacaoCacheLock) {
    val now = System.currentTimeMillis()
    if (now - escalacaoCachedAtMillis < ESCALACAO_CACHE_TTL_MS && escalacaoCachedAtMillis != 0L) {
        return cachedEscalacao
    }

    val path = jogoUrlRegex.find(fetchHtml(TEAM_URL))?.value ?: return null
    val escalacao = parseEscalacao(fetchHtml(GE_BASE + path), rosterNames())

    cachedEscalacao = escalacao
    escalacaoCachedAtMillis = now
    escalacao
}

/**
 * Extrai a escalação titular do Fluminense da página de um jogo do ge.
 * A página tem os dois times; identifica o do Flu pelo bloco `lineUp` cujos nomes mais
 * batem com [roster] (elenco oficial). Função pura para ser testável.
 */
fun parseEscalacao(html: String, roster: List<String>): Escalacao? {
    val rosterTokens = roster.flatMap { nameTokens(it) }.toSet()

    var melhor: Escalacao? = null
    var melhorOverlap = 0

    for (match in lineUpRegex.findAll(html)) {
        val arrayJson = extractJsonArray(html, match.range.first)
        val entries = try {
            sportsDbGson.fromJson(arrayJson, Array<GeLineupEntry>::class.java)?.toList() ?: continue
        } catch (e: Exception) {
            continue
        }

        val starters = entries.mapNotNull { entry ->
            val nome = entry.popularName?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            Starter(nome, entry.position?.description?.takeIf { it.isNotBlank() } ?: "?", entry.shirtNumber)
        }
        if (starters.isEmpty()) continue

        val overlap = starters.count { (nameTokens(it.name) intersect rosterTokens).isNotEmpty() }
        if (overlap > melhorOverlap && overlap > starters.size / 2) {
            melhorOverlap = overlap
            melhor = Escalacao(formationBefore(html, match.range.first), starters)
        }
    }

    return melhor
}

// Formação declarada logo antes do bloco lineUp escolhido.
private fun formationBefore(html: String, lineUpIndex: Int): String? =
    formationRegex.findAll(html)
        .lastOrNull { it.range.first < lineUpIndex }
        ?.groupValues?.get(1)

// Tokens do nome, sem acento e em maiúsculas, pra casar entre ge e site oficial.
private fun nameTokens(name: String): Set<String> =
    Normalizer.normalize(name, Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .uppercase()
        .split(Regex("[^A-Z]+"))
        .filter { it.isNotBlank() }
        .toSet()

// Extrai o array JSON balanceado a partir de `"lineUp":[`.
private fun extractJsonArray(html: String, lineUpMatchStart: Int): String {
    val open = html.indexOf('[', lineUpMatchStart)
    if (open < 0) return ""
    var depth = 0
    var i = open
    while (i < html.length) {
        when (html[i]) {
            '[' -> depth++
            ']' -> {
                depth--
                if (depth == 0) return html.substring(open, i + 1)
            }
        }
        i++
    }
    return ""
}

/**
 * Formata o provável time titular, ex.:
 * "🏆 Provável time titular (último jogo)\nFormação: 4-3-3\n\n#1 Fábio (Goleiro)\n...".
 * Função pura para ser testável.
 */
fun formatEscalacao(escalacao: Escalacao): String {
    val linhas = escalacao.starters.joinToString("\n") { s ->
        val numero = s.number?.takeIf { it.isNotBlank() }?.let { "#$it " } ?: ""
        val posicao = s.position.takeIf { it.isNotBlank() && it != "?" }?.let { " ($it)" } ?: ""
        "$numero${s.name}$posicao"
    }
    val cabecalho = "🏆 Provável time titular (último jogo)"
    val formacao = escalacao.formation?.let { "\nFormação: $it" } ?: ""
    return "$cabecalho$formacao\n\n$linhas"
}
