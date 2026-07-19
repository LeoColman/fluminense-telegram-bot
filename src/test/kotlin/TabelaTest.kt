package br.com.colman.bot

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TabelaTest {

    private val rows = listOf(
        TableRow(rank = 1, team = "Palmeiras", played = 18, goalDiff = 17, points = 41),
        TableRow(rank = 2, team = "Flamengo", played = 17, goalDiff = 14, points = 38),
        TableRow(rank = 3, team = "Fluminense", played = 18, goalDiff = -2, points = 30),
    )

    @Test
    fun `cabecalho e linhas formatadas com saldo`() {
        val out = formatTabela(rows)
        val esperado = """
            📊 Brasileirão

             1 Palmeiras — 41 pts (18j, +17)
             2 Flamengo — 38 pts (17j, +14)
             3 Fluminense — 30 pts (18j, -2) 🇮🇹
        """.trimIndent()
        assertEquals(esperado, out)
    }

    @Test
    fun `destaca apenas o Fluminense`() {
        val out = formatTabela(rows)
        assertEquals(1, out.split("🇮🇹").size - 1)
        assertTrue(out.contains("Fluminense — 30 pts (18j, -2) 🇮🇹"))
    }

    @Test
    fun `tabela vazia devolve aviso`() {
        assertEquals("Tabela indisponível no momento 😕", formatTabela(emptyList()))
    }
}
