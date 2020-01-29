package mozilla.voice.assistant.intents

import android.provider.AlarmClock
import mozilla.voice.assistant.IntentMatcherResult
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
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
        assertEquals("Scores differ for: ${result.utterance}", score, result.score, DELTA)
        matchHourMinute(result, hour, minutes)
    }

    private fun matchHourMinute(
        result: IntentMatcherResult,
        hour: Int,
        minutes: Int
    ) {
        val (_, h, m) = getScoreHourMinute(result)
        assertEquals("Hours differ for: ${result.utterance}", hour, h)
        assertEquals("Minutes differ for: ${result.utterance}", minutes, m)
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

    @Test
    fun matches415AM() {
        val results = matcher.matchTranscript("set alarm for 4:15 a.m.")
        assertEquals(1, results.size)
        matchScoreHourMinute(
            results[0],
            AlarmIntentMatcher.CONFIDENCE_WITH_HM_,
            4,
            15
        )
    }

    @Test
    fun matches1630PM() {
        val results = matcher.matchTranscript("set alarm for 16:30 p.m.")
        assertEquals(1, results.size)
        matchScoreHourMinute(
            results[0],
            AlarmIntentMatcher.CONFIDENCE_WITH_HM_,
            16,
            30
        )
    }

    @Test
    fun matchesMidnight() {
        listOf(
            "set alarm for 12 a.m.",
            "set alarm for 24:00",
            "set alarm for 12 midnight"
        ).flatMap {
            val results = matcher.matchTranscript(it)
            if (results.isEmpty()) {
                fail("No match found for $it")
            }
            results
        }.forEach {
            matchHourMinute(it, 0, 0)
        }
    }

    @Test
    fun matchesNoon() {
        listOf(
            "set alarm for 12 p.m.",
            "set alarm for 12:00",
            "set alarm for 12 noon"
        ).flatMap {
            val results = matcher.matchTranscript(it)
            if (results.isEmpty()) {
                fail("No match found for $it")
            }
            results
        }.forEach {
            matchHourMinute(it, 12, 0)
        }
    }

    @Test
    fun doesNotMatchBadStrings() {
        listOf(
            "set alarm for tomorrow",
            "set alarm never",
            "set alarm for 27:15",
            "set alarm for 2:30 p.m. and then something",
            "set alarm for 13 midnight",
            "set alarm for 11 noon"
        ).forEach {
            assertEquals("Unexpectedly matched $it", 0, matcher.matchTranscript(it).size)
        }
    }

    companion object {
        private const val DELTA = .01
        private const val DEFAULT_INT_VALUE = -1 // default argument to Intent.getIntExtra
    }
}
