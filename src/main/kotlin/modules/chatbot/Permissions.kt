package modules.chatbot

import modules.chatbot.chatBotEvents.LongPollNewMessageEvent

object Permissions {
    fun getUserPermissionsByNewMessageEvent(event: LongPollNewMessageEvent) =
            if (event.api.isUserAdmin(event.chatId, event.userId)) CommandPermission.ADMIN
            else CommandPermission.USER
}