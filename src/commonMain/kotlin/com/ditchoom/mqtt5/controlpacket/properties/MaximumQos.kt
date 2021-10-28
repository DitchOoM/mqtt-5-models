@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package com.ditchoom.mqtt5.controlpacket.properties

import com.ditchoom.buffer.WriteBuffer
import com.ditchoom.mqtt.MalformedPacketException
import com.ditchoom.mqtt.controlpacket.QualityOfService
import com.ditchoom.mqtt.controlpacket.QualityOfService.*

data class MaximumQos(val qos: QualityOfService) : Property(0x24, Type.BYTE) {
    override fun size() = 2u

    override fun write(buffer: WriteBuffer) = when (qos) {
        AT_MOST_ONCE -> write(buffer, false)
        AT_LEAST_ONCE -> write(buffer, true)
        EXACTLY_ONCE -> throw MalformedPacketException("Max QoS Cannot be >= 2 as defined https://docs.oasis-open.org/mqtt/mqtt/v5.0/mqtt-v5.0.html#_Toc514847957")
    }
}
