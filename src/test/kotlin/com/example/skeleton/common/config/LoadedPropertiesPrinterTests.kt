package com.example.skeleton.common.config

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoadedPropertiesPrinterTests {
    private val appender = ListAppender<ILoggingEvent>()
    private val logger = LoggerFactory.getLogger(LoadedPropertiesPrinter::class.java) as Logger

    @BeforeEach
    fun setUp() {
        appender.start()
        logger.level = Level.INFO
        logger.addAppender(appender)
    }

    @AfterEach
    fun tearDown() {
        logger.detachAppender(appender)
        appender.stop()
    }

    @Test
    fun `prints kubernetes configmap source with sensitive value masked`() {
        val environment = StandardEnvironment()
        environment.propertySources.addFirst(
            MapPropertySource(
                "configmap.kcp-network-skeleton.kcp-network",
                mapOf(
                    "spring.datasource.write.hikari.username" to "skeleton",
                    "spring.datasource.write.hikari.password" to "verysecret",
                ),
            ),
        )

        printWith(environment)

        val messages = printedMessages()
        assertTrue(messages.any { it.contains("source: configmap.kcp-network-skeleton.kcp-network") })
        assertTrue(messages.any { it.contains("spring.datasource.write.hikari.username=skeleton") })
        assertTrue(
            messages.any {
                it.contains("spring.datasource.write.hikari.password=v" + "*".repeat(8) + "t")
            },
        )
        assertFalse(messages.any { it.contains("verysecret") })
    }

    @Test
    fun `marks lower precedence value as overridden`() {
        val environment = StandardEnvironment()
        environment.propertySources.addLast(MapPropertySource("application.yml", mapOf("server.port" to "18080")))
        environment.propertySources.addFirst(MapPropertySource("configmap.skeleton.ns", mapOf("server.port" to "8080")))

        printWith(environment)

        val messages = printedMessages()
        assertTrue(messages.any { it == "  server.port=8080" })
        assertTrue(
            messages.any {
                it.contains("server.port=18080") && it.contains("overridden by configmap.skeleton.ns")
            },
        )
    }

    @Test
    fun `skips noisy system sources unless explicitly enabled`() {
        val environment = StandardEnvironment()

        printWith(environment)
        assertFalse(printedMessages().any { it.contains("source: systemEnvironment") })

        appender.list.clear()
        environment.propertySources.addFirst(
            MapPropertySource("override", mapOf("app.config.print-system-properties-on-startup" to "true")),
        )
        printWith(environment)
        assertTrue(printedMessages().any { it.contains("source: systemEnvironment") })
    }

    @Test
    fun `does not fail when placeholder cannot be resolved`() {
        val environment = StandardEnvironment()
        environment.propertySources.addFirst(
            MapPropertySource("configmap.skeleton.ns", mapOf("app.namespace" to "\${UNDEFINED_NAMESPACE}")),
        )

        printWith(environment)

        assertTrue(printedMessages().any { it.contains("app.namespace") && it.contains("placeholder 해석 실패") })
    }

    private fun printWith(environment: StandardEnvironment) {
        LoadedPropertiesPrinter(environment).printLoadedProperties()
    }

    private fun printedMessages(): List<String> = appender.list.map { it.formattedMessage }
}
