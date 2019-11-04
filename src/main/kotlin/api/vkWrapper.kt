package api

import chatIds
import getLogger
import testMode
import vkApiToken
import java.lang.StringBuilder
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.time.Duration
import kotlin.random.Random
import java.net.http.HttpResponse


class Vk {
    @Suppress("SameParameterValue")
    private val log = getLogger("vk wrapper")
    private fun post(methodName: String, params: MutableMap<String, String>) {
        if (testMode) {
            return
        }
        val reqParams = StringBuilder()
        reqParams.append("access_token=$vkApiToken&")
        reqParams.append("v=5.103&")
        reqParams.append("random_id=${Random(System.currentTimeMillis()).nextInt()}&")
        for ((p, v) in params) {
            reqParams.append("$p=$v&")
        }
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.vk.com/method/$methodName"))
            .timeout(Duration.ofSeconds(10))
            .POST(HttpRequest.BodyPublishers.ofString(reqParams.toString()))
            .build()
        val client = HttpClient.newHttpClient()
        val response = client.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        )
        log.info("response: ${response.body()}")
    }

    fun send(message: String, chatId: List<String>) {
        for (id in chatId) {
            log.info("message: $message, char_id: $id")
            this.post(
                "messages.send", mutableMapOf(
                    "message" to message,
                    "chat_id" to id
                )
            )
        }
    }

}

fun main() {
    Vk().send(
        "test",
        chatIds
    )
}