package br.com.colman.bot

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.ZoneId
import java.time.ZonedDateTime

class ResultadoTest : FunSpec({

    val zone = ZoneId.of("America/Sao_Paulo")
    // Sábado, 18/07/2026 às 11:00 (horário de Brasília, UTC-3).
    val now = ZonedDateTime.of(2026, 7, 18, 11, 0, 0, 0, zone)

    fun result(
        utcDate: String,
        home: String = "Fluminense",
        away: String = "Bragantino",
        homeScore: Int = 1,
        awayScore: Int = 1,
        competition: String = "Brasileirão",
    ) = MatchResult(utcDate, Competition(competition), Team(home), Team(away), homeScore, awayScore)

    test("jogo de ontem usa ontem com data") {
        // 23:00Z de 17/07 == 20:00 de sexta 17/07 em São Paulo.
        val out = formatResultado(result("2026-07-17T23:00:00Z"), now)
        out shouldBe "Fluminense 1 x 1 Bragantino (Brasileirão) — ontem 17/07"
    }

    test("jogo de hoje usa hoje") {
        // 12:00Z de 18/07 == 09:00 de hoje em São Paulo.
        val out = formatResultado(result("2026-07-18T12:00:00Z", homeScore = 3, awayScore = 0), now)
        out shouldBe "Fluminense 3 x 0 Bragantino (Brasileirão) — hoje 18/07"
    }

    test("jogo antigo usa ha X dias") {
        // 22:00Z de 13/07 == 19:00 em São Paulo, 5 dias atrás.
        val out = formatResultado(result("2026-07-13T22:00:00Z"), now)
        out shouldBe "Fluminense 1 x 1 Bragantino (Brasileirão) — há 5 dias 13/07"
    }

    test("mostra placar com Fluminense jogando fora") {
        val out = formatResultado(
            result("2026-07-17T23:00:00Z", home = "Flamengo", away = "Fluminense", homeScore = 0, awayScore = 2),
            now,
        )
        out shouldBe "Flamengo 0 x 2 Fluminense (Brasileirão) — ontem 17/07"
    }
})
