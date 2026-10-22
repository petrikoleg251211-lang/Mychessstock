package com.example.chess.rules

data class OpeningInfo(
    val eco: String,
    val nameEn: String,
    val nameUa: String
)

object OpeningBook {
    private val OPENINGS = mapOf(
        "e4" to OpeningInfo("B00", "King's Pawn Opening", "Дебют королівського пішака"),
        "e4 e5" to OpeningInfo("C20", "Open Game", "Відкритий дебют"),
        "e4 e5 Nf3" to OpeningInfo("C40", "King's Knight Opening", "Дебют королівського коня"),
        "e4 e5 Nf3 Nc6" to OpeningInfo("C44", "King's Knight Opening: Normal Variation", "Дебют королівського коня"),
        "e4 e5 Nf3 Nc6 Bb5" to OpeningInfo("C60", "Ruy Lopez (Spanish Opening)", "Іспанська партія"),
        "e4 e5 Nf3 Nc6 Bc4" to OpeningInfo("C50", "Italian Game", "Італійська партія"),
        "e4 e5 Nf3 Nc6 Bc4 Bc5" to OpeningInfo("C53", "Giuoco Piano", "Італійська партія: Джоко Піано"),
        "e4 e5 Nf3 Nc6 Bc4 Nf6" to OpeningInfo("C55", "Two Knights Defense", "Захист двох коней"),
        "e4 e5 Nf3 Nc6 d4" to OpeningInfo("C45", "Scotch Game", "Шотландська партія"),
        "e4 e5 f4" to OpeningInfo("C30", "King's Gambit", "Королівський гамбіт"),
        "e4 c5" to OpeningInfo("B20", "Sicilian Defense", "Сицилійський захист"),
        "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc6 a6" to OpeningInfo("B90", "Sicilian: Najdorf", "Сицилійський захист: Варіант Найдорфа"),
        "e4 c5 Nf3 Nc6" to OpeningInfo("B30", "Old Sicilian", "Старосицилійський захист"),
        "e4 c5 Nf3 e6" to OpeningInfo("B40", "French Sicilian", "Франко-сицилійський захист"),
        "e4 e6" to OpeningInfo("C00", "French Defense", "Французький захист"),
        "e4 e6 d4 d5" to OpeningInfo("C01", "French Defense: Main Line", "Французький захист: Головна лінія"),
        "e4 c6" to OpeningInfo("B10", "Caro-Kann Defense", "Захист Каро-Канн"),
        "e4 d6" to OpeningInfo("B07", "Pirc Defense", "Захист Пірца-Уфімцева"),
        "e4 Nf6" to OpeningInfo("B02", "Alekhine's Defense", "Захист Алехіна"),
        "e4 d5" to OpeningInfo("B01", "Scandinavian Defense", "Скандинавський захист"),
        "d4" to OpeningInfo("A40", "Queen's Pawn Opening", "Дебют ферзевого пішака"),
        "d4 d5" to OpeningInfo("D00", "Queen's Pawn Game", "Партія ферзевого пішака"),
        "d4 d5 c4" to OpeningInfo("D06", "Queen's Gambit", "Ферзевий гамбіт"),
        "d4 d5 c4 e6" to OpeningInfo("D30", "Queen's Gambit Declined", "Відхилений ферзевий гамбіт"),
        "d4 d5 c4 dxc4" to OpeningInfo("D20", "Queen's Gambit Accepted", "Прийнятий ферзевий гамбіт"),
        "d4 d5 c4 c6" to OpeningInfo("D10", "Slav Defense", "Слов'янський захист"),
        "d4 Nf6" to OpeningInfo("A45", "Indian Defense", "Індійський захист"),
        "d4 Nf6 c4 g6" to OpeningInfo("E60", "King's Indian Defense", "Староіндійський захист"),
        "d4 Nf6 c4 e6 Nf3 b6" to OpeningInfo("E12", "Queen's Indian Defense", "Новоіндійський захист"),
        "d4 Nf6 c4 e6 Nc3 Bb4" to OpeningInfo("E20", "Nimzo-Indian Defense", "Захист Німцовича"),
        "d4 Nf6 c4 c5" to OpeningInfo("A56", "Benoni Defense", "Захист Беноні"),
        "d4 f5" to OpeningInfo("A80", "Dutch Defense", "Голландський захист"),
        "c4" to OpeningInfo("A10", "English Opening", "Англійський початок"),
        "Nf3" to OpeningInfo("A04", "Réti Opening", "Початок Реті"),
        "f4" to OpeningInfo("A02", "Bird's Opening", "Дебют Берда")
    )

    fun identifyOpening(sanMoves: List<String>): OpeningInfo? {
        if (sanMoves.isEmpty()) return null
        for (len in minOf(sanMoves.size, 8) downTo 1) {
            val key = sanMoves.take(len).joinToString(" ")
            val found = OPENINGS[key]
            if (found != null) return found
        }
        return null
    }

    fun isBookMove(sanMovesSoFar: List<String>): Boolean {
        if (sanMovesSoFar.isEmpty()) return false
        val key = sanMovesSoFar.joinToString(" ")
        return OPENINGS.keys.any { it.startsWith(key) }
    }
}
