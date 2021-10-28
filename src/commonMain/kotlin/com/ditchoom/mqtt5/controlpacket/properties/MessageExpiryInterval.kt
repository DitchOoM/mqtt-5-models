@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_OVERRIDE")

package com.ditchoom.mqtt5.controlpacket.properties

import com.ditchoom.buffer.WriteBuffer

data class MessageExpiryInterval(val seconds: Long) : Property(0x02, Type.FOUR_BYTE_INTEGER, willProperties = true) {
    override fun size() = size(seconds.toUInt())
    override fun write(buffer: WriteBuffer) = write(buffer, seconds.toUInt())
}
