@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_OVERRIDE")

package com.ditchoom.mqtt5.controlpacket

import com.ditchoom.buffer.ParcelablePlatformBuffer
import com.ditchoom.buffer.ReadBuffer
import com.ditchoom.mqtt.controlpacket.*
import com.ditchoom.mqtt.controlpacket.format.ReasonCode
import com.ditchoom.mqtt.topic.Filter

@Parcelize
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
        payload: ParcelablePlatformBuffer?,
        payloadFormatIndicator: Boolean,
        messageExpiryInterval: Long?,
        topicAlias: Int?,
        responseTopic: CharSequence?,
        correlationData: ParcelablePlatformBuffer?,
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

    override fun subscribe(
        packetIdentifier: Int,
        topicFilter: CharSequence,
        maximumQos: QualityOfService,
        noLocal: Boolean,
        retainAsPublished: Boolean,
        retainHandling: ISubscription.RetainHandling,
        serverReference: CharSequence?,
        userProperty: List<Pair<CharSequence, CharSequence>>
    ): ISubscribeRequest {
        val subscription = Subscription(Filter(topicFilter), maximumQos)
        return subscribe(
            packetIdentifier,
            setOf(subscription),
            serverReference,
            userProperty
        )
    }

    override fun subscribe(
        packetIdentifier: Int,
        subscriptions: Set<ISubscription>,
        serverReference: CharSequence?,
        userProperty: List<Pair<CharSequence, CharSequence>>
    ): ISubscribeRequest {
        val props = SubscribeRequest.VariableHeader.Properties(reasonString = "", userProperty = userProperty)
        val variableHeader = SubscribeRequest.VariableHeader(packetIdentifier, props)
        return SubscribeRequest(variableHeader, subscriptions)
    }

    override fun unsubscribe(
        packetIdentifier: Int,
        topics: Set<CharSequence>,
        userProperty: List<Pair<CharSequence, CharSequence>>
    ) = UnsubscribeRequest(packetIdentifier, topics, userProperty)


    override fun disconnect(
        reasonCode: ReasonCode,
        sessionExpiryIntervalSeconds: Long?,
        reasonString: CharSequence?,
        userProperty: List<Pair<CharSequence, CharSequence>>
    ): IDisconnectNotification {
        val props = DisconnectNotification.VariableHeader.Properties(
            sessionExpiryIntervalSeconds, reasonString, userProperty
        )
        return DisconnectNotification(DisconnectNotification.VariableHeader(reasonCode, props))
    }
}