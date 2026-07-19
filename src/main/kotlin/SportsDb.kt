package br.com.colman.bot

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
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

// Sites como o ge/fluminense.com.br só respondem HTML completo com User-Agent de browser.
internal const val BROWSER_UA = "Mozilla/5.0"

internal fun fetchHtml(url: String): String {
    val request = Request.Builder().url(url).header("User-Agent", BROWSER_UA).build()
    sportsDbHttp.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("HTTP ${response.code} em $url")
        return response.body?.string() ?: ""
    }
}
