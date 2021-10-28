@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_OVERRIDE")

package com.ditchoom.mqtt5.controlpacket.properties

import com.ditchoom.buffer.WriteBuffer

data class RequestProblemInformation(val reasonStringOrUserPropertiesAreSentInFailures: Boolean) :
    Property(0x17, Type.BYTE) {
    override fun size() = 2u
    override fun write(buffer: WriteBuffer) = write(buffer, reasonStringOrUserPropertiesAreSentInFailures)
}
