@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_OVERRIDE")

package com.ditchoom.mqtt5.controlpacket.properties

import com.ditchoom.buffer.WriteBuffer

data class TopicAliasMaximum(val highestValueSupported: Int) : Property(0x23, Type.TWO_BYTE_INTEGER) {
    override fun size() = 3u
    override fun write(buffer: WriteBuffer) = write(buffer, highestValueSupported.toUShort())
}
