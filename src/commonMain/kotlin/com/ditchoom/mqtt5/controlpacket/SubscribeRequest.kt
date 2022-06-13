package com.ditchoom.mqtt5.controlpacket

import com.ditchoom.buffer.Parcelable
import com.ditchoom.buffer.ReadBuffer
import com.ditchoom.buffer.WriteBuffer
import com.ditchoom.mqtt.MalformedPacketException
import com.ditchoom.mqtt.ProtocolError
import com.ditchoom.mqtt.controlpacket.ControlPacket.Companion.readMqttUtf8StringNotValidatedSized
import com.ditchoom.mqtt.controlpacket.ControlPacket.Companion.variableByteSize
import com.ditchoom.mqtt.controlpacket.ControlPacket.Companion.writeMqttUtf8String
import com.ditchoom.mqtt.controlpacket.ControlPacket.Companion.writeVariableByteInteger
import com.ditchoom.mqtt.controlpacket.ISubscribeRequest
import com.ditchoom.mqtt.controlpacket.ISubscription
import com.ditchoom.mqtt.controlpacket.ISubscription.RetainHandling
import com.ditchoom.mqtt.controlpacket.ISubscription.RetainHandling.*
import com.ditchoom.mqtt.controlpacket.QualityOfService
import com.ditchoom.mqtt.controlpacket.format.ReasonCode
import com.ditchoom.mqtt.controlpacket.format.fixed.DirectionOfFlow
import com.ditchoom.mqtt.controlpacket.utf8Length
import com.ditchoom.mqtt5.controlpacket.SubscribeRequest.VariableHeader.Properties
import com.ditchoom.mqtt5.controlpacket.properties.Property
import com.ditchoom.mqtt5.controlpacket.properties.ReasonString
import com.ditchoom.mqtt5.controlpacket.properties.UserProperty
import com.ditchoom.mqtt5.controlpacket.properties.readPropertiesSized

/**
 * 3.8 SUBSCRIBE - Subscribe request
 *
 * The SUBSCRIBE packet is sent from the Client to the Server to create one or more Subscriptions. Each Subscription
 * registers a Client’s interest in one or more Topics. The Server sends PUBLISH packets to the Client to forward
 * Application Messages that were published to Topics that match these Subscriptions. The SUBSCRIBE packet also
 * specifies (for each Subscription) the maximum QoS with which the Server can send Application Messages to the Client.
 *
 * Bits 3,2,1 and 0 of the Fixed Header of the SUBSCRIBE packet are reserved and MUST be set to 0,0,1 and 0
 * respectively. The Server MUST treat any other value as malformed and close the Network Connection [MQTT-3.8.1-1].
 */
@Parcelize
data class SubscribeRequest(val variable: VariableHeader, override val subscriptions: Set<ISubscription>) :
    ControlPacketV5(8, DirectionOfFlow.CLIENT_TO_SERVER, 0b10), ISubscribeRequest {

    constructor(
        packetIdentifier: UShort, topic: CharSequence, qos: QualityOfService, props: Properties = Properties(),
        noLocal: Boolean = false, retainAsPublished: Boolean = false,
        retainHandling: RetainHandling = SEND_RETAINED_MESSAGES_AT_TIME_OF_SUBSCRIBE
    )
            : this(
        VariableHeader(packetIdentifier.toInt(), props),
        setOf(Subscription.from(topic, qos, noLocal, retainAsPublished, retainHandling))
    )

    constructor(
        packetIdentifier: UShort, topic: List<String>, qos: List<QualityOfService>,
        props: Properties = Properties(), noLocalList: List<Boolean>? = null,
        retainAsPublishedList: List<Boolean>? = null,
        retainHandlingList: List<RetainHandling>? = null
    )
            : this(
        VariableHeader(packetIdentifier.toInt(), props),
        Subscription.from(topic, qos, noLocalList, retainAsPublishedList, retainHandlingList)
    )

    override val packetIdentifier = variable.packetIdentifier
    override fun variableHeader(writeBuffer: WriteBuffer) = variable.serialize(writeBuffer)

    override fun expectedResponse() = SubscribeAcknowledgement(variable.packetIdentifier.toUShort(), ReasonCode.SUCCESS)
    override fun payload(writeBuffer: WriteBuffer) =
        subscriptions.forEach { (it as Subscription).serialize(writeBuffer) }

    override fun remainingLength(): Int {
        val variableSize = variable.size()
        val subSize = subscriptions.size()
        return (variableSize + subSize).toInt()
    }

    /**
     * 3.8.2 SUBSCRIBE Variable Header
     *
     * The Variable Header of the SUBSCRIBE Packet contains the following fields in the order: Packet Identifier, and
     * Properties. Section 2.2.1 provides more information about Packet Identifiers. The rules for encoding Properties
     * are described in section 2.2.2.
     *
     * Non-normative example
     *
     * Figure 3-19 shows an example of a SUBSCRIBE variable header with a Packet Identifier of 10 and no properties.
     */
    @Parcelize
    data class VariableHeader(
        val packetIdentifier: Int,
        val properties: Properties = Properties()
    ) : Parcelable {
        fun size() =
            UShort.SIZE_BYTES.toUInt() + variableByteSize(properties.size().toInt()).toUInt() + properties.size()

        fun serialize(writeBuffer: WriteBuffer) {
            writeBuffer.write(packetIdentifier.toUShort())
            properties.serialize(writeBuffer)
        }

        @Parcelize
        data class Properties(
            /**
             * 3.2.2.3.9 Reason String
             *
             * 31 (0x1F) Byte Identifier of the Reason String.
             *
             * Followed by the UTF-8 Encoded String representing the reason associated with this response. This
             * Reason String is a human readable string designed for diagnostics and SHOULD NOT be parsed by
             * the Client.
             *
             * The Server uses this value to give additional information to the Client. The Server MUST NOT send
             * this property if it would increase the size of the CONNACK packet beyond the Maximum Packet Size
             * specified by the Client [MQTT-3.2.2-19]. It is a Protocol Error to include the Reason String more
             * than once.
             *
             * Non-normative comment
             *
             * Proper uses for the reason string in the Client would include using this information in an exception
             * thrown by the Client code, or writing this string to a log.
             */
            val reasonString: CharSequence? = null,
            /**
             * 3.8.2.1.3 User Property
             *
             * 38 (0x26) Byte, Identifier of the User Property.
             *
             * Followed by a UTF-8 String Pair.
             *
             * The User Property is allowed to appear multiple times to represent multiple name, value pairs. The
             * same name is allowed to appear more than once.
             *
             * Non-normative comment
             *
             * User Properties on the SUBSCRIBE packet can be used to send subscription related properties from
             * the Client to the Server. The meaning of these properties is not defined by this specification.
             */
            val userProperty: List<Pair<CharSequence, CharSequence>> = emptyList()
        ) : Parcelable {
            val props by lazy {
                val props = ArrayList<Property>(1 + userProperty.size)
                if (reasonString != null) {
                    props += ReasonString(reasonString)
                }
                if (userProperty.isNotEmpty()) {
                    for (keyValueProperty in userProperty) {
                        val key = keyValueProperty.first
                        val value = keyValueProperty.second
                        props += UserProperty(key, value)
                    }
                }
                props
            }

            fun size(): UInt {
                var size = 0u
                props.forEach { size += it.size() }
                return size
            }

            fun serialize(buffer: WriteBuffer) {
                buffer.writeVariableByteInteger(size().toInt())
                props.forEach { it.write(buffer) }
            }

            companion object {
                fun from(keyValuePairs: Collection<Property>?): Properties {
                    var reasonString: CharSequence? = null
                    val userProperty = mutableListOf<Pair<CharSequence, CharSequence>>()
                    keyValuePairs?.forEach {
                        when (it) {
                            is ReasonString -> {
                                if (reasonString != null) {
                                    throw ProtocolError(
                                        "Reason String added multiple times see: " +
                                                "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477427"
                                    )
                                }
                                reasonString = it.diagnosticInfoDontParse
                            }
                            is UserProperty -> userProperty += Pair(it.key, it.value)
                            else -> throw MalformedPacketException("Invalid Subscribe Request property type found in MQTT properties $it")
                        }
                    }
                    return Properties(reasonString, userProperty)
                }
            }
        }

        companion object {
            fun from(buffer: ReadBuffer, remainingLength: UInt): Pair<UInt, VariableHeader> {
                val packetIdentifier = buffer.readUnsignedShort().toInt()
                var size = 2u
                return if (remainingLength == 2u) {
                    Pair(size, VariableHeader(packetIdentifier))
                } else {
                    val propsData = buffer.readPropertiesSized()
                    val props = Properties.from(propsData.second)
                    size += propsData.first + variableByteSize(propsData.first.toInt()).toUInt()
                    Pair(size, VariableHeader(packetIdentifier, props))
                }
            }
        }
    }

    companion object {
        fun from(buffer: ReadBuffer, remainingLength: UInt): SubscribeRequest {
            val header = VariableHeader.from(buffer, remainingLength)
            val subscriptions = Subscription.fromMany(buffer, remainingLength - header.first)
            return SubscribeRequest(header.second, subscriptions)
        }
    }
}

@Parcelize
data class Subscription(
    override val topicFilter: String,
    /**
     * Bits 0 and 1 of the Subscription Options represent Maximum QoS field. This gives the maximum
     * QoS level at which the Server can send Application Messages to the Client. It is a Protocol
     * Error if the Maximum QoS field has the value 3.
     */
    override val maximumQos: QualityOfService = QualityOfService.AT_LEAST_ONCE,
    /**
     * Bit 2 of the Subscription Options represents the No Local option. If the value is 1,
     * Application Messages MUST NOT be forwarded to a connection with a ClientID equal to the
     * ClientID of the publishing connection [MQTT-3.8.3-3]. It is a Protocol Error to set the No
     * Local bit to 1 on a Shared Subscription [MQTT-3.8.3-4].
     */
    override val noLocal: Boolean = false,
    /**
     * Bit 3 of the Subscription Options represents the Retain As Published option. If 1,
     * Application Messages forwarded using this subscription keep the RETAIN flag they were
     * published with. If 0, Application Messages forwarded using this subscription have the
     * RETAIN flag set to 0. Retained messages sent when the subscription is established have
     * the RETAIN flag set to 1.
     */
    override val retainAsPublished: Boolean = false,
    /**
     * Bits 4 and 5 of the Subscription Options represent the Retain Handling option. This option
     * specifies whether retained messages are sent when the subscription is established. This
     * does not affect the sending of retained messages at any point after the subscribe. If there
     * are no retained messages matching the Topic Filter, all of these values act the same. The
     * values are:
     *
     * 0 = Send retained messages at the time of the subscribe
     *
     * 1 = Send retained messages at subscribe only if the subscription does not currently exist
     *
     * 2 = Do not send retained messages at the time of the subscribe
     *
     * It is a Protocol Error to send a Retain Handling value of 3.
     */
    override val retainHandling: RetainHandling = SEND_RETAINED_MESSAGES_AT_TIME_OF_SUBSCRIBE
) : ISubscription {

    fun serialize(writeBuffer: WriteBuffer) {
        writeBuffer.writeMqttUtf8String(topicFilter)
        val qosInt = maximumQos.integerValue
        val nlShifted = (if (noLocal) 1 else 0).shl(2)
        val rapShifted = (if (retainAsPublished) 1 else 0).shl(3)
        val rH = retainHandling.value.toInt().shl(4)
        val combinedByte = (qosInt + nlShifted + rapShifted + rH).toByte()
        writeBuffer.write(combinedByte)
    }

    fun size() =
        (topicFilter.utf8Length() + UShort.SIZE_BYTES).toUInt() + Byte.SIZE_BYTES.toUInt()

    companion object {
        fun fromMany(buffer: ReadBuffer, remainingLength: UInt): Set<Subscription> {
            val subscriptions = HashSet<Subscription>()
            var bytesRead = 0u
            while (bytesRead < remainingLength) {
                val result = from(buffer)
                bytesRead += result.first
                subscriptions.add(result.second)
            }
            return subscriptions
        }

        fun from(buffer: ReadBuffer): Pair<UInt, Subscription> {
            var size = 0.toUInt()
            val topic = buffer.readMqttUtf8StringNotValidatedSized()
            size += topic.first.toUInt() + 2u
            val topicFilter = topic.second
            val subOptionsInt = buffer.readUnsignedByte().toInt()
            size += 1u
            val reservedBit7 = subOptionsInt.shr(7) == 1
            if (reservedBit7) {
                throw ProtocolError("Bit 7 in Subscribe payload is set to an invalid value (it is reserved)")
            }
            val reservedBit6 = subOptionsInt.shl(1).shr(7) == 1
            if (reservedBit6) {
                throw ProtocolError("Bit 7 in Subscribe payload is set to an invalid value (it is reserved)")
            }
            val retainHandlingBit5 = subOptionsInt.shl(2).shr(7) == 1
            val retainHandlingBit4 = subOptionsInt.shl(3).shr(7) == 1
            val retainHandling = if (retainHandlingBit5 && retainHandlingBit4) {
                throw ProtocolError("Retain Handling Value cannot be set to 3")
            } else if (retainHandlingBit5 && !retainHandlingBit4) {
                DO_NOT_SEND_RETAINED_MESSAGES
            } else if (!retainHandlingBit5 && retainHandlingBit4) {
                SEND_RETAINED_MESSAGES_AT_SUBSCRIBE_ONLY_IF_SUBSCRIBE_DOESNT_EXISTS
            } else {
                SEND_RETAINED_MESSAGES_AT_TIME_OF_SUBSCRIBE
            }
            val rapBit3 = subOptionsInt.shl(4).shr(7) == 1
            val nlBit2 = subOptionsInt.shl(5).shr(7) == 1
            val qosBit1 = subOptionsInt.shl(6).shr(7) == 1
            val qosBit0 = subOptionsInt.shl(7).shr(7) == 1
            val qos = QualityOfService.fromBooleans(qosBit1, qosBit0)
            return Pair(size, Subscription(topicFilter.toString(), qos, nlBit2, rapBit3, retainHandling))
        }

        fun from(
            topic: CharSequence,
            qos: QualityOfService,
            noLocal: Boolean = false,
            retainAsPublished: Boolean = false,
            retainHandlingList: RetainHandling =
                SEND_RETAINED_MESSAGES_AT_TIME_OF_SUBSCRIBE
        ) =
            from(
                listOf(topic),
                listOf(qos),
                listOf(noLocal),
                listOf(retainAsPublished),
                listOf(retainHandlingList)
            ).first()

        fun from(
            topics: List<CharSequence>, qos: List<QualityOfService>,
            noLocalList: List<Boolean>? = null,
            retainAsPublishedList: List<Boolean>? = null,
            retainHandlingList: List<RetainHandling>? = null
        ): Set<Subscription> {
            if (topics.size != qos.size) {
                throw IllegalArgumentException("Non matching topics collection size with the QoS collection size")
            }
            if (noLocalList != null && noLocalList.size != topics.size) {
                throw IllegalArgumentException("Non matching topics collection size with the noLocalList collection size")
            }
            if (retainAsPublishedList != null && retainAsPublishedList.size != topics.size) {
                throw IllegalArgumentException("Non matching topics collection size with the retainAsPublishedList collection size")
            }
            if (retainHandlingList != null && retainHandlingList.size != retainHandlingList.size) {
                throw IllegalArgumentException("Non matching topics collection size with the retainHandlingList collection size")
            }
            val subscriptions = mutableSetOf<Subscription>()
            topics.forEachIndexed { index, topic ->
                val noLocal = noLocalList?.get(index) ?: false
                val retainAsPublished = retainAsPublishedList?.get(index) ?: false
                val retainHandling = retainHandlingList?.get(index) ?: SEND_RETAINED_MESSAGES_AT_TIME_OF_SUBSCRIBE
                subscriptions += Subscription(topic.toString(), qos[index], noLocal, retainAsPublished, retainHandling)
            }
            return subscriptions
        }


    }
}

fun Collection<ISubscription>.size(): UInt {
    var size = 0u
    forEach { size += (it as Subscription).size() }
    return size
}
