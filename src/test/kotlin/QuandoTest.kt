package br.com.colman.bot

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class QuandoTest {

    private val zone = ZoneId.of("America/Sao_Paulo")
    // Sábado, 18/07/2026 às 11:00 (horário de Brasília, UTC-3).
    private val now = ZonedDateTime.of(2026, 7, 18, 11, 0, 0, 0, zone)

    private fun match(
        utcDate: String,
        home: String = "Fluminense FC",
        away: String = "Flamengo",
        competition: String = "Campeonato Brasileiro",
    ) = Match(utcDate, Competition(competition), Team(home), Team(away))

    @Test
    fun `jogo hoje mostra frase relativa em horas`() {
        // 16:00Z == 13:00 em São Paulo, 2h depois de now.
        val result = formatQuando(match("2026-07-18T16:00:00Z"), now)
        assertEquals("Hoje daqui 2h às 13:00 vs Flamengo (Campeonato Brasileiro)", result)
    }

    @Test
    fun `jogo hoje com horas e minutos`() {
        // 16:30Z == 13:30 em São Paulo, 2h30 depois de now.
        val result = formatQuando(match("2026-07-18T16:30:00Z"), now)
        assertEquals("Hoje daqui 2h 30min às 13:30 vs Flamengo (Campeonato Brasileiro)", result)
    }

    @Test
    fun `jogo amanha usa Amanha sem relativo`() {
        // 00:00Z de 20/07 == 21:00 de 19/07 em São Paulo.
        val result = formatQuando(match("2026-07-20T00:00:00Z"), now)
        assertEquals("Amanhã às 21:00 vs Flamengo (Campeonato Brasileiro)", result)
    }

    @Test
    fun `jogo na mesma semana usa dia da semana e data`() {
        // 19:00Z de 22/07 == 16:00 de quarta 22/07 em São Paulo.
        val result = formatQuando(match("2026-07-22T19:00:00Z"), now)
        assertEquals("Quarta 22/07 às 16:00 vs Flamengo (Campeonato Brasileiro)", result)
    }

    @Test
    fun `jogo distante usa data e dias`() {
        // 19:00Z de 01/08 == 16:00 em São Paulo, 14 dias depois.
        val result = formatQuando(match("2026-08-01T19:00:00Z"), now)
        assertEquals("01/08 (daqui 14 dias) às 16:00 vs Flamengo (Campeonato Brasileiro)", result)
    }

    @Test
    fun `identifica adversario quando Fluminense joga fora`() {
        val result = formatQuando(match("2026-07-18T16:00:00Z", home = "Flamengo", away = "Fluminense FC"), now)
        assertEquals("Hoje daqui 2h às 13:00 vs Flamengo (Campeonato Brasileiro)", result)
    }
}
