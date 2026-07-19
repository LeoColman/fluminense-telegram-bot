package br.com.colman.bot

import com.google.gson.Gson
import okhttp3.OkHttpClient
import java.time.ZoneId

// Helpers compartilhados pelos comandos que consomem o TheSportsDB (/resultado, /tabela).
// O /quando (Quando.kt) mantém suas próprias cópias privadas por ser anterior a este arquivo.

// TheSportsDB league id do Brasileirão Série A. Confirmado via lookuptable.php?l=4351.
internal const val BRASILEIRAO_ID = 4351

internal val sportsDbZone: ZoneId = ZoneId.of("America/Sao_Paulo")
internal val sportsDbHttp = OkHttpClient()
internal val sportsDbGson = Gson()

// Nomes de liga em PT-BR (TheSportsDB devolve em inglês). Nomes não mapeados ficam como vêm.
private val ligaPt = mapOf(
    "Brazilian Serie A" to "Brasileirão",
)

internal fun prettyLiga(name: String?): String = name?.let { ligaPt[it] ?: it } ?: "Jogo"
