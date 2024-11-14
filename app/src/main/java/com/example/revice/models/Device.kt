package com.example.revice.models

data class Device (
    val deviceId: String,
    val deviceName: String,
    val devicePrice: Number = 0,
    val deviceType: String
)

enum class DeviceType {Gardening, Cooking, Cleaning}