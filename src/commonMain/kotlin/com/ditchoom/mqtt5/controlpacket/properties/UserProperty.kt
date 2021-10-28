package com.ditchoom.mqtt5.controlpacket.properties

import com.ditchoom.buffer.WriteBuffer
import com.ditchoom.mqtt.controlpacket.ControlPacket.Companion.writeMqttUtf8String
import com.ditchoom.mqtt.controlpacket.utf8Length

@ExperimentalUnsignedTypes
data class UserProperty(val key: CharSequence, val value: CharSequence) : Property(
    0x26,
    Type.UTF_8_STRING_PAIR, willProperties = true
) {
    override fun write(buffer: WriteBuffer): UInt {
        buffer.write(identifierByte)
        buffer.writeMqttUtf8String(key)
        buffer.writeMqttUtf8String(value)
        return size()
    }

    override fun size() = (5 + key.utf8Length() + value.utf8Length()).toUInt()
}
