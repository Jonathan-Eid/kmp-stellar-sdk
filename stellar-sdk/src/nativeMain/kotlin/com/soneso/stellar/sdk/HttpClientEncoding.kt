package com.soneso.stellar.sdk

import io.ktor.client.*

/**
 * Native implementation of installContentEncoding.
 *
 * Does nothing: the Darwin engine's `NSURLSession` negotiates compression and decodes the body itself.
 */
internal actual fun HttpClientConfig<*>.installContentEncoding() {}
