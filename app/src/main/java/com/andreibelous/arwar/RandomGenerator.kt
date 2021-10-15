package com.andreibelous.arwar

import kotlin.random.Random

object RandomGenerator {

//    fun generateSessionId(): Int = Random.nextInt(0, 999_999)
    fun generateSessionId(): Int = 111111
    fun generateUserId(): Int = Random.nextInt(0, 99_999)
}