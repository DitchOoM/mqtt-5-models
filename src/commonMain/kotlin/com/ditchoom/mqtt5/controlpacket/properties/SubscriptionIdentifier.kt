@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_OVERRIDE")

package com.ditchoom.mqtt5.controlpacket.properties

import com.ditchoom.buffer.WriteBuffer
import com.ditchoom.mqtt.controlpacket.ControlPacket.Companion.variableByteSize
import com.ditchoom.mqtt.controlpacket.ControlPacket.Companion.writeVariableByteInteger

data class SubscriptionIdentifier(val value: Long) : Property(0x0B, Type.VARIABLE_BYTE_INTEGER) {
    override fun size() = variableByteSize(value.toUInt()) + 1u
    override fun write(buffer: WriteBuffer): UInt {
        buffer.write(identifierByte)
        buffer.writeVariableByteInteger(value.toUInt())
        return size()
    }
}
