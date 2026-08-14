package nl.pixento.remindforge.domain

import kotlin.random.Random

/**
 * A [Random] whose range draws are decided by [pick] rather than by a PRNG, so a test of jittered
 * trigger times can name the instant it expects instead of whatever a particular implementation
 * happens to produce for a seed.
 */
class PinnedRandom(private val pick: (from: Long, until: Long) -> Long) : Random() {
    override fun nextBits(bitCount: Int): Int = 0
    override fun nextLong(from: Long, until: Long): Long = pick(from, until)

    companion object {
        /** Always the shortest gap the randomness allows. */
        val Shortest = PinnedRandom { from, _ -> from }

        /** Always the longest gap it allows - `until` is exclusive, hence the -1. */
        val Longest = PinnedRandom { _, until -> until - 1 }
    }
}
