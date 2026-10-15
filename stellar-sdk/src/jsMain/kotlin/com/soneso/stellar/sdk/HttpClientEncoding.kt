package com.soneso.stellar.sdk

import io.ktor.client.*

/**
 * JavaScript implementation of installContentEncoding.
 *
 * Does nothing: `fetch` negotiates compression and decodes the body itself.
 */
internal actual fun HttpClientConfig<*>.installContentEncoding() {}
