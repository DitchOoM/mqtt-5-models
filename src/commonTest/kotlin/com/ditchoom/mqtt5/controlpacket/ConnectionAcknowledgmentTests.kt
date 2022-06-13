package com.ditchoom.mqtt5.controlpacket

import com.ditchoom.buffer.PlatformBuffer
import com.ditchoom.buffer.allocate
import com.ditchoom.buffer.toBuffer
import com.ditchoom.mqtt.MalformedPacketException
import com.ditchoom.mqtt.ProtocolError
import com.ditchoom.mqtt.controlpacket.ControlPacket.Companion.readVariableByteInteger
import com.ditchoom.mqtt.controlpacket.ControlPacket.Companion.writeVariableByteInteger
import com.ditchoom.mqtt.controlpacket.QualityOfService.AT_LEAST_ONCE
import com.ditchoom.mqtt.controlpacket.format.ReasonCode.*
import com.ditchoom.mqtt.controlpacket.format.fixed.get
import com.ditchoom.mqtt5.controlpacket.ConnectionAcknowledgment.VariableHeader
import com.ditchoom.mqtt5.controlpacket.ConnectionAcknowledgment.VariableHeader.Properties
import com.ditchoom.mqtt5.controlpacket.properties.*
import kotlin.test.*

class ConnectionAcknowledgmentTests {
    @Test
    fun serializeDefaults() {
        val buffer = PlatformBuffer.allocate(6)
        val actual = ConnectionAcknowledgment()
        actual.serialize(buffer)
        buffer.resetForRead()
        // fixed header
        assertEquals(0b00100000.toUByte(), buffer.readUnsignedByte(), "byte1 fixed header")
        assertEquals(3, buffer.readVariableByteInteger(), "byte2 fixed header remaining length")
        // variable header
        assertEquals(0, buffer.readByte(), "byte0 variable header session Present Flag")
        assertEquals(SUCCESS.byte, buffer.readUnsignedByte(), "byte1 variable header connect reason code")
        assertEquals(0, buffer.readVariableByteInteger(), "property length")
    }

    @Test
    fun deserializeDefaults() {
        val buffer = PlatformBuffer.allocate(5)
        // fixed header
        buffer.write(0b00100000.toUByte())
        buffer.writeVariableByteInteger(3)
        // variable header
        buffer.write(0.toByte())
        buffer.write(SUCCESS.byte)
        buffer.writeVariableByteInteger(0)
        buffer.resetForRead()
        assertEquals(ConnectionAcknowledgment(), ControlPacketV5.from(buffer))
    }

    @Test
    fun bit0SessionPresentFalseFlags() {
        val buffer = PlatformBuffer.allocate(3)
        val model = ConnectionAcknowledgment()
        model.header.serialize(buffer)
        buffer.resetForRead()
        val sessionPresentBit = buffer.readUnsignedByte().get(0)
        assertFalse(sessionPresentBit)

        val buffer2 = PlatformBuffer.allocate(5)
        model.serialize(buffer2)
        buffer2.resetForRead()
        val result = ControlPacketV5.from(buffer2) as ConnectionAcknowledgment
        assertFalse(result.header.sessionPresent)
    }

    @Test
    fun bit0SessionPresentFlags() {
        val buffer = PlatformBuffer.allocate(3)
        val model = ConnectionAcknowledgment(VariableHeader(true))
        model.header.serialize(buffer)
        buffer.resetForRead()
        val sessionPresentBit = buffer.readUnsignedByte().get(0)
        assertTrue(sessionPresentBit)
    }

    @Test
    fun connectReasonCodeDefaultSuccess() {
        val buffer = PlatformBuffer.allocate(3)
        val model = ConnectionAcknowledgment()
        model.header.serialize(buffer)
        buffer.resetForRead()
        val sessionPresentBit = buffer.readUnsignedByte().get(0)
        assertFalse(sessionPresentBit)

        val buffer2 = PlatformBuffer.allocate(5)
        model.serialize(buffer2)
        buffer2.resetForRead()
        val result = ControlPacketV5.from(buffer2) as ConnectionAcknowledgment
        assertEquals(result.header.connectReason, SUCCESS)
    }

    @Test
    fun connectReasonCodeDefaultUnspecifiedError() {
        val buffer = PlatformBuffer.allocate(5)
        val model = ConnectionAcknowledgment(VariableHeader(connectReason = UNSPECIFIED_ERROR))
        model.serialize(buffer)
        buffer.resetForRead()
        val model2 = ControlPacketV5.from(buffer) as ConnectionAcknowledgment
        assertEquals(model, model2)
        assertEquals(UNSPECIFIED_ERROR, model2.header.connectReason)
    }

    @Test
    fun sessionExpiryInterval() {
        val actual = ConnectionAcknowledgment(VariableHeader(properties = Properties(4L)))
        val buffer = PlatformBuffer.allocate(12)
        actual.serialize(buffer)
        buffer.resetForRead()
        // fixed header
        assertEquals(0b00100000.toUByte(), buffer.readUnsignedByte(), "byte1 fixed header")
        assertEquals(8, buffer.readVariableByteInteger(), "byte2 fixed header remaining length")
        // variable header
        assertEquals(0, buffer.readByte(), "byte0 variable header session Present Flag")
        assertEquals(SUCCESS.byte, buffer.readUnsignedByte(), "byte1 variable header connect reason code")
        assertEquals(5, buffer.readVariableByteInteger(), "property length")
        assertEquals(0x11, buffer.readByte())
        assertEquals(4.toUInt(), buffer.readUnsignedInt())
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment
        assertEquals(4L, expected.header.properties.sessionExpiryIntervalSeconds)
    }

    @Test
    fun sessionExpiryIntervalMultipleTimesThrowsProtocolError() {
        val obj1 = SessionExpiryInterval(4L)
        val obj2 = obj1.copy()
        val buffer = PlatformBuffer.allocate(14)
        val size = obj1.size() + obj2.size()
        buffer.writeVariableByteInteger(size.toInt())
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
    fun receiveMaximum() {
        val actual = ConnectionAcknowledgment(VariableHeader(properties = Properties(receiveMaximum = 4)))
        val buffer = PlatformBuffer.allocate(8)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.receiveMaximum, 4)
        assertEquals(expected, actual)
    }

    @Test
    fun receiveMaximumSetToZeroThrowsProtocolError() {
        val actual = ConnectionAcknowledgment(VariableHeader(properties = Properties(receiveMaximum = 0)))
        val buffer = PlatformBuffer.allocate(8)
        actual.serialize(buffer)
        buffer.resetForRead()
        try {
            ControlPacketV5.from(buffer)
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun receiveMaximumMultipleTimesThrowsProtocolError() {
        val obj1 = ReceiveMaximum(4)
        val obj2 = obj1.copy()
        val buffer = PlatformBuffer.allocate(7)
        val size = obj1.size() + obj1.size()
        buffer.writeVariableByteInteger(size.toInt())
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
    fun maximumQos() {
        val actual = ConnectionAcknowledgment(VariableHeader(properties = Properties(maximumQos = AT_LEAST_ONCE)))
        val buffer = PlatformBuffer.allocate(8)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.maximumQos, AT_LEAST_ONCE)
        assertEquals(expected, actual)
    }

    @Test
    fun maximumQosMultipleTimesThrowsProtocolError() {
        val obj1 = MaximumQos(AT_LEAST_ONCE)
        val obj2 = obj1.copy()
        val buffer = PlatformBuffer.allocate(5)
        val size = obj1.size() + obj2.size()
        buffer.writeVariableByteInteger(size.toInt())
        obj1.write(buffer)
        obj2.write(buffer)
        buffer.resetForRead()
        try {
            VariableHeader(properties = Properties.from(buffer.readProperties()))
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun retainAvailableTrue() {
        val actual = ConnectionAcknowledgment(VariableHeader(properties = Properties(retainAvailable = true)))
        val buffer = PlatformBuffer.allocate(5)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.retainAvailable, true)
        assertEquals(expected, actual)
    }

    @Test
    fun retainAvailableFalse() {
        val actual = ConnectionAcknowledgment(VariableHeader(properties = Properties(retainAvailable = false)))
        val buffer = PlatformBuffer.allocate(7)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.retainAvailable, false)
        assertEquals(expected, actual)
    }

    @Test
    fun retainAvailableSendDefaults() {
        val actual = ConnectionAcknowledgment()
        val buffer = PlatformBuffer.allocate(5)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.retainAvailable, true)
        assertEquals(expected, actual)
    }


    @Test
    fun retainAvailableMultipleTimesThrowsProtocolError() {
        val obj1 = RetainAvailable(true)
        val obj2 = obj1.copy()
        val buffer = PlatformBuffer.allocate(5)
        val size = obj1.size() + obj2.size()
        buffer.writeVariableByteInteger(size.toInt())
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
    fun maximumPacketSize() {
        val actual = ConnectionAcknowledgment(VariableHeader(properties = Properties(maximumPacketSize = 4)))
        val buffer = PlatformBuffer.allocate(10)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.maximumPacketSize, 4)
        assertEquals(expected, actual)
    }

    @Test
    fun maximumPacketSizeSetToZeroThrowsProtocolError() {
        val actual = ConnectionAcknowledgment(VariableHeader(properties = Properties(maximumPacketSize = 0)))
        val buffer = PlatformBuffer.allocate(10)
        actual.serialize(buffer)
        buffer.resetForRead()
        try {
            ControlPacketV5.from(buffer)
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun maximumPacketSizeMultipleTimesThrowsProtocolError() {
        val obj1 = MaximumPacketSize(4)
        val obj2 = obj1.copy()
        val buffer = PlatformBuffer.allocate(11)
        val size = obj1.size() + obj2.size()
        buffer.writeVariableByteInteger(size.toInt())
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
    fun assignedClientIdentifier() {
        val actual = ConnectionAcknowledgment(
            VariableHeader(properties = Properties(assignedClientIdentifier = "yolo"))
        )
        val buffer = PlatformBuffer.allocate(12)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.assignedClientIdentifier.toString(), "yolo")
        assertEquals(expected.toString(), actual.toString())
    }

    @Test
    fun assignedClientIdentifierMultipleTimesThrowsProtocolError() {
        val obj1 = AssignedClientIdentifier("yolo")
        val obj2 = obj1.copy()
        val buffer = PlatformBuffer.allocate(15)
        val size = obj1.size() + obj2.size()
        buffer.writeVariableByteInteger(size.toInt())
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
    fun topicAliasMaximum() {
        val actual = ConnectionAcknowledgment(
            VariableHeader(properties = Properties(topicAliasMaximum = 4))
        )
        val buffer = PlatformBuffer.allocate(8)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.topicAliasMaximum, 4)
    }

    @Test
    fun topicAliasMaximumMultipleTimesThrowsProtocolError() {
        val obj1 = TopicAliasMaximum(4)
        val obj2 = obj1.copy()
        val buffer = PlatformBuffer.allocate(7)
        val size = obj1.size() + obj2.size()
        buffer.writeVariableByteInteger(size.toInt())
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
    fun reasonString() {
        val actual = ConnectionAcknowledgment(
            VariableHeader(properties = Properties(reasonString = "yolo"))
        )
        val buffer = PlatformBuffer.allocate(12)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.reasonString.toString(), "yolo")
        assertEquals(expected.toString(), actual.toString())
    }

    @Test
    fun reasonStringMultipleTimesThrowsProtocolError() {
        val obj1 = ReasonString("yolo")
        val obj2 = obj1.copy()
        val buffer = PlatformBuffer.allocate(15)
        val size = obj1.size() + obj2.size()
        buffer.writeVariableByteInteger(size.toInt())
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
    fun variableHeaderPropertyUserProperty() {
        val props = Properties.from(setOf(UserProperty("key", "value")))
        val userPropertyResult = props.userProperty
        for ((key, value) in userPropertyResult) {
            assertEquals(key, "key")
            assertEquals(value, "value")
        }
        assertEquals(userPropertyResult.size, 1)

        val connack = ConnectionAcknowledgment(VariableHeader(properties = props))
        val buffer = PlatformBuffer.allocate(18)
        connack.serialize(buffer)
        buffer.resetForRead()
        val requestRead = ControlPacketV5.from(buffer) as ConnectionAcknowledgment
        val (key, value) = requestRead.header.properties.userProperty.first()
        assertEquals(key.toString(), "key")
        assertEquals(value.toString(), "value")
    }

    @Test
    fun wildcardSubscriptionAvailable() {
        val actual =
            ConnectionAcknowledgment(VariableHeader(properties = Properties(supportsWildcardSubscriptions = false)))
        val buffer = PlatformBuffer.allocate(7)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.supportsWildcardSubscriptions, false)
    }

    @Test
    fun wildcardSubscriptionAvailableDefaults() {
        val actual = ConnectionAcknowledgment()
        val buffer = PlatformBuffer.allocate(5)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.supportsWildcardSubscriptions, true)
    }

    @Test
    fun wildcardSubscriptionAvailableMultipleTimesThrowsProtocolError() {
        val obj1 = WildcardSubscriptionAvailable(true)
        val obj2 = obj1.copy()
        val buffer = PlatformBuffer.allocate(5)
        val size = obj1.size() + obj2.size()
        buffer.writeVariableByteInteger(size.toInt())
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
    fun subscriptionIdentifierAvailableDefaults() {
        val actual = ConnectionAcknowledgment()
        val buffer = PlatformBuffer.allocate(5)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.subscriptionIdentifiersAvailable, true)
    }

    @Test
    fun subscriptionIdentifierAvailable() {
        val actual =
            ConnectionAcknowledgment(VariableHeader(properties = Properties(subscriptionIdentifiersAvailable = false)))
        val buffer = PlatformBuffer.allocate(7)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.subscriptionIdentifiersAvailable, false)
    }

    @Test
    fun subscriptionIdentifierAvailableMultipleTimesThrowsProtocolError() {
        val obj1 = SubscriptionIdentifierAvailable(true)
        val obj2 = obj1.copy()
        val buffer = PlatformBuffer.allocate(5)
        val size = obj1.size() + obj2.size()
        buffer.writeVariableByteInteger(size.toInt())
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
    fun sharedSubscriptionAvailableDefaults() {
        val actual = ConnectionAcknowledgment()
        val buffer = PlatformBuffer.allocate(5)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.sharedSubscriptionAvailable, true)
    }

    @Test
    fun sharedSubscriptionAvailable() {
        val actual =
            ConnectionAcknowledgment(VariableHeader(properties = Properties(sharedSubscriptionAvailable = false)))
        val buffer = PlatformBuffer.allocate(7)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.sharedSubscriptionAvailable, false)
    }

    @Test
    fun sharedSubscriptionAvailableMultipleTimesThrowsProtocolError() {
        val obj1 = SharedSubscriptionAvailable(true)
        val obj2 = obj1.copy()
        val buffer = PlatformBuffer.allocate(5)
        val size = obj1.size() + obj2.size()
        buffer.writeVariableByteInteger(size.toInt())
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
    fun serverKeepAlive() {
        val actual = ConnectionAcknowledgment(VariableHeader(properties = Properties(serverKeepAlive = 5)))
        val buffer = PlatformBuffer.allocate(8)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.sharedSubscriptionAvailable, true)
    }

    @Test
    fun serverKeepAliveMultipleTimesThrowsProtocolError() {
        val obj1 = ServerKeepAlive(5)
        val obj2 = obj1.copy()
        val buffer = PlatformBuffer.allocate(7)
        val size = obj1.size() + obj2.size()
        buffer.writeVariableByteInteger(size.toInt())
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
    fun responseInformation() {
        val actual = ConnectionAcknowledgment(
            VariableHeader(properties = Properties(responseInformation = "yolo"))
        )
        val buffer = PlatformBuffer.allocate(12)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.responseInformation.toString(), "yolo")
    }

    @Test
    fun responseInformationMultipleTimesThrowsProtocolError() {
        val obj1 = ResponseInformation("yolo")
        val obj2 = obj1.copy()
        val buffer = PlatformBuffer.allocate(15)
        val size = obj1.size() + obj2.size()
        buffer.writeVariableByteInteger(size.toInt())
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
    fun serverReference() {
        val actual = ConnectionAcknowledgment(
            VariableHeader(properties = Properties(serverReference = "yolo"))
        )
        val buffer = PlatformBuffer.allocate(12)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.serverReference.toString(), "yolo")
    }

    @Test
    fun serverReferenceMultipleTimesThrowsProtocolError() {
        val obj1 = ServerReference("yolo")
        val obj2 = obj1.copy()
        val buffer = PlatformBuffer.allocate(15)
        val size = obj1.size() + obj2.size()
        buffer.writeVariableByteInteger(size.toInt())
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
    fun authenticationMethodAndData() {
        val actual = ConnectionAcknowledgment(
            VariableHeader(
                properties = Properties(
                    authentication =
                    Authentication("yolo", "1234".toBuffer())
                )
            )
        )
        val buffer = PlatformBuffer.allocate(19)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as ConnectionAcknowledgment
        assertEquals(expected.header.properties.authentication?.method?.toString(), "yolo")
        assertEquals(expected.header.properties.authentication?.data, "1234".toBuffer())
    }

    @Test
    fun authenticationMethodMultipleTimesThrowsProtocolError() {
        val obj1 = AuthenticationMethod("yolo")
        val obj2 = obj1.copy()
        val buffer = PlatformBuffer.allocate(15)
        val size = obj1.size() + obj2.size()
        buffer.writeVariableByteInteger(size.toInt())
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
    fun authenticationDataMultipleTimesThrowsProtocolError() {
        val obj1 = AuthenticationData("1234".toBuffer())
        val obj2 = obj1.copy()
        val buffer = PlatformBuffer.allocate(15)
        val size = obj1.size() + obj2.size()
        buffer.writeVariableByteInteger(size.toInt())
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
    fun invalidPropertyOnVariableHeaderThrowsMalformedPacketException() {
        val method = WillDelayInterval(3)
        try {
            Properties.from(listOf(method, method))
            fail()
        } catch (e: MalformedPacketException) {
        }
    }

    @Test
    fun connectionReasonByteOnVariableHeaderIsInvalidThrowsMalformedPacketException() {
        val buffer = PlatformBuffer.allocate(2)
        buffer.write(1.toByte())
        buffer.write(SERVER_SHUTTING_DOWN.byte)
        buffer.resetForRead()
        try {
            VariableHeader.from(buffer, 2)
            fail()
        } catch (e: MalformedPacketException) {
        }
    }
}
