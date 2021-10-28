@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.ditchoom.mqtt5.controlpacket.properties

import com.ditchoom.buffer.PlatformBuffer
import com.ditchoom.buffer.ReadBuffer
import com.ditchoom.buffer.WriteBuffer
import com.ditchoom.buffer.allocateNewBuffer
import com.ditchoom.mqtt.MalformedPacketException
import com.ditchoom.mqtt.ProtocolError
import com.ditchoom.mqtt.controlpacket.ControlPacket.Companion.readMqttUtf8StringNotValidatedSized
import com.ditchoom.mqtt.controlpacket.ControlPacket.Companion.readVariableByteInteger
import com.ditchoom.mqtt.controlpacket.ControlPacket.Companion.writeMqttUtf8String
import com.ditchoom.mqtt.controlpacket.QualityOfService
import com.ditchoom.mqtt.controlpacket.QualityOfService.*
import com.ditchoom.mqtt.controlpacket.utf8Length

@Suppress("UNUSED_PARAMETER")
abstract class Property(val identifierByte: Byte, val type: Type, val willProperties: Boolean = false) {
    open fun write(buffer: WriteBuffer): UInt {
        return 0u
    }

    open fun size(): UInt {
        return 0u
    }

    internal fun write(buffer: WriteBuffer, boolean: Boolean): UInt {
        buffer.write(identifierByte)
        buffer.write((if (boolean) 1 else 0).toUByte())
        return 2u
    }

    fun size(number: UInt) = 5u
    fun write(bytePacketBuilder: WriteBuffer, number: UInt): UInt {
        bytePacketBuilder.write(identifierByte)
        bytePacketBuilder.write(number)
        return 5u
    }

    fun size(number: UShort) = 3u
    fun write(bytePacketBuilder: WriteBuffer, number: UShort): UInt {
        bytePacketBuilder.write(identifierByte)
        bytePacketBuilder.write(number)
        return 3u
    }

    fun size(string: CharSequence) = UShort.SIZE_BYTES.toUInt() + 1u + string.utf8Length().toUInt()

    fun write(bytePacketBuilder: WriteBuffer, string: CharSequence): UInt {
        bytePacketBuilder.write(identifierByte)
        val size = string.utf8Length().toUInt()
        bytePacketBuilder.writeMqttUtf8String(string)
        return size
    }
}

fun Collection<Property?>.addTo(map: HashMap<Int, Any>) {
    forEach {
        map.addProperty(it)
    }
}

fun HashMap<Int, Any>.addProperty(property: Property?) {
    property ?: return
    put(property.identifierByte.toInt(), property)
}

fun ReadBuffer.readPlatformBuffer(): PlatformBuffer {
    val size = readUnsignedShort().toUInt()
    val buffer = allocateNewBuffer(size)
    buffer.write(readByteArray(size))
    buffer.resetForRead()
    return buffer
}

fun ReadBuffer.readMqttProperty(): Pair<Property, Long> {
    val identifierByte = readByte().toInt()
    val property = when (identifierByte) {
        0x01 -> {
            PayloadFormatIndicator(readByte() == 1.toByte())
        }
        0x02 -> {
            MessageExpiryInterval(readUnsignedInt().toLong())
        }
        0x03 -> {
            ContentType(readMqttUtf8StringNotValidatedSized().second)
        }
        0x08 -> ResponseTopic(readMqttUtf8StringNotValidatedSized().second)
        0x09 -> CorrelationData(readPlatformBuffer())
        0x0B -> SubscriptionIdentifier(readVariableByteInteger().toLong())
        0x11 -> SessionExpiryInterval(readUnsignedInt().toLong())
        0x12 -> AssignedClientIdentifier(readMqttUtf8StringNotValidatedSized().second)
        0x13 -> ServerKeepAlive(readUnsignedShort().toInt())
        0x15 -> AuthenticationMethod(readMqttUtf8StringNotValidatedSized().second)
        0x16 -> AuthenticationData(readPlatformBuffer())
        0x17 -> {
            val uByteAsInt = readByte().toInt()
            if (!(uByteAsInt == 0 || uByteAsInt == 1)) {
                throw ProtocolError(
                    "Request Problem Information cannot have a value other than 0 or 1" +
                            "see: https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477353"
                )
            }
            RequestProblemInformation(uByteAsInt == 1)
        }
        0x18 -> WillDelayInterval(readUnsignedInt().toLong())
        0x19 -> {
            val uByteAsInt = readUnsignedByte().toInt()
            if (!(uByteAsInt == 0 || uByteAsInt == 1)) {
                throw ProtocolError(
                    "Request Response Information cannot have a value other than 0 or 1" +
                            "see: https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477352"
                )
            }
            RequestResponseInformation(uByteAsInt == 1)
        }
        0x1A -> ResponseInformation(readMqttUtf8StringNotValidatedSized().second)
        0x1C -> ServerReference(readMqttUtf8StringNotValidatedSized().second)
        0x1F -> ReasonString(readMqttUtf8StringNotValidatedSized().second)
        0x21 -> ReceiveMaximum(readUnsignedShort().toInt())
        0x22 -> TopicAlias(readUnsignedShort().toInt())
        0x23 -> TopicAliasMaximum(readUnsignedShort().toInt())
        0x24 -> MaximumQos(if (readByte() == 1.toByte()) AT_LEAST_ONCE else AT_MOST_ONCE) // Should not be present for 2
        0x25 -> RetainAvailable(readByte() == 1.toByte())
        0x26 -> UserProperty(
            readMqttUtf8StringNotValidatedSized().second,
            readMqttUtf8StringNotValidatedSized().second
        )
        0x27 -> MaximumPacketSize(readUnsignedInt().toLong())
        0x28 -> WildcardSubscriptionAvailable(readByte() == 1.toByte())
        0x29 -> SubscriptionIdentifierAvailable(readByte() == 1.toByte())
        0x2A -> SharedSubscriptionAvailable(readByte() == 1.toByte())
        else -> throw MalformedPacketException(
            "Invalid Byte Code while reading properties $identifierByte 0x${identifierByte.toString(
                16
            )}"
        )
    }
    return Pair(property, property.size().toLong() + 1)
}

fun ReadBuffer.readProperties() = readPropertiesSized().second

fun ReadBuffer.readPropertiesSized(): Pair<UInt, Collection<Property>?> {
    val propertyLength = readVariableByteInteger()
    val list = mutableListOf<Property>()
    var totalBytesRead = 0L
    while (totalBytesRead < propertyLength.toInt()) {
        val (property, bytesRead) = readMqttProperty()
        totalBytesRead += bytesRead
        list += property
    }
    return Pair(propertyLength, if (list.isEmpty()) null else list)
}