package com.martdev.service.notification

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import com.martdev.repository.DbResult
import com.martdev.repository.creator_repo.CreatorRepository
import org.koin.core.annotation.Single

@Single
class FirebaseNotificationServiceImpl(
    private val repository: CreatorRepository
) : NotificationService {
    override suspend fun notifyFollowers(creatorId: Long, articleTitle: String) {
        val result = repository.getFollowersByCreatorId(creatorId)
        if (result !is DbResult.Success) return

        val tokens = result.value.map { it.fcmToken }
        if (tokens.isEmpty()) return

        val message = MulticastMessage.builder()
            .setNotification(
                Notification.builder()
                    .setTitle("New article published")
                    .setBody(articleTitle)
                    .build()
            ).addAllTokens(tokens)
            .build()
        FirebaseMessaging.getInstance().sendEachForMulticast(message)
    }
}