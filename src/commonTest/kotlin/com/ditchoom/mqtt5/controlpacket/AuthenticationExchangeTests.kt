@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package com.ditchoom.mqtt5.controlpacket

import com.ditchoom.buffer.allocateNewBuffer
import com.ditchoom.buffer.toBuffer
import com.ditchoom.mqtt.MalformedPacketException
import com.ditchoom.mqtt.ProtocolError
import com.ditchoom.mqtt.controlpacket.ControlPacket.Companion.readMqttUtf8StringNotValidatedSized
import com.ditchoom.mqtt.controlpacket.ControlPacket.Companion.readVariableByteInteger
import com.ditchoom.mqtt.controlpacket.ControlPacket.Companion.writeVariableByteInteger
import com.ditchoom.mqtt.controlpacket.format.ReasonCode.BANNED
import com.ditchoom.mqtt.controlpacket.format.ReasonCode.SUCCESS
import com.ditchoom.mqtt5.controlpacket.AuthenticationExchange.VariableHeader
import com.ditchoom.mqtt5.controlpacket.AuthenticationExchange.VariableHeader.Properties
import com.ditchoom.mqtt5.controlpacket.properties.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.fail

class AuthenticationExchangeTests {

    @Test
    fun serializationByteVerification() {
        val buffer = allocateNewBuffer(14u)
        val props = Properties(Authentication("test", "".toBuffer()))
        val disconnect = AuthenticationExchange(VariableHeader(SUCCESS, props))
        disconnect.serialize(buffer)
        buffer.resetForRead()
        // fixed header
        assertEquals(0b11110000.toUByte(), buffer.readUnsignedByte(), "byte1 fixed header")
        assertEquals(12u, buffer.readVariableByteInteger(), "byte2 fixed header remaining length")
        // variable header
        assertEquals(SUCCESS.byte, buffer.readUnsignedByte(), "byte0 variable header reason code")
        assertEquals(10u, buffer.readVariableByteInteger(), "property length")
        assertEquals(0x15.toUByte(), buffer.readUnsignedByte(), "identifier of the authentication method")
        assertEquals(4u, buffer.readUnsignedShort())
        assertEquals("test", buffer.readUtf8(4u).toString(), "authentication method value")
    }

    @Test
    fun serializeDeserializeVariableHeader() {
        val buffer = allocateNewBuffer(16u)
        val variableHeader = VariableHeader(SUCCESS, Properties(Authentication("hello", "123".toBuffer())))
        variableHeader.serialize(buffer)
        buffer.resetForRead()
        assertEquals(SUCCESS.byte, buffer.readUnsignedByte(), "reason code")
        assertEquals(14u, buffer.readVariableByteInteger(), "property length")
        assertEquals(0x15, buffer.readVariableByteInteger().toInt(), "property identifier auth method")
        assertEquals("hello", buffer.readMqttUtf8StringNotValidatedSized().second.toString(), "auth method string")
        assertEquals(0x16, buffer.readVariableByteInteger().toInt(), "property identifier auth data")
        assertEquals(3u, buffer.readUnsignedShort(), "auth data size")
        assertEquals("123", buffer.readUtf8(3).toString(), "auth data payload")
    }

    @Test
    fun serializeDeserialize() {
        val buffer = allocateNewBuffer(14u)
        val props = Properties(Authentication("test", "".toBuffer()))
        val disconnect = AuthenticationExchange(VariableHeader(SUCCESS, props))
        disconnect.serialize(buffer)
        buffer.resetForRead()
        val deserialized = ControlPacketV5.from(buffer) as AuthenticationExchange
        assertEquals(deserialized.variable.reasonCode, SUCCESS)
        buffer.resetForRead()
        assertEquals(0b11110000.toUByte(), buffer.readUnsignedByte(), "control packet code")
        assertEquals(12u, buffer.readVariableByteInteger(), "remaining length")
        assertEquals(SUCCESS.byte, buffer.readUnsignedByte(), "reason code")
        assertEquals(10u, buffer.readVariableByteInteger(), "property length")
        assertEquals(0x15, buffer.readVariableByteInteger().toInt(), "property identifier auth method")
        assertEquals("test", buffer.readMqttUtf8StringNotValidatedSized().second.toString(), "auth method string")
        assertEquals(0x16, buffer.readVariableByteInteger().toInt(), "property identifier auth data")
        assertEquals(0u, buffer.readUnsignedShort(), "auth data size")
        assertEquals("", buffer.readUtf8(0).toString(), "auth data payload")
    }

    @Test
    fun serializeDeserializeInvalid() {
        try {
            VariableHeader(BANNED, Properties(Authentication("test", "".toBuffer())))
            fail()
        } catch (e: MalformedPacketException) {
        }
    }

    @Test
    fun reasonString() {
        val buffer = allocateNewBuffer(18u)
        val props = Properties(
            Authentication("2", "".toBuffer()),
            reasonString = "yolo"
        )
        val header = VariableHeader(SUCCESS, properties = props)
        val expected = AuthenticationExchange(header)
        expected.serialize(buffer)
        buffer.resetForRead()
        val actual = ControlPacketV5.from(buffer) as AuthenticationExchange
        assertEquals("yolo", actual.variable.properties.reasonString.toString())
    }

    @Test
    fun reasonStringMultipleTimesThrowsProtocolError() {
        val obj1 = ReasonString("yolo")
        val obj2 = obj1.copy()
        val buffer = allocateNewBuffer(20u)
        val size = obj1.size() + obj2.size()
        buffer.writeVariableByteInteger(size)
        obj1.write(buffer)
        obj2.write(buffer)
        buffer.resetForRead()
        try {
            Properties.from(buffer.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun variableHeaderPropertyByteValidation() {
        val props = Properties.from(
            setOf(
                AuthenticationMethod("2"),
                UserProperty("key", "value")
            )
        )
        val userPropertyResult = props.userProperty
        for ((key, value) in userPropertyResult) {
            assertEquals(key, "key")
            assertEquals(value, "value")
        }
        assertEquals(userPropertyResult.size, 1)

        val buffer = allocateNewBuffer(17u)
        AuthenticationExchange(VariableHeader(SUCCESS, properties = props)).serialize(buffer)
        buffer.resetForRead()
        // fixed header
        assertEquals(0b11110000.toUByte(), buffer.readUnsignedByte(), "byte1 fixed header")
        assertEquals(15u, buffer.readVariableByteInteger(), "byte2 fixed header remaining length")
        // variable header
        assertEquals(SUCCESS.byte, buffer.readUnsignedByte(), "byte0 variable header reason code")
        assertEquals(13u, buffer.readVariableByteInteger(), "property length")
        assertEquals(0x26.toUByte(), buffer.readUnsignedByte(), "user property flag")
        assertEquals("key", buffer.readMqttUtf8StringNotValidatedSized().second.toString(), "user property key")
        assertEquals("value", buffer.readMqttUtf8StringNotValidatedSized().second.toString(), "user property value")
    }

    @Test
    fun variableHeaderPropertyUserProperty() {
        val props = Properties.from(
            setOf(
                AuthenticationMethod("2"),
                UserProperty("key", "value")
            )
        )
        val userPropertyResult = props.userProperty
        for ((key, value) in userPropertyResult) {
            assertEquals(key, "key")
            assertEquals(value, "value")
        }
        assertEquals(userPropertyResult.size, 1)

        val buffer = allocateNewBuffer(21u)
        AuthenticationExchange(VariableHeader(SUCCESS, properties = props)).serialize(buffer)
        buffer.resetForRead()
        val requestRead = ControlPacketV5.from(buffer) as AuthenticationExchange
        val (key, value) = requestRead.variable.properties.userProperty.first()
        assertEquals(key.toString(), "key")
        assertEquals(value.toString(), "value")
    }

    @Test
    fun authMethodMultipleTimesThrowsProtocolError() {
        val obj1 = AuthenticationMethod("yolo")
        val buffer1 = allocateNewBuffer(20u)
        val remainingLength = 2u * obj1.size() + 1u
        buffer1.writeVariableByteInteger(remainingLength)
        obj1.write(buffer1)
        val obj2 = obj1.copy()
        obj2.write(buffer1)
        buffer1.resetForRead()
        assertFailsWith<ProtocolError>("should throw error because auth method is added twice") {
            Properties.from(buffer1.readProperties())
        }
    }

    @Test
    fun authDataMultipleTimesThrowsProtocolError() {
        val method = AuthenticationMethod("yolo")
        val authData = AuthenticationData("123".toBuffer())
        val buffer1 = allocateNewBuffer(20u)
        buffer1.writeVariableByteInteger(19u)
        method.write(buffer1)
        authData.write(buffer1)

        val obj2 = authData.copy()
        obj2.write(buffer1)
        buffer1.resetForRead()
        assertFailsWith<ProtocolError>("should throw error because auth data is added twice") {
            Properties.from(buffer1.readProperties())
        }
    }

    @Test
    fun invalidReasonCode() {
        try {
            VariableHeader(BANNED, Properties(Authentication("test", "".toBuffer())))
            fail()
        } catch (e: MalformedPacketException) {
        }
    }

    @Test
    fun invalidPropertyThrowsMalformedException() {
        try {
            Properties.from(setOf(WillDelayInterval(2)))
            fail()
        } catch (e: MalformedPacketException) {
        }
    }
}
