package com.example.util

import com.example.data.model.ContactEntity

object T9SearchEngine {

    private val CHAR_TO_DIGIT_MAP = mapOf(
        'a' to '2', 'b' to '2', 'c' to '2',
        'd' to '3', 'e' to '3', 'f' to '3',
        'g' to '4', 'h' to '4', 'i' to '4',
        'j' to '5', 'k' to '5', 'l' to '5',
        'm' to '6', 'n' to '6', 'o' to '6',
        'p' to '7', 'q' to '7', 'r' to '7', 's' to '7',
        't' to '8', 'u' to '8', 'v' to '8',
        'w' to '9', 'x' to '9', 'y' to '9', 'z' to '9'
    )

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
        val matchedSubtext: String = ""
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

        val results = mutableListOf<T9MatchResult>()

        for (contact in contacts) {
            val phoneDigits = contact.phoneNumber.replace("[^0-9+]".toRegex(), "")
            // 1. Direct number substring match
            if (phoneDigits.contains(cleanQuery)) {
                results.add(T9MatchResult(contact, MatchType.NUMBER, cleanQuery))
                continue
            }

            // 2. Direct name substring match (if user typed letters)
            if (contact.name.contains(cleanQuery, ignoreCase = true)) {
                results.add(T9MatchResult(contact, MatchType.NAME, cleanQuery))
                continue
            }

            // 3. T9 conversion match for names
            val t9Name = nameToT9Digits(contact.name)
            // Match anywhere or start of word
            if (t9Name.contains(cleanQuery)) {
                results.add(T9MatchResult(contact, MatchType.NAME, cleanQuery))
            }
        }

        return results
    }
}
