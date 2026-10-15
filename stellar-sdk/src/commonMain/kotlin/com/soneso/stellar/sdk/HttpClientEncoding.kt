package com.soneso.stellar.sdk

import io.ktor.client.*

/**
 * Makes an SDK-created [HttpClient] ask servers for gzip and deflate responses and decode them.
 *
 * Horizon and Soroban RPC compress responses only for clients that send `Accept-Encoding`, and
 * list pages are several times smaller compressed. Every client the SDK builds for itself calls
 * this; a client passed in by the caller is used as given.
 *
 * Only the JVM and Android targets (the CIO engine) install anything: CIO sends no
 * `Accept-Encoding` and does not decode. The Darwin (`NSURLSession`), browser and Node.js `fetch`
 * engines already negotiate compression and hand back decoded bodies, and a second decoder on
 * top of them would fail on the already-decoded body.
 */
internal expect fun HttpClientConfig<*>.installContentEncoding()
