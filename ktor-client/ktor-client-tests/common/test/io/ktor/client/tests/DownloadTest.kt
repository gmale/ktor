/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.tests

import io.ktor.client.request.*
import io.ktor.client.tests.utils.*
import io.ktor.utils.io.*
import kotlin.test.*

class DownloadTest : ClientLoader() {
    @Test
    fun testDownloadGoogle() = clientTests {
        test { client ->
            val response = client.get<String>("http://www.google.com/")
            assertTrue { response.isNotEmpty() }
        }
    }

    @Test
    fun testLocalhostEcho() = clientTests {
        val text = "Hello, world"
        test { client ->
            val response = client.post<String>("$TEST_SERVER/echo") {
                body = text
            }

            assertEquals(text, response)
        }
    }

    @Test
    fun testEchoWithChannelBody() = clientTests {
        test { client ->
            val channel = client.get<ByteReadChannel>("http://www.google.com/")
            val size = channel.readRemaining().remaining
            assertTrue(size > 0)
        }
    }

    @Test
    fun testDownload8175() = clientTests {
        test { client ->
            repeat(100) {
                val uri =
                    "https://raw.githubusercontent.com/ssp/ktor/4f1986df68e3594714ea12949c8af8274be99d01/ktor-client/ktor-client-apache/jvm/test/io/ktor/client/engine/apache/8175.txt"
                client.get<String>(uri)
            }
        }
    }
}
