package com.martdev.plugins

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.martdev.config.FirebaseConfig
import io.ktor.server.application.*
import org.koin.ktor.ext.inject
import java.io.IOException

fun Application.configureFirebase() {
    val firebaseConfig by inject<FirebaseConfig>()
    try {
        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.getApplicationDefault())
            .setProjectId(firebaseConfig.projectId)
            .build()
        FirebaseApp.initializeApp(options).options.projectId
    } catch (e: IOException) {
        e.printStackTrace()
    }
}