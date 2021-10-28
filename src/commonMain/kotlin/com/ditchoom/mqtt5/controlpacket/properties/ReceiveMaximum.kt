@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_OVERRIDE")

package com.ditchoom.mqtt5.controlpacket.properties

import com.ditchoom.buffer.WriteBuffer

data class ReceiveMaximum(val maxQos1Or2ConcurrentMessages: Int) : Property(0x21, Type.TWO_BYTE_INTEGER) {
    override fun size() = 3u
    override fun write(buffer: WriteBuffer) = write(buffer, maxQos1Or2ConcurrentMessages.toUShort())
}
