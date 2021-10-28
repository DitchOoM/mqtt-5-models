@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_OVERRIDE")

package com.ditchoom.mqtt5.controlpacket.properties

import com.ditchoom.buffer.WriteBuffer

data class ResponseInformation(val requestResponseInformationInConnack: CharSequence) :
    Property(0x1A, Type.UTF_8_ENCODED_STRING) {
    override fun write(buffer: WriteBuffer) = write(buffer, requestResponseInformationInConnack)
    override fun size() = size(requestResponseInformationInConnack)
}
