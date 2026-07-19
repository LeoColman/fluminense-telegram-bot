package br.com.colman.bot

// Listagem do elenco no site oficial. Hoje serve só pra saber QUEM é do Fluminense
// (usado por Escalacao.kt pra separar o time do Flu do adversário na escalação do ge).
private const val SQUAD_URL = "https://www.fluminense.com.br/o-time/futebol/profissional"

// Um jogador da listagem: slug do site + nome + número da camisa.
data class RawPlayer(val slug: String, val name: String, val number: Int?)

private val listingRegex =
    Regex("<a href=\"/jogador/([^\"]+)\">\\s*<div class=\"player-name text-center\">([^<]+)</div>")

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

/** Nomes do elenco atual do Fluminense (site oficial). Lança IOException em erro de rede. */
fun rosterNames(): List<String> = parseSquadListing(fetchHtml(SQUAD_URL)).map { it.name }
