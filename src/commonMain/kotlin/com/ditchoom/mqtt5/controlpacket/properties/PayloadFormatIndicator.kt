@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_OVERRIDE")

package com.ditchoom.mqtt5.controlpacket.properties

import com.ditchoom.buffer.WriteBuffer

data class PayloadFormatIndicator(val willMessageIsUtf8: Boolean) : Property(
    0x01, Type.BYTE,
    willProperties = true
) {
    override fun size() = 2u
    override fun write(buffer: WriteBuffer) = write(buffer, willMessageIsUtf8)
}
