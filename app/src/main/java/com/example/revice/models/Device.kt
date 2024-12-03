package com.example.revice.models

data class Device (
    val deviceId: String,
    val deviceName: String,
    val devicePrice: Double = 0.0,
    val deviceType: String,
    val deviceImage: String = "" // Base64 image string
)

enum class DeviceType {Gardening, Cooking, Cleaning}