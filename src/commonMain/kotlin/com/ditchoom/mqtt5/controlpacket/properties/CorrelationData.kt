package com.ditchoom.mqtt5.controlpacket.properties

import com.ditchoom.buffer.PlatformBuffer
import com.ditchoom.buffer.WriteBuffer

data class CorrelationData(val data: PlatformBuffer) :
    Property(0x09, Type.BINARY_DATA, willProperties = true) {
    override fun size(): UInt {
        data.position(0)
        return 1u + UShort.SIZE_BYTES.toUInt() + data.remaining().toUInt()
    }

    override fun write(buffer: WriteBuffer): UInt {
        buffer.write(identifierByte)
        data.position(0)
        buffer.write(data.remaining().toUShort())
        buffer.write(data)
        return size()
    }
}
