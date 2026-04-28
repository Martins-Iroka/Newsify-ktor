package com.martdev.service.notification

interface NotificationService {
    suspend fun notifyFollowers(creatorId: Long, articleTitle: String)
}