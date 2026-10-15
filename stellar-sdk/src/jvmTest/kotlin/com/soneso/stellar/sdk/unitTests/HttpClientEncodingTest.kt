package com.soneso.stellar.sdk.unitTests

import com.soneso.stellar.sdk.horizon.HorizonServer
import com.soneso.stellar.sdk.rpc.SorobanServer
import com.soneso.stellar.sdk.sep.sep01.StellarToml
import com.soneso.stellar.sdk.sep.sep10.WebAuth
import com.soneso.stellar.sdk.sep.sep45.WebAuthForContracts
import com.sun.net.httpserver.HttpServer
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList
import java.util.zip.GZIPOutputStream
import kotlin.test.*

/**
 * Tests that the HTTP clients the SDK creates for itself ask for gzip and decode it, and that a
 * client supplied by the caller is left as given.
 *
 * The JVM and Android targets use the CIO engine, which sends no `Accept-Encoding` and does not
 * decode, so the SDK installs Ktor's `ContentEncoding` plugin there.
 */
class HttpClientEncodingTest {

    private fun gzip(text: String): ByteArray {
        val buffer = ByteArrayOutputStream()
        GZIPOutputStream(buffer).use { it.write(text.toByteArray()) }
        return buffer.toByteArray()
    }

    /** Serves [body] gzipped to every request and records each request's `Accept-Encoding`. */
    private class GzipServer(body: ByteArray, contentType: String) {
        val acceptEncodings = CopyOnWriteArrayList<String?>()
        private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val port: Int get() = server.address.port

        init {
            server.createContext("/") { exchange ->
                val acceptEncoding = exchange.requestHeaders.getFirst(HttpHeaders.AcceptEncoding)
                acceptEncodings.add(acceptEncoding)
                exchange.responseHeaders.add(HttpHeaders.ContentType, contentType)
                exchange.responseHeaders.add(HttpHeaders.ContentEncoding, "gzip")
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            server.start()
        }

        fun stop() = server.stop(0)
    }

    @Test
    fun testDefaultClientsAdvertiseGzip() = runTest {
        val clients = listOf(
            HorizonServer.createDefaultHttpClient(),
            HorizonServer.createSubmitHttpClient(),
            SorobanServer.defaultHttpClient(),
            WebAuth.createDefaultHttpClient(),
            WebAuthForContracts.createDefaultHttpClient(),
        )
        for (client in clients) {
            try {
                val acceptEncoding = captureFirstRequest(client).headers[HttpHeaders.AcceptEncoding]
                assertNotNull(acceptEncoding)
                assertTrue("gzip" in acceptEncoding, "Accept-Encoding was: $acceptEncoding")
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun testHorizonDecodesGzippedResponse() = runTest {
        val server = GzipServer(gzip("""{"_embedded": {"records": []}}"""), "application/hal+json")
        val horizon = HorizonServer("http://127.0.0.1:${server.port}")
        try {
            val page = horizon.ledgers().limit(5).execute()

            assertTrue(page.records.isEmpty())
            assertEquals(1, server.acceptEncodings.size)
            assertTrue("gzip" in server.acceptEncodings.single().orEmpty())
        } finally {
            horizon.close()
            server.stop()
        }
    }

    @Test
    fun testStellarTomlFetchedWithoutClientDecodesGzippedResponse() = runTest {
        val server = GzipServer(gzip("VERSION=\"2.0.0\"\n"), "text/plain")
        try {
            val toml = StellarToml.fromDomain("127.0.0.1:${server.port}")

            assertEquals("2.0.0", toml.generalInformation.version)
            assertTrue("gzip" in server.acceptEncodings.single().orEmpty())
        } finally {
            server.stop()
        }
    }

    @Test
    fun testCallerSuppliedClientIsNotModified() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request ->
            requests.add(request)
            respond(
                content = """{"_embedded": {"records": []}}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val horizon = HorizonServer("https://horizon.example.org", HttpClient(engine) {
            install(ContentNegotiation) { json(HorizonServer.defaultJson) }
        })

        horizon.ledgers().execute()

        assertNull(requests.single().headers[HttpHeaders.AcceptEncoding])
    }
}
