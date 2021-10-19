package com.andreibelous.arwar

import kotlin.random.Random

object RandomGenerator {

    fun generateSessionId(): Int = Random.nextInt(100_000, 999_999)
//    fun generateSessionId(): Int = 111_111

    fun generateUserId(): Int = Random.nextInt(0, 99_999)
}