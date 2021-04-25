package chatbot.chatModules

import api.*
import chatbot.CommandPermission
import chatbot.ModuleObject
import chatbot.OnCommand
import chatbot.chatBotEvents.LongPollNewMessageEvent
import java.lang.Thread.sleep
import java.util.concurrent.ConcurrentHashMap

@ModuleObject
object Gulag : Votable() {
    private const val kickMinuteTime = 12 * 60 // Время нахождения в ГУЛАГе
    val gulagKickTime = ConcurrentHashMap<Pair<Int, Int>, Long>()

    @OnCommand(["гулаг", "gulag"], "голосование на отправление в трудовой лагерь")
    fun votingGulag(event: LongPollNewMessageEvent) {
        voting(event) { api, _, _, target ->
            val screenName = target.targetScreenName
            if (target.targetId to event.chatId in gulagKickTime.keys) {
                api.send("Партия уже наказала $screenName", event.chatId)
                return@voting
            }
            super.votingForMessage = "Голосование на отправление $screenName в лагерь началось\n" +
                    "Отправь /гулаг ${target.rawText}"
            super.successVoteMessage = "за отправление $screenName в лагерь"
            super.keyboardMessage = "/гулаг ${target.rawText}"
            super.onEndVotingMessage =
                "Подумай над своим поведением, $screenName, а потом напиши админам, чтобы тебя позвали назад"
        }
    }

    @OnCommand(["вернуть", "back"], "вернуть из ссылки", CommandPermission.ADMIN)
    fun cancelVotingResultGulag(event: LongPollNewMessageEvent) {
        cancelVotingResult(event, "Самопроизвольное возвращение из ссылки запрещено!") { api, chatId, _, target ->
            if (gulagKickTime.remove(target.targetId to chatId) == null) {
                api.send("Данного человека нет в архивах ГУЛАГ", chatId)
                return@cancelVotingResult
            }

            api.send("${target.targetScreenName} может вернуться досрочно", chatId)
        }
    }

    @OnCommand(["admgulag"], "В гулаг без суда и следствия", CommandPermission.ADMIN, showOnHelp = false)
    fun adminVotingGulag(event: LongPollNewMessageEvent) {
        adminVoting(event) { api, chatId, _, target ->
            api.send(
                "Подумай над своим поведением, ${target.targetScreenName}, а потом напиши админам, чтобы тебя позвали назад",
                chatId
            )
        }
    }

    override fun onEndVoting(targetId: Int, chatId: Int, api: VkPlatform) {
        sleep(500)
        api.kickUserFromChat(chatId, targetId)
        val currentTime = System.currentTimeMillis()
        gulagKickTime[targetId to chatId] = currentTime + 1000 * 60 * kickMinuteTime
        voting.remove(targetId to chatId)
    }

    override var targetNoneMessage: String = "Не указан ссыльный"
    override var targetNullMessage: String = "Товарищ, нельзя сослать того, кого нет"
    override var targetNoneGetBackMessage: String = "Не указан сосланный"
    override var targetEqualsSenderMessage: String = "Товарищ! Вы еще нужны своей Родине"
    override var targetEqualsBotMessage: String = "Пара воронков уже выехали"
    override var alreadyVotedMessage = ", Вы уже проголосовали за этого предателя Родины"
}