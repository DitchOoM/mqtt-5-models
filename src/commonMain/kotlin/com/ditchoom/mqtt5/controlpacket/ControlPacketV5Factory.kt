@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_OVERRIDE")

package com.ditchoom.mqtt5.controlpacket

import com.ditchoom.buffer.PlatformBuffer
import com.ditchoom.buffer.ReadBuffer
import com.ditchoom.mqtt.controlpacket.ControlPacketFactory
import com.ditchoom.mqtt.controlpacket.IPublishMessage
import com.ditchoom.mqtt.controlpacket.QualityOfService

object ControlPacketV5Factory : ControlPacketFactory {
    override fun from(buffer: ReadBuffer, byte1: UByte, remainingLength: UInt) =
        ControlPacketV5.from(buffer, byte1, remainingLength)


    override fun pingRequest() = PingRequest
    override fun pingResponse() = PingResponse


    override fun publish(
        dup: Boolean,
        qos: QualityOfService,
        packetIdentifier: Int?,
        retain: Boolean,
        topicName: CharSequence,
        payload: PlatformBuffer?,
        payloadFormatIndicator: Boolean,
        messageExpiryInterval: Long?,
        topicAlias: Int?,
        responseTopic: CharSequence?,
        correlationData: PlatformBuffer?,
        userProperty: List<Pair<CharSequence, CharSequence>>,
        subscriptionIdentifier: Set<Long>,
        contentType: CharSequence?
    ): IPublishMessage {
        val fixedHeader = PublishMessage.FixedHeader(dup, qos, retain)
        val properties = PublishMessage.VariableHeader.Properties(
            payloadFormatIndicator,
            messageExpiryInterval,
            topicAlias,
            responseTopic,
            correlationData,
            userProperty,
            subscriptionIdentifier,
            contentType
        )
        val variableHeader = PublishMessage.VariableHeader(topicName, packetIdentifier, properties)
        return PublishMessage(fixedHeader, variableHeader, payload)
    }
}