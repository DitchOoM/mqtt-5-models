@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_OVERRIDE")

package com.ditchoom.mqtt5.controlpacket.properties

import com.ditchoom.buffer.WriteBuffer

data class ResponseTopic(val value: CharSequence) : Property(0x08, Type.UTF_8_ENCODED_STRING, willProperties = true) {
    override fun write(buffer: WriteBuffer) = write(buffer, value)
    override fun size() = size(value)
}
