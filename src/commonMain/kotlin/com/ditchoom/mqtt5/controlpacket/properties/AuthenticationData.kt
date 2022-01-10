@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_OVERRIDE")

package com.ditchoom.mqtt5.controlpacket.properties

import com.ditchoom.buffer.ParcelablePlatformBuffer
import com.ditchoom.buffer.WriteBuffer

data class AuthenticationData(val data: ParcelablePlatformBuffer) : Property(0x16, Type.BINARY_DATA) {
    override fun size(): UInt {
        data.position(0)
        return 1u + UShort.SIZE_BYTES.toUInt() + data.remaining()
    }

    override fun write(buffer: WriteBuffer): UInt {
        buffer.write(identifierByte)
        data.position(0)
        buffer.write(data.remaining().toUShort())
        buffer.write(data)
        return size()
    }
}
