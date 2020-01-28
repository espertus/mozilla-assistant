package mozilla.voice.assistant.intents

import android.content.Intent
import android.provider.AlarmClock
import java.lang.NumberFormatException
import mozilla.voice.assistant.IntentMatcher
import mozilla.voice.assistant.IntentMatcherResult

class AlarmIntentMatcher : IntentMatcher {

    override fun matchTranscript(transcript: String): List<IntentMatcherResult> {
        regex.find(transcript)?. let {
            val (hour, minutes, period) = it.destructured
            val intent = Intent(AlarmClock.ACTION_SET_ALARM)
            try {
                // Note: Originally I used AlarmClock.EXTRA_IS_PM, but it is only used
                // when matching existing alarms, not when possibly creating them.
                var h = hour.toInt()
                if (period.trim().isNotEmpty() && period.trim()[0] == 'p' && h < HOURS_PER_PERIOD) {
                    h += HOURS_PER_PERIOD
                }
                val m = if (minutes.isEmpty()) 0 else minutes.toInt()
                if (h in MIN_HOUR..MAX_HOUR &&
                    m in MIN_MINUTE..MAX_MINUTE) {
                    intent.putExtra(AlarmClock.EXTRA_HOUR, h)
                    intent.putExtra(AlarmClock.EXTRA_MINUTES, m)
                    val confidence =
                        if (period.isEmpty() && minutes.isEmpty()) {
                            CONFIDENCE_WITH_HOUR_ONLY
                        } else {
                            CONFIDENCE_WITH_ALL_FIELDS
                        }
                    return listOf(
                        IntentMatcherResult(confidence, "set alarm", transcript, intent)
                    )
                }
            } catch (e: NumberFormatException) {
                // No match if we couldn't interpret hours and minutes as integers.
                // This should be impossible, since they matched \d+.
            }
        }
        return emptyList()
    }

    companion object {
        private const val MIN_HOUR = 0
        private const val MAX_HOUR = 23
        private const val MIN_MINUTE = 0
        private const val MAX_MINUTE = 59
        private const val HOURS_PER_PERIOD = 12 // AM/PM
        private const val CONFIDENCE_WITH_ALL_FIELDS = 1.0
        private const val CONFIDENCE_WITH_HOUR_ONLY = .9

        val regex = Regex("""set alarm for (\d+)[ :](\d+)?(\s?[ap].m.)?""")
    }
}
