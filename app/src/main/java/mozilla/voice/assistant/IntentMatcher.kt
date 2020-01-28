package mozilla.voice.assistant

interface IntentMatcher {
    fun matchTranscript(transcript: String): List<IntentMatcherResult>
}
