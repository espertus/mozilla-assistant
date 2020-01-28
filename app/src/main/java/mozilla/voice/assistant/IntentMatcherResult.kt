package mozilla.voice.assistant

import android.content.Intent
import kotlin.math.sign

class IntentMatcherResult(
    val score: Double,
    val name: String,
    val utterance: String,
    val intent: Intent
) : Comparable<IntentMatcherResult> {
    override fun compareTo(other: IntentMatcherResult): Int =
        // No danger of overflow because scores are in [0..1].
        sign(this.score - other.score).toInt()
}
