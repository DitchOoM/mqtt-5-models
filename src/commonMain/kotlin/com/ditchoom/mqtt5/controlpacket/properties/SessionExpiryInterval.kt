@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.ditchoom.mqtt5.controlpacket.properties

import com.ditchoom.buffer.WriteBuffer

data class SessionExpiryInterval(val seconds: Long) : Property(0x11, Type.FOUR_BYTE_INTEGER) {
    override fun size() = size(seconds.toUInt())
    override fun write(buffer: WriteBuffer) = write(buffer, seconds.toUInt())
}
