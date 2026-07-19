package br.com.colman.bot

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TransmissaoTest {

    // Fixture no formato do JSON embutido da agenda do ge (só os campos que usamos).
    private fun evento(home: String, away: String, date: String, media: List<String>): String {
        val gameVideos = media.joinToString(",") { "{\"mediaType\":\"$it\",\"video\":{\"id\":1}}" }
        return """
            "firstContestant":{"popularName":"$home"},
            "secondContestant":{"popularName":"$away"},
            "startDate":"$date","startHour":"20:00:00",
            "transmission":{"gameVideos":[$gameVideos],"id":"x"}
        """.trimIndent()
    }

    @Test
    fun `extrai emissoras do jogo do Flu na data`() {
        val html = evento("Fluminense", "Bragantino", "2026-07-17", listOf("premiere", "sportv"))
        assertEquals(listOf("Premiere", "SporTV"), parseFluBroadcast(html, LocalDate.parse("2026-07-17")))
    }

    @Test
    fun `acha o Flu jogando fora`() {
        val html = evento("Grêmio", "Fluminense", "2026-07-26", listOf("premiere"))
        assertEquals(listOf("Premiere"), parseFluBroadcast(html, LocalDate.parse("2026-07-26")))
    }

    @Test
    fun `ignora jogo em outra data`() {
        val html = evento("Fluminense", "Bragantino", "2026-07-17", listOf("premiere"))
        assertEquals(emptyList<String>(), parseFluBroadcast(html, LocalDate.parse("2026-07-26")))
    }

    @Test
    fun `ignora jogo sem o Flu`() {
        val html = evento("Palmeiras", "Corinthians", "2026-07-17", listOf("globo"))
        assertEquals(emptyList<String>(), parseFluBroadcast(html, LocalDate.parse("2026-07-17")))
    }

    @Test
    fun `mediaType desconhecido vira capitalizado`() {
        val html = evento("Fluminense", "Vasco", "2026-07-17", listOf("novocanal"))
        assertEquals(listOf("Novocanal"), parseFluBroadcast(html, LocalDate.parse("2026-07-17")))
    }

    @Test
    fun `linha com canais junta com virgula`() {
        assertEquals("📺 Premiere, SporTV", formatBroadcastLine(listOf("Premiere", "SporTV")))
    }

    @Test
    fun `linha vazia avisa que nao foi anunciado`() {
        assertEquals("📺 canal ainda não anunciado", formatBroadcastLine(emptyList()))
    }
}
