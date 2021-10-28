@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_OVERRIDE")

package com.ditchoom.mqtt5.controlpacket.properties

import com.ditchoom.buffer.WriteBuffer

data class ServerReference(val otherServer: CharSequence) : Property(0x1C, Type.UTF_8_ENCODED_STRING) {
    override fun write(buffer: WriteBuffer) = write(buffer, otherServer)
    override fun size() = size(otherServer)
}
