package modules.loops

import io.github.classgraph.ClassGraph
import kotlinx.coroutines.*
import log
import kotlin.concurrent.thread

class LoopStream : Runnable {

    private val loops: List<Loop>

    init {
        log.info("Initialising LoopStream...")
        var loaded: List<Loop> = emptyList()
        ClassGraph().enableAllInfo().whitelistPackages("modules.loops")
            .scan().use { scanResult ->
                val filtered = scanResult.getClassesImplementing("modules.loops.Loop")
                    .filter { classInfo ->
                        classInfo.hasAnnotation("modules.Active")
                    }
                loaded = filtered
                    .map { it.loadClass() }
                    .map { it.getConstructor().newInstance() } as List<Loop>
            }
        loops = loaded
        log.info("LoopStream is initialised")
    }

    override fun run() {
        thread {
            runBlocking {
                val jobs = mutableListOf<Job>()
                for (loop in loops) {
                    val job = async {
                        while (true) {
                            loop.call()
                            delay(loop.delay)
                        }
                    }
                    jobs.add(job)
                }
            }
        }
    }
}