package org.maspitzner.presencesimulation.utils.datahandling

/*
 * @(#)PowerLaw.java    ver 1.2  6/20/2005
 *
 * Modified by Weishuai Yang (wyang@cs.binghamton.edu).
 *
 * this file is based on T J Finney's Manuscripts Simulation Tool, 2001
 *
 * http://www.cs.binghamton.edu/~wyang/gps/javadoc/gps/util/PowerLaw.html
 */

import java.util.*
import kotlin.math.pow

/**
 * provides power law selection. Modified by Weishuai Yang this file is based on
 * T J Finney's Manuscripts Simulation Tool, 2001
 */
class PowerLaw {

    private var rand: Random? = null

    /**
     * constructs a power law object using an external random generator
     *
     * @param r
     * random generator passed in
     */
    constructor(r: Random) {
        rand = r
    }

    /**
     * constructs a power law object using an internal random generator
     */
    constructor() {
        rand = Random()
    }

    /**
     * get uniformly distributed double in [0, 1]
     */
    fun getRand(): Double {
        return rand!!.nextDouble()
    }

    /**
     * get uniformly distributed integer in [0, N - 1]
     */
    fun getRandInt(N: Int): Int {
        return rand!!.nextInt(N)
    }

    /**
     * selects item using power law probability of selecting array item: p(ni) =
     * k * (ni^p) k is a normalisation constant p(ni) = 0 if ni is zero, even
     * when p < 0
     *
     *
     * @param nums
     * array of numbers ni
     * @param p
     * exponent p
     * @return index in [0, array size - 1]
     */

    fun select(nums: DoubleArray, p: Double): Int {
        // make array of probabilities
        val probs = DoubleArray(nums.size)
        for (i in probs.indices) {
            if (nums[i] == 0.0)
                probs[i] = 0.0
            else
                probs[i] = nums[i].pow(p)
        }

        // sum probabilities
        var sum = 0.0
        for (i in probs.indices) {
            sum += probs[i]
        }

        // obtain random number in range [0, sum]
        var r = sum * getRand()

        // subtract probs until result negative
        // no of iterations gives required index
        var i = 0
        while (i < probs.size) {
            r -= probs[i]
            if (r < 0) {
                break
            }
            i++
        }
        return i
    }

    /**
     * select item using Zipf's law
     *
     * @param size
     * of ranked array
     * @return index in [0, array size - 1]
     */
    fun getZipfInt(size: Int): Int {
        // make array of numbers
        val nums = DoubleArray(size)
        for (i in nums.indices) {
            nums[i] = (i + 1).toDouble()
        }
        // get index using special case of power law
        return select(nums, -1.0)
    }
}