package com.ditchoom.mqtt5.controlpacket.properties

import com.ditchoom.buffer.WriteBuffer

data class SessionExpiryInterval(val seconds: Long) : Property(0x11, Type.FOUR_BYTE_INTEGER) {
    override fun size(): Int = size(seconds.toUInt())
    override fun write(buffer: WriteBuffer): Int = write(buffer, seconds.toUInt())
}
