package modules.pageChecking

import api.Vk
import chatIds
import log
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Paths
import modules.Module
import sendGet

class PageChecker : Module {
    override val callingType = 1
    override val millis = arrayOf(30 * 1000L)
    override val name = "Проверка обновления страницы"
    override var lastCalling = 0L

    // Можно добавить сюда другие сайты
    private val pages = listOf(
        Link("Практика по дискретке [Сабонис]", "http://sergei-sabonis.ru/Student/20192020/dm2019.pdf")
    )

    private fun getPath(page: String): String {
        val filePath = "/data/savedPages/" + page.replace(Regex("""[\\?|"/.:<>*]"""), "_") + ".txt"
        return Paths.get("").toAbsolutePath().toString() + filePath
    }

    private fun isUpdated(page: String): Boolean? {
        val path = getPath(page)
        val newPage = sendGet(page) ?: return null
        return try {
            if (newPage != File(path).readText()) {
                File(path).bufferedWriter().use { it.write(newPage) }
                log.info("Page $path was updated")
                true
            } else {
                false
            }
        } catch (e: FileNotFoundException) {
            log.warning("File $path not found")
            File(path).writeText(newPage)
            log.info("File $path was created")
            null
        }
    }

    override fun call() {
        for (page in pages) {
            if (isUpdated(page.trueUrl) == true) {
                log.info("Page ${page.showingUrl} was updated")
                Vk().send("Страница ${page.showingUrl} была обновлена", chatIds)
            }
        }
    }
}

data class Link(val name: String, val trueUrl: String, val showingUrl: String = trueUrl)