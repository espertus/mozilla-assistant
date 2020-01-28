package mozilla.voice.assistant.intents

import android.provider.AlarmClock
import mozilla.voice.assistant.IntentMatcherResult
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AlarmIntentMatcherTest {
    private val matcher = AlarmIntentMatcher()

    private fun getScoreHourMinute(result: IntentMatcherResult): Triple<Double, Int, Int> =
        Triple(
            result.score,
            result.intent.getIntExtra(AlarmClock.EXTRA_HOUR, DEFAULT_INT_VALUE),
            result.intent.getIntExtra(AlarmClock.EXTRA_MINUTES, DEFAULT_INT_VALUE)
        )

    private fun matchScoreHourMinute(
        result: IntentMatcherResult,
        score: Double,
        hour: Int,
        minutes: Int
    ) {
        val (s, h, m) = getScoreHourMinute(result)
        assertEquals(score, s, DELTA)
        assertEquals(hour, h)
        assertEquals(minutes, m)
    }

    @Test
    fun matches3PM() {
        val results = matcher.matchTranscript("set alarm for 3 p.m.")
        assertEquals(1, results.size)
        matchScoreHourMinute(
            results[0],
            AlarmIntentMatcher.CONFIDENCE_WITH_HP,
            15,
            0
        )
    }

    companion object {
        private const val DELTA = .01
        private const val DEFAULT_INT_VALUE = -1 // default argument to Intent.getIntExtra
    }
}
