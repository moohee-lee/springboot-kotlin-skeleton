package com.example.skeleton.common.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.classic.util.LogbackMDCAdapter
import net.logstash.logback.appender.LoggingEventAsyncDisruptorAppender
import net.logstash.logback.marker.Markers
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.readLines
import kotlin.test.assertEquals

class AsyncFileLoggingTests {
    @TempDir
    lateinit var directory: Path

    private val context = LoggerContext()

    @AfterEach
    fun stopLogging() = context.stop()

    @Test
    fun `a single event in each log is flushed without further traffic`() {
        configure()
        context.getLogger("api.raw").info(Markers.append("id", 1), "")
        context.getLogger("reactor.netty.http.server.AccessLog").info("GET /1")

        await().atMost(Duration.ofSeconds(5)).untilAsserted {
            assertEquals(listOf("""{"id":1}"""), directory.resolve("api.log").readLines())
            assertEquals(listOf("GET /1"), directory.resolve("access.log").readLines())
        }
    }

    @Test
    fun `file buffer writes on overflow and shutdown flushes the remainder`() {
        context.putProperty("ACCESS_LOG_BUFFER_SIZE", "1KB")
        configure()
        val logger = context.getLogger("reactor.netty.http.server.AccessLog")
        val async = logger.getAppender("ASYNC_ACCESS_FILE") as LoggingEventAsyncDisruptorAppender
        val appender = async.getAppender("ACCESS_FILE")
        val file = directory.resolve("access.log")
        val line = "x".repeat(600)
        val event = LoggingEvent(javaClass.name, logger, Level.INFO, line, null, null)

        // Write directly to isolate the byte buffer from Disruptor's end-of-batch flush.
        appender.doAppend(event)
        appender.doAppend(event)
        assertEquals(listOf(line), file.readLines())
        context.stop()
        assertEquals(listOf(line, line), file.readLines())
    }

    @Test
    fun `shutdown drains both ring buffers and preserves event order in each file`() {
        configure()
        val api = context.getLogger("api.raw")
        val access = context.getLogger("reactor.netty.http.server.AccessLog")
        val ids = 1..1024
        for (id in ids) {
            api.info(Markers.append("id", id), "")
            access.info("GET /{}", id)
        }
        context.stop()
        assertEquals(ids.map { """{"id":$it}""" }, directory.resolve("api.log").readLines())
        assertEquals(ids.map { "GET /$it" }, directory.resolve("access.log").readLines())
    }

    private fun configure() {
        context.mdcAdapter = LogbackMDCAdapter()
        context.putProperty("LOG_DIR", directory.toString())
        context.start()
        val config = """
            <configuration>
                <include resource="logback/logback-api.xml"/>
                <include resource="logback/logback-access.xml"/>
            </configuration>
        """.trimIndent()
        JoranConfigurator().also { it.context = context }.doConfigure(config.byteInputStream())
    }
}
