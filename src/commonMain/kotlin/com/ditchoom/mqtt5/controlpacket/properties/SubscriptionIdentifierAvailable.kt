@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_OVERRIDE")

package com.ditchoom.mqtt5.controlpacket.properties

import com.ditchoom.buffer.WriteBuffer

data class SubscriptionIdentifierAvailable(val serverSupported: Boolean) : Property(0x29, Type.BYTE) {
    override fun size() = 2u
    override fun write(buffer: WriteBuffer) = write(buffer, serverSupported)
}
