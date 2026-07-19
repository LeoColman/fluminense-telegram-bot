package br.com.colman.bot

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ElencoTest : FunSpec({

    test("parseia listagem em slug, numero e nome") {
        val html = """
            <a href="/jogador/fabio"> <div class="player-name text-center">1 - FÁBIO</div> </a>
            <a href="/jogador/ph-ganso"> <div class="player-name text-center">10 - PH GANSO</div> </a>
        """.trimIndent()
        parseSquadListing(html) shouldBe listOf(
            RawPlayer("fabio", "FÁBIO", 1),
            RawPlayer("ph-ganso", "PH GANSO", 10),
        )
    }

    test("jogador sem numero fica com nome inteiro e numero nulo") {
        val html = """<a href="/jogador/x"> <div class="player-name text-center">Fulano de Tal</div> </a>"""
        parseSquadListing(html) shouldBe listOf(RawPlayer("x", "Fulano de Tal", null))
    }

    test("extrai posicao da pagina do jogador") {
        parsePosition("""<div class="player-position"> Volante </div>""") shouldBe "Volante"
    }

    test("posicao ausente vira interrogacao") {
        parsePosition("<div>sem posicao aqui</div>") shouldBe "?"
    }

    test("ordena por numero e formata com posicao") {
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
        formatElenco(players) shouldBe esperado
    }

    test("jogador sem numero vai pro fim e posicao interrogacao some") {
        val players = listOf(
            Player("Sem Numero", "?", null),
            Player("Camisa Dez", "Meia", 10),
        )
        val esperado = """
            👥 Elenco do Fluminense

            #10 Camisa Dez (Meia)
            Sem Numero
        """.trimIndent()
        formatElenco(players) shouldBe esperado
    }

    test("elenco vazio devolve aviso") {
        formatElenco(emptyList()) shouldBe "Elenco indisponível no momento 😕"
    }
})
