@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_OVERRIDE")

package com.ditchoom.mqtt5.controlpacket.properties

import com.ditchoom.buffer.WriteBuffer

data class ReasonString(val diagnosticInfoDontParse: CharSequence) : Property(0x1F, Type.UTF_8_ENCODED_STRING) {
    override fun write(buffer: WriteBuffer) = write(buffer, diagnosticInfoDontParse)
    override fun size() = size(diagnosticInfoDontParse)
}
