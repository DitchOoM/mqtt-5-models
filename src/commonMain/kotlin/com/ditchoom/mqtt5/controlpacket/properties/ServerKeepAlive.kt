@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package com.ditchoom.mqtt5.controlpacket.properties

import com.ditchoom.buffer.WriteBuffer

data class ServerKeepAlive(val seconds: Int) : Property(0x13, Type.TWO_BYTE_INTEGER) {
    override fun size() = 3u
    override fun write(buffer: WriteBuffer) = write(buffer, seconds.toUShort())
}
