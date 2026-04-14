package com.martdev.util

import org.junit.Assert.*
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun hashPasswordAndVerifyPassword() {
        val plainPassword = "password"

        val hashedPassword = PasswordHasher.hash(plainPassword)

        assertTrue(hashedPassword.isNotEmpty())

        val isValid = PasswordHasher.verify(plainPassword, hashedPassword)

        assertTrue(isValid)
    }
}