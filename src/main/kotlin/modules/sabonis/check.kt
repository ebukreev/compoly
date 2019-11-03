package modules.sabonis

import java.io.File
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Paths

const val page = "http://sergei-sabonis.ru/Student/20192020/dm2019.htm"
const val folderPath = """\data\sabonis\"""
const val fileName = """oldPage.txt"""

fun sendGet(address: String): String? {
    try {
        val url = URL(address)
        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET"  // optional default is GET
            println("\nSent 'GET' request to URL : $url; Response Code : $responseCode")
            return inputStream.bufferedReader().readText()
        }
    } catch (e: FileNotFoundException) {
        return null
    }
}

fun isUpdated(): Boolean? {

    val path = Paths.get("").toAbsolutePath().toString() + folderPath + fileName

    val newPage = sendGet(page)
    if (newPage == null) {
        println("Не удается получить доступ к странице $page")
        return null
    }

    return try {
        if (newPage != File(path).readText()) {
            File(path).bufferedWriter().use { it.write(newPage) }
            println("Файл $folderPath$fileName обновлен")
            true
        } else {
            false
        }
    } catch (e: FileNotFoundException) {
        println("Указанный файл не найден")
        null
    }

}

fun main() {
    isUpdated()
}