package com.soneso.stellar.sdk

import io.ktor.client.*
import io.ktor.client.plugins.compression.*

/**
 * JVM implementation of installContentEncoding.
 *
 * Installs Ktor's [ContentEncoding] plugin for gzip and deflate; `identity` is registered so a
 * response labelled `Content-Encoding: identity` passes through. The plugin leaves a request that
 * already carries `Accept-Encoding` alone, which is how server-sent event streams opt out.
 */
internal actual fun HttpClientConfig<*>.installContentEncoding() {
    install(ContentEncoding) {
        gzip()
        deflate()
        identity()
    }
}
