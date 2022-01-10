@file:OptIn(ExperimentalMultiplatform::class)

package com.ditchoom.mqtt5.controlpacket

@OptionalExpectation
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
expect annotation class Parcelize()