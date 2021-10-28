@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.ditchoom.mqtt5.controlpacket.properties

import com.ditchoom.buffer.WriteBuffer

data class MaximumPacketSize(val packetSizeLimitationBytes: Long) : Property(0x27, Type.FOUR_BYTE_INTEGER) {
    override fun size() = size(packetSizeLimitationBytes.toUInt())
    override fun write(buffer: WriteBuffer) =
        write(buffer, packetSizeLimitationBytes.toUInt())
}
