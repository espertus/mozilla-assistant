package mozilla.voice.assistant.intents

import android.provider.AlarmClock
import mozilla.voice.assistant.intents.AlarmIntentMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest= Config.NONE)
class AlarmIntentMatcherTest {
    private val matcher = AlarmIntentMatcher()

    @Test
    fun matches3PM() {
        val results = matcher.matchTranscript("set alarm for 3 p.m.")
        assertEquals(1, results.size)
        val result = results[0]
        assertEquals(AlarmIntentMatcher.CONFIDENCE_WITH_HP, result.score, DELTA)
        val intent = result.intent
        assertEquals(15, intent.getIntExtra(AlarmClock.EXTRA_HOUR, -1))
        assertEquals(0, intent.getIntExtra(AlarmClock.EXTRA_MINUTES, -1))
    }

    companion object {
        private const val DELTA = .01
    }
}
