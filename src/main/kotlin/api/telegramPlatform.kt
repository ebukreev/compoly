package api

import com.google.gson.Gson
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.HttpClientBuilder
import telApiToken
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class TelegramPlatform : PlatformApiInterface {
    private val gson = Gson()
    private val client = HttpClient.newHttpClient()
    private val chatIds = mutableSetOf<Int>()

    override fun send(text: String, chatId: Int, attachments: List<String>) {
        chatIds.add(chatId)
        if(attachments.isEmpty()) sendMessage(chatId, text)
        else {
            if (attachments.size == 1) sendPhotoURL(chatId, attachments[0], text)
            else sendMediaGroupURL(
                    chatId,
                    attachments.map { TGInputMedia("photo", it) }.toTypedArray()
            )
        }
    }

    override fun getUserNameById(id: Int): String? {
        for (chatId in chatIds) {
            val values = mapOf(
                    "chat_id" to chatId,
                    "user_id" to id
            )
            val result =
                    makeJsonRequest<ChatMemberResponse>("getChatMember", values)
            if (result != null) return (result as TGChatMember).user.username
        }
        return null
    }

    override fun getUserIdByName(username: String): Int? {
        for (chatId in chatIds) {
            val values = mapOf(
                    "chat_id" to chatId,
                    "user_id" to "@$username"
            )
            val result = makeJsonRequest<ChatMemberResponse>("getChatMember", values)
            if (result != null) return (result as TGChatMember).user.id
        }
        return null
    }

    override fun kickUserFromChat(chatId: Int, userId: Int) {
        val values = mapOf(
                "chat_id" to chatId,
                "user_id" to userId
        )
        makeJsonRequest<Boolean>("kickChatMember", values)
    }

    fun getMe() = makeJsonRequest<UserResponse>("getMe", null) as TGUser?

    fun getUpdates(offset: Int): Array<TGUpdate>? {
        val values = mapOf(
                "offset" to offset,
                "timeout" to 25
        )
        return makeJsonRequest<UpdatesResponse>("getUpdates", values) as Array<TGUpdate>?
    }

    private fun sendMessage(chatId: Int, text: String): TGMessage? {
        val values = mapOf(
                "chat_id" to chatId,
                "text" to text
        )
        return makeJsonRequest<MessageResponse>("sendMessage", values) as TGMessage?
    }

    private fun sendPhotoURL(chatId: Int, photo: String, caption: String = ""): TGMessage? {
        val values = mapOf(
                "chat_id" to chatId,
                "photo" to photo,
                "caption" to caption
        )
        return makeJsonRequest<MessageResponse>("sendPhoto", values) as TGMessage?
    }

    private fun sendMediaGroupURL(chatId: Int, media: Array<TGInputMedia>): Array<TGMessage>? {
        val values = mapOf(
                "chat_id" to chatId,
                "media" to media
        )
        return makeJsonRequest<MessagesResponse>("sendMediaGroup", values) as Array<TGMessage>?
    }

    @ExperimentalStdlibApi
    fun sendPhotoFile(chatId: Int, photoByteArray: ByteArray, caption: String?): String {
        val parameters = mapOf(
                "chat_id" to chatId.toString(),
                "caption" to caption
        )
        return makeMultipartRequest(parameters, photoByteArray)
    }

    private inline fun <reified T> makeJsonRequest(
            method: String, values: Map<String, Any?>?
    ): Any? {
        val requestBody = gson.toJson(values)

        val request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .uri(URI.create("https://api.telegram.org/bot$telApiToken/$method"))
                .header("Content-Type", "application/json")
                .build()

        val json = client.send(request, HttpResponse.BodyHandlers.ofString()).body()
        val response = gson.fromJson(json, T::class.java)

        return if (response is Response && response.ok) response.result
        else null
    }


    @ExperimentalStdlibApi
    private fun makeMultipartRequest(
            parameters: Map<String, String?>, byteArray: ByteArray
    ): String {
        val multipartBuilder = MultipartEntityBuilder
                .create()
                .addBinaryBody("photo", byteArray)
        for ((key, value) in parameters) {
            multipartBuilder.addTextBody(key, value)
        }
        val multipartData = multipartBuilder.build()
        val requestUploadImage = HttpPost("https://api.telegram.org/bot$telApiToken/sendPhoto")
        requestUploadImage.entity = multipartData
        return HttpClientBuilder
                .create()
                .build()
                .execute(requestUploadImage)
                .entity
                .content
                .readAllBytes()
                .decodeToString()
    }
}

abstract class Response {
    abstract val ok: Boolean
    abstract val result: Any
    abstract val description: String?
}

data class UpdatesResponse(
        override val ok: Boolean,
        override val result: Array<TGUpdate>,
        override val description: String?
): Response()

data class MessageResponse(
        override val ok: Boolean,
        override val result: TGMessage,
        override val description: String?
): Response()

data class MessagesResponse(
        override val ok: Boolean,
        override val result: Array<TGMessage>,
        override val description: String?
): Response()

data class UserResponse(
        override val ok: Boolean,
        override val result: TGUser,
        override val description: String?
): Response()

data class ChatMemberResponse(
        override val ok: Boolean,
        override val result: TGChatMember,
        override val description: String?
): Response()


data class TGUser(
        val id: Int,
        val is_bot: Boolean,
        val first_name: String?,
        val last_name: String?,
        val username: String?,
        val language_code: String?,
        val can_join_groups: Boolean,
        val can_read_all_group_messages: Boolean,
        val supports_inline_queries: Boolean
)

data class TGChatMember(
        val user: TGUser,
        val status: String
)

data class TGChat(
        val id: Int,
        val type: String,
        val title: String
)

data class TGMessage(
        val message_id: Int,
        val from: TGUser,
        val date: Int,
        val chat: TGChat,
        val text: String
)

data class TGUpdate(
        val update_id: Int,
        val message: TGMessage
)

data class TGInputMedia(
        val type: String,
        val media: String
)