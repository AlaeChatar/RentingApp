package com.example.revice.models

data class Device (
    val deviceId: String,
    val deviceName: String,
    val devicePrice: Double = 0.0,
    val deviceType: String,
    val imageUrl: String? = null
)

enum class DeviceType {Gardening, Cooking, Cleaning}