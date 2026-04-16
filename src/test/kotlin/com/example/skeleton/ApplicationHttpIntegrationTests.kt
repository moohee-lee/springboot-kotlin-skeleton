package com.example.skeleton

import com.example.skeleton.adapter.input.web.sample.protocol.CreateSampleRequest
import com.example.skeleton.adapter.input.web.sample.protocol.SampleResponse
import com.example.skeleton.adapter.input.web.sample.protocol.UpdateSampleRequest
import com.example.skeleton.common.constant.CommonConstant.API_VERSION_V1
import com.example.skeleton.common.errors.ApiErrorResponse
import com.example.skeleton.common.errors.CommonErrorCode
import com.example.skeleton.common.errors.ErrorSource
import com.example.skeleton.common.errors.SampleErrorCode
import com.example.skeleton.domain.sample.model.SampleStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ApplicationHttpIntegrationTests {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    @Qualifier("writeDataSource")
    private lateinit var writeDataSource: DataSource

    @Autowired
    @Qualifier("readDataSource")
    private lateinit var readDataSource: DataSource

    private lateinit var webTestClient: WebTestClient
    private lateinit var writeJdbc: JdbcTemplate
    private lateinit var readJdbc: JdbcTemplate

    @BeforeEach
    fun setUp() {
        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://127.0.0.1:$port")
            .build()
        writeJdbc = JdbcTemplate(writeDataSource)
        readJdbc = JdbcTemplate(readDataSource)
        writeJdbc.update("DELETE FROM samples")
        readJdbc.update("DELETE FROM samples")
    }

    // ── CRUD 정상 흐름 ──────────────────────────────────────────────────────

    @Test
    fun `POST creates a sample and returns it`() {
        val request = CreateSampleRequest(name = "Alice", age = 30, status = SampleStatus.ACTIVE)

        webTestClient.post()
            .uri("/sample/$API_VERSION_V1/samples")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody<SampleResponse>()
            .consumeWith { result ->
                val response = assertNotNull(result.responseBody)
                assertNotNull(response.id)
                assertEquals("Alice", response.name)
                assertEquals(30, response.age)
                assertEquals(SampleStatus.ACTIVE, response.status)
            }
    }

    @Test
    fun `GET by id returns existing sample`() {
        // GET 은 read DB 에서 조회
        readJdbc.update("INSERT INTO samples (id, name, age, status) VALUES (1, 'Bob', 25, 'active')")

        webTestClient.get()
            .uri("/sample/$API_VERSION_V1/samples/1")
            .exchange()
            .expectStatus().isOk
            .expectBody<SampleResponse>()
            .consumeWith { result ->
                val response = assertNotNull(result.responseBody)
                assertEquals(1L, response.id)
                assertEquals("Bob", response.name)
                assertEquals(25, response.age)
                assertEquals(SampleStatus.ACTIVE, response.status)
            }
    }

    @Test
    fun `GET search with query params filters results`() {
        // GET 은 read DB 에서 조회
        readJdbc.update("INSERT INTO samples (name, age, status) VALUES ('Alice', 30, 'active')")
        readJdbc.update("INSERT INTO samples (name, age, status) VALUES ('Bob', 20, 'inactive')")
        readJdbc.update("INSERT INTO samples (name, age, status) VALUES ('Charlie', 40, 'active')")

        webTestClient.get()
            .uri("/sample/$API_VERSION_V1/samples?minAge=25&maxAge=35")
            .exchange()
            .expectStatus().isOk
            .expectBodyList<SampleResponse>()
            .hasSize(1)
            .consumeWith<WebTestClient.ListBodySpec<SampleResponse>> { result ->
                val list = assertNotNull(result.responseBody)
                assertEquals("Alice", list[0].name)
                assertEquals(30, list[0].age)
                assertEquals(SampleStatus.ACTIVE, list[0].status)
            }
    }

    @Test
    fun `PUT updates an existing sample`() {
        // PUT 은 write DB 에서 수정
        writeJdbc.update("INSERT INTO samples (id, name, age, status) VALUES (1, 'Old', 10, 'active')")

        val request = UpdateSampleRequest(name = "Updated", age = 99, status = SampleStatus.INACTIVE)

        webTestClient.put()
            .uri("/sample/$API_VERSION_V1/samples/1")
            .header("X-Modified-By", "tester")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody<SampleResponse>()
            .consumeWith { result ->
                val response = assertNotNull(result.responseBody)
                assertEquals(1L, response.id)
                assertEquals("Updated", response.name)
                assertEquals(99, response.age)
                assertEquals(SampleStatus.INACTIVE, response.status)
            }
    }

    @Test
    fun `DELETE removes an existing sample`() {
        // DELETE 는 write DB 에서 삭제
        writeJdbc.update("INSERT INTO samples (id, name, age, status) VALUES (1, 'ToDelete', 1, 'active')")

        webTestClient.delete()
            .uri("/sample/$API_VERSION_V1/samples/1")
            .exchange()
            .expectStatus().isNoContent
    }

    // ── 에러 응답 ───────────────────────────────────────────────────────────

    @Test
    fun `GET by id returns 404 when sample not found`() {
        webTestClient.get()
            .uri("/sample/$API_VERSION_V1/samples/999")
            .exchange()
            .expectStatus().isNotFound
            .expectHeader().exists("X-Trace-Id")
            .expectBody<ApiErrorResponse>()
            .consumeWith { result ->
                val error = assertNotNull(result.responseBody)
                assertEquals(SampleErrorCode.SAMPLE_NOT_FOUND.code, error.code)
                assertNotNull(error.traceId)
            }
    }

    @Test
    fun `PUT returns 400 when X-Modified-By header is missing`() {
        val request = UpdateSampleRequest(name = "Test", age = 20, status = SampleStatus.ACTIVE)

        webTestClient.put()
            .uri("/sample/$API_VERSION_V1/samples/1")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody<ApiErrorResponse>()
            .consumeWith { result ->
                val error = assertNotNull(result.responseBody)
                assertEquals(CommonErrorCode.INVALID_HEADER_PARAMETER.code, error.code)
                val errors = error.errors
                assertNotNull(errors)
                assertEquals(ErrorSource.HEADER.wireName, errors[0].source)
                assertEquals("X-Modified-By", errors[0].field)
            }
    }

    @Test
    fun `PUT returns 400 when body is empty`() {
        webTestClient.put()
            .uri("/sample/$API_VERSION_V1/samples/1")
            .header("X-Modified-By", "tester")
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody<ApiErrorResponse>()
            .consumeWith { result ->
                val error = assertNotNull(result.responseBody)
                assertEquals(CommonErrorCode.EMPTY_BODY.code, error.code)
            }
    }

    @Test
    fun `POST returns validation errors when body is invalid`() {
        val request = CreateSampleRequest(name = "", age = -1, status = SampleStatus.ACTIVE)

        webTestClient.post()
            .uri("/sample/$API_VERSION_V1/samples")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody<ApiErrorResponse>()
            .consumeWith { result ->
                val error = assertNotNull(result.responseBody)
                assertEquals(CommonErrorCode.VALIDATION_FAIL.code, error.code)
                val errors = error.errors
                assertNotNull(errors)
                assertTrue(errors.isNotEmpty())
                assertTrue(errors.all { it.source == ErrorSource.BODY.wireName })
            }
    }

    @Test
    fun `DELETE returns 404 when sample not found`() {
        webTestClient.delete()
            .uri("/sample/$API_VERSION_V1/samples/999")
            .exchange()
            .expectStatus().isNotFound
            .expectBody<ApiErrorResponse>()
            .consumeWith { result ->
                val error = assertNotNull(result.responseBody)
                assertEquals(SampleErrorCode.SAMPLE_NOT_FOUND.code, error.code)
            }
    }

    @Test
    fun `GET returns 400 for invalid path variable`() {
        webTestClient.get()
            .uri("/sample/$API_VERSION_V1/samples/abc")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody<ApiErrorResponse>()
            .consumeWith { result ->
                val error = assertNotNull(result.responseBody)
                assertEquals(CommonErrorCode.INVALID_PARAMETER.code, error.code)
            }
    }

    @Test
    fun `GET returns query field name when query parameter binding fails`() {
        webTestClient.get()
            .uri("/sample/$API_VERSION_V1/samples?minAge=abc")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody<ApiErrorResponse>()
            .consumeWith { result ->
                val error = assertNotNull(result.responseBody)
                assertEquals(CommonErrorCode.INVALID_PARAMETER.code, error.code)
                val errors = error.errors
                assertNotNull(errors)
                assertEquals(ErrorSource.QUERY.wireName, errors[0].source)
                assertEquals("minAge", errors[0].field)
            }
    }

    // ── 공통 에러 ───────────────────────────────────────────────────────────

    @Test
    fun `unknown path returns not found`() {
        webTestClient.get()
            .uri("/unknown")
            .exchange()
            .expectStatus().isNotFound
            .expectHeader().exists("X-Trace-Id")
            .expectBody<ApiErrorResponse>()
            .consumeWith { result ->
                val error = assertNotNull(result.responseBody)
                assertEquals(CommonErrorCode.NOT_FOUND.code, error.code)
                assertNotNull(error.traceId)
            }
    }

    @Test
    fun `provided trace id is reused in response`() {
        val traceId = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        webTestClient.get()
            .uri("/unknown")
            .header("X-Trace-Id", traceId)
            .exchange()
            .expectStatus().isNotFound
            .expectHeader().valueEquals("X-Trace-Id", traceId)
            .expectBody<ApiErrorResponse>()
            .consumeWith { result ->
                val error = assertNotNull(result.responseBody)
                assertEquals(traceId, error.traceId)
            }
    }
}
