@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")

package com.ditchoom.mqtt5.controlpacket

import com.ditchoom.buffer.allocateNewBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

class PingRequestTests {
    @Test
    fun serializeDeserialize() {
        val buffer = allocateNewBuffer(2u)
        val ping = PingRequest
        ping.serialize(buffer)
        buffer.resetForRead()
        assertEquals(12.shl(4).toByte(), buffer.readByte())
        assertEquals(0, buffer.readByte())
        buffer.resetForRead()
        val result = ControlPacketV5.from(buffer)
        assertEquals(result, ping)
    }
}
