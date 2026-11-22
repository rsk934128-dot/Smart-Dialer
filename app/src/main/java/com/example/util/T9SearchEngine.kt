package com.example.util

import com.example.data.model.ContactEntity

object T9SearchEngine {

    val DIGIT_TO_LETTERS_MAP: Map<Char, String> = mapOf(
        '2' to "ABC",
        '3' to "DEF",
        '4' to "GHI",
        '5' to "JKL",
        '6' to "MNO",
        '7' to "PQRS",
        '8' to "TUV",
        '9' to "WXYZ"
    )

    val CHAR_TO_DIGIT_MAP: Map<Char, Char> = buildMap {
        DIGIT_TO_LETTERS_MAP.forEach { (digit, letters) ->
            letters.forEach { ch ->
                put(ch.lowercaseChar(), digit)
            }
        }
    }

    fun getLettersForDigit(digit: Char): String {
        return DIGIT_TO_LETTERS_MAP[digit] ?: ""
    }

    fun describeQueryT9Letters(query: String): List<Pair<Char, String>> {
        return query.mapNotNull { ch ->
            val letters = DIGIT_TO_LETTERS_MAP[ch]
            if (letters != null) ch to letters else null
        }
    }

    fun nameToT9Digits(name: String): String {
        val sb = StringBuilder()
        for (ch in name.lowercase()) {
            val digit = CHAR_TO_DIGIT_MAP[ch]
            if (digit != null) {
                sb.append(digit)
            } else if (ch.isDigit()) {
                sb.append(ch)
            } else {
                sb.append(' ')
            }
        }
        return sb.toString()
    }

    data class T9MatchResult(
        val contact: ContactEntity,
        val matchType: MatchType,
        val matchedSubtext: String = "",
        val matchedNameIndices: Set<Int> = emptySet(),
        val matchedNumberRange: IntRange? = null,
        val score: Int = 0
    )

    enum class MatchType {
        NAME,
        NUMBER
    }

    fun filterContacts(query: String, contacts: List<ContactEntity>): List<T9MatchResult> {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) {
            return contacts.map { T9MatchResult(it, MatchType.NAME) }
        }

        val normalizedDigitQuery = cleanQuery.filter { it.isDigit() || it == '+' }
        val results = mutableListOf<T9MatchResult>()

        for (contact in contacts) {
            // 1. Check T9 & Name matches first so alphabet searches rank naturally
            val nameMatch = findNameMatch(cleanQuery, contact.name)
            val numberMatchRange = findPhoneNumberMatch(cleanQuery, normalizedDigitQuery, contact.phoneNumber)

            if (nameMatch != null) {
                results.add(
                    T9MatchResult(
                        contact = contact,
                        matchType = MatchType.NAME,
                        matchedSubtext = cleanQuery,
                        matchedNameIndices = nameMatch.first,
                        matchedNumberRange = numberMatchRange,
                        score = nameMatch.second
                    )
                )
                continue
            }

            if (numberMatchRange != null) {
                results.add(
                    T9MatchResult(
                        contact = contact,
                        matchType = MatchType.NUMBER,
                        matchedSubtext = cleanQuery,
                        matchedNumberRange = numberMatchRange,
                        score = if (numberMatchRange.first == 0) 60 else 40
                    )
                )
            }
        }

        return results.sortedByDescending { it.score }
    }

    private fun findNameMatch(query: String, name: String): Pair<Set<Int>, Int>? {
        if (name.isBlank()) return null

        // Direct substring match when letters are entered
        val directIdx = name.indexOf(query, ignoreCase = true)
        if (directIdx >= 0 && query.any { it.isLetter() }) {
            val indices = (directIdx until (directIdx + query.length)).toSet()
            val score = if (directIdx == 0) 120 else 95
            return indices to score
        }

        val digitQuery = query.filter { it in '2'..'9' || it.isDigit() }
        if (digitQuery.isEmpty() || digitQuery.length != query.length) return null

        val t9Full = nameToT9Digits(name)

        // Check substring in spaced T9 string (prefers word start)
        var bestIdx = -1
        var bestScore = -1
        var searchFrom = 0
        while (searchFrom <= t9Full.length - digitQuery.length) {
            val idx = t9Full.indexOf(digitQuery, searchFrom)
            if (idx < 0) break
            val atStart = idx == 0
            val atWordBoundary = idx > 0 && t9Full[idx - 1] == ' '
            val score = when {
                atStart -> 110
                atWordBoundary -> 100
                else -> 70
            }
            if (score > bestScore) {
                bestScore = score
                bestIdx = idx
            }
            searchFrom = idx + 1
        }
        if (bestIdx >= 0) {
            return (bestIdx until (bestIdx + digitQuery.length)).toSet() to bestScore
        }

        // Check space-stripped continuous T9 match (e.g., "74345432" -> "Sheikh Farid")
        val compactPairs = mutableListOf<Pair<Int, Char>>()
        t9Full.forEachIndexed { originalIndex, ch ->
            if (ch != ' ') compactPairs.add(originalIndex to ch)
        }
        val compactT9 = compactPairs.map { it.second }.joinToString("")
        val compactIdx = compactT9.indexOf(digitQuery)
        if (compactIdx >= 0) {
            val indices = (compactIdx until (compactIdx + digitQuery.length))
                .map { compactPairs[it].first }
                .toSet()
            val score = if (compactIdx == 0) 90 else 65
            return indices to score
        }

        // Check word initials / acronym T9 match (e.g., "73" -> "Sheikh Farid")
        val wordInitialIndices = mutableListOf<Int>()
        for (i in t9Full.indices) {
            if (t9Full[i] != ' ' && (i == 0 || t9Full[i - 1] == ' ')) {
                wordInitialIndices.add(i)
            }
        }
        val initialsT9 = wordInitialIndices.map { t9Full[it] }.joinToString("")
        val initialMatchIdx = initialsT9.indexOf(digitQuery)
        if (digitQuery.length >= 2 && initialMatchIdx >= 0) {
            val indices = (initialMatchIdx until (initialMatchIdx + digitQuery.length))
                .map { wordInitialIndices[it] }
                .toSet()
            return indices to 85
        }

        return null
    }

    private fun findPhoneNumberMatch(rawQuery: String, normalizedQuery: String, phoneNumber: String): IntRange? {
        if (normalizedQuery.isEmpty()) return null
        val digitPairs = mutableListOf<Pair<Int, Char>>()
        phoneNumber.forEachIndexed { idx, ch ->
            if (ch.isDigit() || ch == '+') {
                digitPairs.add(idx to ch)
            }
        }
        val cleanPhone = digitPairs.map { it.second }.joinToString("")
        val matchIdx = cleanPhone.indexOf(normalizedQuery)
        if (matchIdx >= 0) {
            val startOriginal = digitPairs[matchIdx].first
            val endOriginal = digitPairs[matchIdx + normalizedQuery.length - 1].first
            return startOriginal..endOriginal
        }
        val rawIdx = phoneNumber.indexOf(rawQuery)
        if (rawIdx >= 0) {
            return rawIdx until (rawIdx + rawQuery.length)
        }
        return null
    }
}
