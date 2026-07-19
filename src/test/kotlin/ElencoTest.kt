package br.com.colman.bot

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ElencoTest {

    @Test
    fun `parseia listagem em slug, numero e nome`() {
        val html = """
            <a href="/jogador/fabio"> <div class="player-name text-center">1 - FÁBIO</div> </a>
            <a href="/jogador/ph-ganso"> <div class="player-name text-center">10 - PH GANSO</div> </a>
        """.trimIndent()
        assertEquals(
            listOf(
                RawPlayer("fabio", "FÁBIO", 1),
                RawPlayer("ph-ganso", "PH GANSO", 10),
            ),
            parseSquadListing(html),
        )
    }

    @Test
    fun `jogador sem numero fica com nome inteiro e numero nulo`() {
        val html = """<a href="/jogador/x"> <div class="player-name text-center">Fulano de Tal</div> </a>"""
        assertEquals(listOf(RawPlayer("x", "Fulano de Tal", null)), parseSquadListing(html))
    }

    @Test
    fun `extrai posicao da pagina do jogador`() {
        assertEquals("Volante", parsePosition("""<div class="player-position"> Volante </div>"""))
    }

    @Test
    fun `posicao ausente vira interrogacao`() {
        assertEquals("?", parsePosition("<div>sem posicao aqui</div>"))
    }

    @Test
    fun `ordena por numero e formata com posicao`() {
        val players = listOf(
            Player("David Terans", "Meia", 80),
            Player("Agustín Canobbio", "Atacante", 17),
            Player("Alisson", "Atacante", 25),
        )
        val esperado = """
            👥 Elenco do Fluminense

            #17 Agustín Canobbio (Atacante)
            #25 Alisson (Atacante)
            #80 David Terans (Meia)
        """.trimIndent()
        assertEquals(esperado, formatElenco(players))
    }

    @Test
    fun `jogador sem numero vai pro fim e posicao interrogacao some`() {
        val players = listOf(
            Player("Sem Numero", "?", null),
            Player("Camisa Dez", "Meia", 10),
        )
        val esperado = """
            👥 Elenco do Fluminense

            #10 Camisa Dez (Meia)
            Sem Numero
        """.trimIndent()
        assertEquals(esperado, formatElenco(players))
    }

    @Test
    fun `elenco vazio devolve aviso`() {
        assertEquals("Elenco indisponível no momento 😕", formatElenco(emptyList()))
    }
}
