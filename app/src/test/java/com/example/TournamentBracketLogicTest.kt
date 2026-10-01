package com.example

import com.example.data.entity.MatchEntity
import com.example.data.entity.RoundEntity
import com.example.data.model.DrawMode
import com.example.data.model.Participant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.log2
import kotlin.math.pow

class TournamentBracketLogicTest {

    private fun calculatePowerOfTwo(n: Int): Int {
        var p = 2
        while (p < n) {
            p *= 2
        }
        return p
    }

    private fun generateSeedPattern(size: Int): List<Int> {
        var rounds = listOf(1, 2)
        while (rounds.size < size) {
            val nextSize = rounds.size * 2
            val nextRounds = mutableListOf<Int>()
            for (seed in rounds) {
                nextRounds.add(seed)
                nextRounds.add(nextSize + 1 - seed)
            }
            rounds = nextRounds
        }
        return rounds
    }

    @Test
    fun `test bracket size calculation for power of two and non-power of two`() {
        assertEquals(4, calculatePowerOfTwo(3))
        assertEquals(4, calculatePowerOfTwo(4))
        assertEquals(8, calculatePowerOfTwo(5))
        assertEquals(8, calculatePowerOfTwo(8))
        assertEquals(16, calculatePowerOfTwo(9))
        assertEquals(16, calculatePowerOfTwo(16))
    }

    @Test
    fun `test seeded draw pattern for 8 participants`() {
        val pattern = generateSeedPattern(8)
        // Standard tournament bracket seeding: 1 vs 8, 4 vs 5, 2 vs 7, 3 vs 6
        assertEquals(listOf(1, 8, 4, 5, 2, 7, 3, 6), pattern)
    }

    @Test
    fun `test total matches for single elimination tournament`() {
        // In single elimination for N teams where N is power of 2, total matches = N - 1
        val teamsCount = 8
        val bracketSize = calculatePowerOfTwo(teamsCount)
        val totalRounds = (log2(bracketSize.toDouble())).toInt()

        var totalMatches = 0
        for (r in 0 until totalRounds) {
            val matchesInRound = bracketSize / (2.0.pow(r + 1).toInt())
            totalMatches += matchesInRound
        }

        assertEquals(7, totalMatches)
    }

    @Test
    fun `test bye matches identification when participants count is less than bracket size`() {
        val participantsCount = 6
        val bracketSize = calculatePowerOfTwo(participantsCount)
        val byesExpected = bracketSize - participantsCount
        assertEquals(2, byesExpected)
    }
}
