
package com.example.gloveworks30

fun generateUserId(
    isAnonymous: Boolean,
    firstName: String = "",
    lastName: String = "",
    dob: String = "" // Expecting format: yyyy-mm-dd or similar
): String {
    return if (isAnonymous) {
        val letters = (1..4).map { ('A'..'Z').random() }.joinToString("")
        val numbers = (100..999).random().toString()
        "glw_anon_${letters}${numbers}"
    } else {
        val fPart = firstName.take(3).padEnd(3, '-').lowercase()
        val lPart = lastName.take(3).padEnd(3, '-').lowercase()

        // Extract numbers from dob (e.g., "2001-04-17" -> "20010417")
        val dobDigits = dob.filter { it.isDigit() }.take(8).padEnd(8, '0')

        "glw_${fPart}_${lPart}${dobDigits}"
    }
}
