package com.example.skeleton

import com.example.skeleton.adapter.input.web.sample.protocol.SampleResponse
import com.example.skeleton.common.constant.CommonConstant.API_VERSION_V1
import com.example.skeleton.domain.sample.model.SampleStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBodyList
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ReadWriteDatabaseRoutingIntegrationTests {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    @Qualifier("writeDataSource")
    private lateinit var writeDataSource: DataSource

    @Autowired
    @Qualifier("readDataSource")
    private lateinit var readDataSource: DataSource

    private lateinit var webTestClient: WebTestClient
    private lateinit var writeJdbcTemplate: JdbcTemplate
    private lateinit var readJdbcTemplate: JdbcTemplate

    @BeforeEach
    fun setUp() {
        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://127.0.0.1:$port")
            .build()

        writeJdbcTemplate = JdbcTemplate(writeDataSource)
        readJdbcTemplate = JdbcTemplate(readDataSource)

        writeJdbcTemplate.update("DELETE FROM samples")
        readJdbcTemplate.update("DELETE FROM samples")

        writeJdbcTemplate.update("INSERT INTO samples (name, age, status) VALUES ('writer-only', 11, '${SampleStatus.ACTIVE.value}')")
        readJdbcTemplate.update("INSERT INTO samples (name, age, status) VALUES ('reader-only', 22, '${SampleStatus.ACTIVE.value}')")
    }

    @Test
    fun `GET samples reads from read database`() {
        webTestClient.get()
            .uri("/sample/$API_VERSION_V1/samples")
            .exchange()
            .expectStatus().isOk
            .expectBodyList<SampleResponse>()
            .hasSize(1)
            .consumeWith<WebTestClient.ListBodySpec<SampleResponse>> { result ->
                val list = assertNotNull(result.responseBody)
                assertEquals("reader-only", list[0].name)
                assertEquals(22, list[0].age)
                assertEquals(SampleStatus.ACTIVE, list[0].status)
            }
    }
}
