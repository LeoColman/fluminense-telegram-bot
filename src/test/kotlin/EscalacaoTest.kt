package br.com.colman.bot

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class EscalacaoTest : FunSpec({

    // Blocos no formato do JSON de escalação do ge (só os campos que usamos).
    val adversario = """
        "formation":"4-4-2","lineUp":[
          {"popularName":"Volpi","shirtNumber":"18","position":{"description":"Goleiro"},"substitutedBy":null}
        ]
    """.trimIndent()
    val fluminense = """
        "formation":"4-3-3","lineUp":[
          {"popularName":"Fábio","shirtNumber":"1","position":{"description":"Goleiro"},"substitutedBy":null},
          {"popularName":"Martinelli","shirtNumber":"8","position":{"description":"Volante"},"substitutedBy":null}
        ]
    """.trimIndent()
    val roster = listOf("FÁBIO", "MARTINELLI", "GUGA")

    test("escolhe o bloco do Flu pelo overlap com o elenco") {
        val esc = parseEscalacao("$adversario xxx $fluminense", roster)
        esc?.formation shouldBe "4-3-3"
        esc?.starters?.map { it.name } shouldBe listOf("Fábio", "Martinelli")
        esc?.starters?.map { it.number } shouldBe listOf("1", "8")
    }

    test("null quando nenhum bloco bate com o elenco") {
        parseEscalacao(adversario, roster) shouldBe null
    }

    test("formata titular com formacao, numero e posicao") {
        val esc = Escalacao(
            "4-3-3",
            listOf(Starter("Fábio", "Goleiro", "1"), Starter("Martinelli", "Volante", "8")),
        )
        val esperado = """
            🏆 Provável time titular (último jogo)
            Formação: 4-3-3

            #1 Fábio (Goleiro)
            #8 Martinelli (Volante)
        """.trimIndent()
        formatEscalacao(esc) shouldBe esperado
    }
})
