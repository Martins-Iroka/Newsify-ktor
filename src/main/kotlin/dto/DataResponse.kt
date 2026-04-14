package com.martdev.dto

import kotlinx.serialization.Serializable

@Serializable
data class DataResponse<T>(
    val data: T
)
