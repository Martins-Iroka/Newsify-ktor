package com.martdev.util

import at.favre.lib.crypto.bcrypt.BCrypt

object PasswordHasher {

    private const val COST = 12  // work factor: 2^12 iterations (~250ms on modern hardware)

    fun hash(plainPassword: String): String {
        return BCrypt.withDefaults().hashToString(COST, plainPassword.toCharArray())
    }

    fun verify(plainPassword: String, hashedPassword: String): Boolean {
        return BCrypt.verifyer()
            .verify(plainPassword.toCharArray(), hashedPassword)
            .verified
    }
}
