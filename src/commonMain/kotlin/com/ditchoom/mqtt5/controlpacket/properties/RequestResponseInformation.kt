package com.ditchoom.mqtt5.controlpacket.properties

import com.ditchoom.buffer.WriteBuffer

data class RequestResponseInformation(val requestServerToReturnInfoInConnack: Boolean) : Property(0x19, Type.BYTE) {
    override fun size() = 2u
    override fun write(buffer: WriteBuffer) = write(buffer, requestServerToReturnInfoInConnack)
}
