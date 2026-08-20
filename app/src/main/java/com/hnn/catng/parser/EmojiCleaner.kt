package com.hnn.catng.parser

object EmojiCleaner {
    // رگکس قدرتمند برای حذف تمامی کاراکترها و نمادهای ایموجی
    private val EMOJI_PATTERN = Regex(
        "[\\x{1F600}-\\x{1F64F}" + // Emoticons
        "\\x{1F300}-\\x{1F5FF}" + // Misc Symbols and Pictographs (💦, 🚀, 🔥, etc.)
        "\\x{1F680}-\\x{1F6FF}" + // Transport and Map
        "\\x{1F1E0}-\\x{1F1FF}" + // Flags
        "\\x{2600}-\\x{26FF}" +   // Misc symbols (⚡, ☕, etc.)
        "\\x{2700}-\\x{27BF}" +   // Dingbats
        "\\x{FE00}-\\x{FE0F}" +   // Variation Selectors
        "\\x{1F900}-\\x{1F9FF}" + // Supplemental Symbols and Pictographs
        "\\x{1FA00}-\\x{1FAFF}" + // Symbols and Pictographs Extended-A
        "\\x{200D}" +             // Zero Width Joiner
        "]"
    )

    fun clean(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val withoutEmojis = input.replace(EMOJI_PATTERN, "")
        // حذف فاصله‌های اضافی و کاراکترهای تزئینی ابتدایی و انتهایی
        return withoutEmojis
            .replace(Regex("^[\\s-_:•|~]+"), "")
            .replace(Regex("[\\s-_:•|~]+$"), "")
            .trim()
    }
}
