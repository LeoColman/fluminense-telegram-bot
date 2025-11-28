package br.com.colman.bot

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.handlers.CommandHandlerEnvironment
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.TelegramFile


val torcidaOptions = listOf(
    listOf("Fluminense Eterno Amor", "É por isso que eu canto", "E visto esse manto", "Orgulho de ser Tricolor"),
    listOf("Desde que eu nasci, te acompanhei", "Fluminense eu sei, até a morte, contigo estarei", "Haja o que houver", "Onde quer que eu vá", "Comigo vou levar, as cores que herdei", "Verde branco e grená"),
    listOf("Meu coração acelera", "Vendo o maraca cantar", "Meu Fluminense Escuta teu povo", "Que veio te apoiar"),
    listOf("Tricolor em toda terra", "Amor igual não se viu", "Canta feliz a torcida do clube", "Mais amado do Brasil"),
    listOf("Neeeeeense!", "Neeeeeense!", "Neeeeeense!"),
    listOf("FLU MI NENSE", "Olê, olê, olê", "Olê, olê, olê", "Olê, olê, olê")
)

fun main() {
    val bot = bot {
        token = System.getenv("TELEGRAM_TOKEN")

        dispatch {
            command("nense") { handleNense() }
            command("fabio") { handleFabio() }
            command("vence") { handleVence() }
            command("louco") { handleLouco() }
            command("libertadores") { handleLibertadores() }
            command("bencao") { handleBencao() }
            command("xerem") { handleXerem() }
        }
    }

    bot.startPolling()
    println("Application started.")
}

fun CommandHandlerEnvironment.sendMessage(text: String) {
    bot.sendMessage(ChatId.fromId(message.chat.id), text)
}

fun CommandHandlerEnvironment.sendPhoto(url: String) {
    bot.sendPhoto(ChatId.fromId(message.chat.id), TelegramFile.ByUrl(url))
}

fun CommandHandlerEnvironment.handleNense() {
    sendMessage("NENSE! 🇮🇹🇮🇹🇮🇹")
    torcidaOptions.random().forEach { sendMessage(it) }
}

fun CommandHandlerEnvironment.handleFabio() {
    sendMessage("Fáaaaaaaaaaaaaabio!! 🇮🇹🥅🧱️⚽️🏃‍♂️🇮🇹")
}

fun CommandHandlerEnvironment.handleVence() {
    sendMessage("Vitória Fluminense! 🇮🇹🇮🇹")
    sendPhoto("https://i.pinimg.com/originals/08/7f/1f/087f1f5b45e57cfc9793693976d07aeb.jpg")
}

fun CommandHandlerEnvironment.handleLouco() {
    val lines = listOf(
        "Fluminense vai jogar",
        "Eu vou ficar",
        "Louco da cabeça",
        "Nada me interessa",
        "Sou tricolor, sou tricolor",
        "Laialaia, laialaia, laialaia"
    )
    lines.forEach { sendMessage(it) }
}

fun CommandHandlerEnvironment.handleLibertadores() {
    val lines = listOf(
        "Vamooooos tricores! Chegou a hora vamos ganhar a libertadores",
        "Torcida, que se levante!",
        "Se viemos até aqui foi pra apoiar",
        "Tem jogo, do gigante",
        "Isso aqui é arquibancada não é sofáaaaaa",
        "Vaaaaaaaaamoss tricolores! Chegou a hora vamos ganhar a libertadores"
    )
    lines.forEach { sendMessage(it) }
}

fun CommandHandlerEnvironment.handleBencao() {
    val lines = listOf(
        "A bênção, João de Deus",
        "Nosso povo te abraça",
        "Tu vens em missão de paz",
        "Sê bem-vindo",
        "E abençoa este povo que te ama!"
    )
    lines.forEach { sendMessage(it) }
}

fun CommandHandlerEnvironment.handleXerem() {
    val lines = listOf(
        "Uh vem que tem",
        "É os mlk de Xerém"
    )
    lines.forEach { sendMessage(it) }
}