package com.ditchoom.mqtt5.controlpacket

import com.ditchoom.mqtt.controlpacket.IReserved
import com.ditchoom.mqtt.controlpacket.format.fixed.DirectionOfFlow

@Parcelize
object Reserved : ControlPacketV5(0, DirectionOfFlow.FORBIDDEN), IReserved
