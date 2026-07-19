package br.com.colman.bot

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class TransmissaoTest : FunSpec({

    // Fixture no formato do JSON embutido da agenda do ge (só os campos que usamos).
    fun evento(home: String, away: String, date: String, media: List<String>): String {
        val gameVideos = media.joinToString(",") { "{\"mediaType\":\"$it\",\"video\":{\"id\":1}}" }
        return """
            "firstContestant":{"popularName":"$home"},
            "secondContestant":{"popularName":"$away"},
            "startDate":"$date","startHour":"20:00:00",
            "transmission":{"gameVideos":[$gameVideos],"id":"x"}
        """.trimIndent()
    }

    test("extrai emissoras do jogo do Flu na data") {
        val html = evento("Fluminense", "Bragantino", "2026-07-17", listOf("premiere", "sportv"))
        parseFluBroadcast(html, LocalDate.parse("2026-07-17")) shouldBe listOf("Premiere", "SporTV")
    }

    test("acha o Flu jogando fora") {
        val html = evento("Grêmio", "Fluminense", "2026-07-26", listOf("premiere"))
        parseFluBroadcast(html, LocalDate.parse("2026-07-26")) shouldBe listOf("Premiere")
    }

    test("ignora jogo em outra data") {
        val html = evento("Fluminense", "Bragantino", "2026-07-17", listOf("premiere"))
        parseFluBroadcast(html, LocalDate.parse("2026-07-26")) shouldBe emptyList<String>()
    }

    test("ignora jogo sem o Flu") {
        val html = evento("Palmeiras", "Corinthians", "2026-07-17", listOf("globo"))
        parseFluBroadcast(html, LocalDate.parse("2026-07-17")) shouldBe emptyList<String>()
    }

    test("mediaType desconhecido vira capitalizado") {
        val html = evento("Fluminense", "Vasco", "2026-07-17", listOf("novocanal"))
        parseFluBroadcast(html, LocalDate.parse("2026-07-17")) shouldBe listOf("Novocanal")
    }

    test("linha com canais junta com virgula") {
        formatBroadcastLine(listOf("Premiere", "SporTV")) shouldBe "📺 Premiere, SporTV"
    }

    test("linha vazia avisa que nao foi anunciado") {
        formatBroadcastLine(emptyList()) shouldBe "📺 canal ainda não anunciado"
    }
})
