@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.ditchoom.mqtt5.controlpacket.properties

import com.ditchoom.buffer.WriteBuffer

data class AssignedClientIdentifier(val value: CharSequence) : Property(0x12, Type.UTF_8_ENCODED_STRING) {
    override fun write(buffer: WriteBuffer) = write(buffer, value)
    override fun size() = size(value)
}
