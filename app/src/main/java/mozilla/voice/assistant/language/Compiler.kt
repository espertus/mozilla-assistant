package mozilla.voice.assistant.language

import androidx.annotation.VisibleForTesting

/**
 * Compiler for converting phrases into [Pattern]s.
 */
class Compiler {
    companion object {
        // matches something like: [direction=up]
        private val parameterRegex = Regex("""^\[(\w+)\s*=\s*(\w+)\s*](.*)$""")

        // matches something like: [number:smallNumber]
        private val typedSlotRegex = Regex("""^\[(\w+)\s*:l\s*(\w+)\s*](.*)$""")

        // matches something like: [query]
        private val untypedSlotRegex = Regex("""^\[(\w+)\w*](.*)$""")

        // matches something like: (find | search | look for |)
        private val alternativesRegex = Regex("""^\(([^)]*)\)(.*)$""")

        // matches something like: seek{s}
        private val altWordRegex = Regex("""\{([^}]+)}""")

        // matches everything before the next left parenthesis/brace and the remainder
        private val wordsRegex = Regex("""\s*([^(\[]+)(..*)""")

        @VisibleForTesting
        internal fun getParameter(phrase: String): Triple<String, String, String>? =
            parameterRegex.matchEntire(phrase)?.run {
                require(groupValues.size == 4)
                val (parameter, value, rest) = destructured
                Triple(parameter, value, rest)
            }

        // Entities should be processed by this method before being passed to compile().
        internal fun convertEntities(entityMapping: Map<String, List<String>>) =
            entityMapping.entries.associate { (key, value) ->
                key to Alternatives(value.map { makeWordMatcher(it) })
            }

        private fun makeWordMatcher(s: String): Pattern {
            val list = s.toWordList()
            return if (list.size == 1) {
                list[0]
            } else {
                Sequence(list)
            }
        }

        @VisibleForTesting
        internal fun splitPhraseLines(s: String): List<String> =
            s.split("\n")
                .map { it.trim() }
                .filterNot { it.isEmpty() || it.startsWith("#") || it.startsWith("//") }

        @VisibleForTesting
        internal fun splitAlternatives(s: String) =
            altWordRegex.find(s)?.run {
                listOf(
                    s.replace(altWordRegex, ""), // omit optional bit
                    s.replace(altWordRegex, this.groupValues[1]) // include optional bit
                )
            } ?: listOf(s)

        internal fun compile(
            string: String,
            entities: Map<String, Pattern>? = null,
            intentName: String? = null
        ): Pattern {
            var toParse = string
            val parameters = mutableMapOf<String, String>()
            val seq = mutableListOf<Pattern>()

            while (toParse.isNotEmpty()) {
                // Check for parameter, such as [direction=up]
                val parameterMatch = parameterRegex.matchEntire(toParse)
                if (parameterMatch != null) {
                    val (name, value, rest) = parameterMatch.destructured
                    parameters[name] = value
                    toParse = rest
                    continue
                }

                // Check for typed slot, such as [number:smallNumber]
                val typedSlotMatch = typedSlotRegex.matchEntire(toParse)
                if (typedSlotMatch != null) {
                    val (slotName, entityName, rest) = typedSlotMatch.destructured
                    val entityPattern =
                        entities?.get(entityName) ?: throw Error("No entity type $entityName")
                    seq.add(Slot(entityPattern, slotName = slotName))
                    toParse = rest
                    continue
                }

                // Check for untyped slot, such as [query]
                val untypedSlotMatch = untypedSlotRegex.matchEntire(toParse)
                if (untypedSlotMatch != null) {
                    val (slotName, rest) = untypedSlotMatch.destructured
                    seq.add(Slot(Wildcard(), slotName = slotName))
                    toParse = rest
                    continue
                }

                // Check for conjunction, such as (search | find | look for)
                if (toParse.startsWith("(")) {
                    val alternativesMatch = alternativesRegex.matchEntire(toParse)
                    if (alternativesMatch != null) {
                        val (altString, rest) = alternativesMatch.destructured
                        val alts = altString.split("|").map { it.trim() }
                        val patterns = alts
                            .flatMap { splitAlternatives(it) }
                            .map { makeWordMatcher(it) }
                        seq.add(Alternatives(patterns, empty = alts.contains("")))
                        toParse = rest
                        continue
                    }
                }

                // Check for words before the next left parenthesis or left bracket.
                wordsRegex.matchEntire(toParse)?.run {
                    val (wordsString, rest) = destructured
                    wordsString.split("\\s+").forEach {
                        val alts = splitAlternatives(wordsString)
                        seq.add(
                            when (alts.size) {
                                1 -> Word(alts[0])
                                2 -> Alternatives(alts.map { Word(it) })
                                else -> throw Error("Illegal value returned from splitAlterrnatives(\"$wordsString\': ${alts.size}")
                            }
                        )
                    }
                    toParse = rest
                } ?: throw Error("No case matched in compile() for: \"$toParse\"")
            }

            return FullPhrase(
                Sequence(seq),
                originalSource = string,
                intentName = intentName,
                parameters = parameters
            )
        }
    }
}
