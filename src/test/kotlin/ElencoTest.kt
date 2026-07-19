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
})
