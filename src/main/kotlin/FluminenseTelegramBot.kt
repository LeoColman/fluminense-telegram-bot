package br.com.colman.bot

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.text
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
            command("nense") {
                bot.sendMessage(chatId = ChatId.fromId(message.chat.id), text = "NENSE! 🇮🇹🇮🇹🇮🇹")
                torcidaOptions.random().forEach {
                    bot.sendMessage(chatId = ChatId.fromId(message.chat.id), text = it)
                }
            }

            command("fabio") {
                bot.sendMessage(chatId = ChatId.fromId(message.chat.id), text = "Fáaaaaaaaaaaaaabio!! 🇮🇹🥅🧱️⚽️🏃‍♂️🇮🇹")
            }

            command("vence") {
                bot.sendMessage(chatId = ChatId.fromId(message.chat.id), text = "Vitória Fluminense! 🇮🇹🇮🇹")
                bot.sendPhoto(chatId = ChatId.fromId(message.chat.id), photo = TelegramFile.ByUrl("https://i.pinimg.com/originals/08/7f/1f/087f1f5b45e57cfc9793693976d07aeb.jpg"))
            }
        }
    }

    bot.startPolling()
    println("Application started.")
}