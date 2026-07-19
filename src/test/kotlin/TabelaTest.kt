package br.com.colman.bot

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class TabelaTest : FunSpec({

    val rows = listOf(
        TableRow(rank = 1, team = "Palmeiras", played = 18, goalDiff = 17, points = 41),
        TableRow(rank = 2, team = "Flamengo", played = 17, goalDiff = 14, points = 38),
        TableRow(rank = 3, team = "Fluminense", played = 18, goalDiff = -2, points = 30),
    )

    test("cabecalho e linhas formatadas com saldo") {
        val out = formatTabela(rows)
        val esperado = """
            📊 Brasileirão

             1 Palmeiras — 41 pts (18j, +17)
             2 Flamengo — 38 pts (17j, +14)
             3 Fluminense — 30 pts (18j, -2) 🇮🇹
        """.trimIndent()
        out shouldBe esperado
    }

    test("destaca apenas o Fluminense") {
        val out = formatTabela(rows)
        (out.split("🇮🇹").size - 1) shouldBe 1
        out shouldContain "Fluminense — 30 pts (18j, -2) 🇮🇹"
    }

    test("tabela vazia devolve aviso") {
        formatTabela(emptyList()) shouldBe "Tabela indisponível no momento 😕"
    }
})
