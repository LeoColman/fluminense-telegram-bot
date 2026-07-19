package br.com.colman.bot

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ElencoTest {

    @Test
    fun `ordena por numero e formata com posicao`() {
        val players = listOf(
            Player("David Terans", "Attacking Midfield", 80),
            Player("Agustín Canobbio", "Right Winger", 17),
            Player("Alisson", "Attacker", 25),
        )
        val out = formatElenco(players)
        val esperado = """
            👥 Elenco do Fluminense (parcial)

            #17 Agustín Canobbio (Right Winger)
            #25 Alisson (Attacker)
            #80 David Terans (Attacking Midfield)
        """.trimIndent()
        assertEquals(esperado, out)
    }

    @Test
    fun `jogador sem numero vai pro fim sem hashtag`() {
        val players = listOf(
            Player("Sem Numero", "Defender", null),
            Player("Camisa Dez", "Midfield", 10),
        )
        val out = formatElenco(players)
        val esperado = """
            👥 Elenco do Fluminense (parcial)

            #10 Camisa Dez (Midfield)
            Sem Numero (Defender)
        """.trimIndent()
        assertEquals(esperado, out)
    }

    @Test
    fun `elenco vazio devolve aviso`() {
        assertEquals("Elenco indisponível no momento 😕", formatElenco(emptyList()))
    }
}
