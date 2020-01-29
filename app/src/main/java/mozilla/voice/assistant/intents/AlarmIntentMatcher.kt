package mozilla.voice.assistant.intents

import android.content.Intent
import android.provider.AlarmClock
import androidx.annotation.VisibleForTesting
import java.lang.NumberFormatException
import mozilla.voice.assistant.IntentMatcher
import mozilla.voice.assistant.IntentMatcherResult

class AlarmIntentMatcher : IntentMatcher {
    private fun calculateHour(hour: String, period: String): Int {
        // Note: Originally I used AlarmClock.EXTRA_IS_PM, but it is only used
        // when matching existing alarms, not when possibly creating them.
        var h = hour.toInt()
        if (period.trim().isNotEmpty()) {
            return when (period.trim()[0]) {
                'p' -> (h % HOURS_PER_PERIOD) + HOURS_PER_PERIOD
                // special case for 12 am = midnight
                // https://www.wikiwand.com/en/12-hour_clock#/Confusion_at_noon_and_midnight
                'a' -> if (h == 12) 0 else h
                'm' -> 0 // midnight
                'n' -> 12 // noon
                else -> h // should not happen
            }
        }
        return if (h == HOURS_PER_DAY) 0 else h
    }

    private fun process(
        transcript: String,
        hour: String,
        minutes: String,
        period: String
    ): List<IntentMatcherResult> {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM)
        try {
            val h = calculateHour(hour, period)
            val m = if (minutes.isEmpty()) 0 else minutes.toInt()
            if (h in MIN_HOUR..MAX_HOUR &&
                m in MIN_MINUTE..MAX_MINUTE
            ) {
                intent.putExtra(AlarmClock.EXTRA_HOUR, h)
                intent.putExtra(AlarmClock.EXTRA_MINUTES, m)
                val confidence =
                    if (period.isEmpty() && minutes.isEmpty()) {
                        CONFIDENCE_WITH_H
                    } else if (minutes.isEmpty()) {
                        CONFIDENCE_WITH_HP
                    } else {
                        CONFIDENCE_WITH_HM_
                    }
                return listOf(
                    IntentMatcherResult(confidence, "set alarm", transcript, intent)
                )
            }
        } catch (e: NumberFormatException) {
            // No match if we couldn't interpret hours and minutes as integers.
            // This should be impossible, since they matched \d+.
        }
        return emptyList()
    }

    override fun matchTranscript(transcript: String): List<IntentMatcherResult> =
        interpreters.flatMap { matchTranscript(it, transcript) }

    private fun matchTranscript(
        interpreter: Interpreter,
        transcript: String
    ): List<IntentMatcherResult> {
        interpreter.regex.find(transcript)?.let {
            return matchResultToList(interpreter, transcript, it)
        }
        return emptyList()
    }

    private fun matchResultToList(
        interpreter: Interpreter,
        transcript: String,
        matchResult: MatchResult?
    ): List<IntentMatcherResult> {
        if (matchResult != null) {
            val (hour, minutes, period) = interpreter.f(matchResult)
            return process(transcript, hour, minutes, period)
        }
        return emptyList()
    }

    data class Interpreter(
        val regex: Regex,
        val f: (MatchResult) -> Triple<String, String, String>
    )

    companion object {
        private const val MIN_HOUR = 0
        private const val MAX_HOUR = 23
        private const val MIN_MINUTE = 0
        private const val MAX_MINUTE = 59
        private const val HOURS_PER_PERIOD = 12 // AM/PM
        private const val HOURS_PER_DAY = 24
        @VisibleForTesting
        const val CONFIDENCE_WITH_HM_ = 1.0 // hours and minutes, with or without period
        @VisibleForTesting
        const val CONFIDENCE_WITH_HP = .95 // hours and period, no minute
        @VisibleForTesting
        const val CONFIDENCE_WITH_H = .9 // hours, no minute or period

        val interpreters = listOf(
            Interpreter(
                Regex("""set alarm for (\d+)[ :](\d+)?(\s?[ap].m.)?$""")
            ) { mr: MatchResult ->
                mr.let {
                    val (hour, min, period) = it.destructured
                    Triple(hour, min, period)
                }
            },

            Interpreter(
                Regex("""set alarm for (12 ?)noon""")
            ) { _: MatchResult -> Triple("12", "0", "noon") },

            Interpreter(
                Regex("""set alarm for (12 ?)midnight""")
            ) { _: MatchResult -> Triple("12", "0", "midnight") }
        )
    }
}
