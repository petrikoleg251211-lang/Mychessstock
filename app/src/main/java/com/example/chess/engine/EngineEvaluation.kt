package com.example.chess.engine

data class EngineEvaluation(
    val depth: Int = 0,
    val seldepth: Int = 0,
    val scoreCp: Int? = null,
    val scoreMate: Int? = null,
    val nodes: Long = 0L,
    val nps: Long = 0L,
    val timeMs: Long = 0L,
    val pv: List<String> = emptyList(),
    val bestMoveUci: String? = null,
    val multipv: Int = 1
) {
    val formattedScore: String
        get() = when {
            scoreMate != null -> if (scoreMate > 0) "#+$scoreMate" else "#$scoreMate"
            scoreCp != null -> {
                val pawnScore = scoreCp / 100.0
                if (pawnScore > 0) "+%.2f".format(pawnScore) else "%.2f".format(pawnScore)
            }
            else -> "0.00"
        }

    val numericCpValue: Int
        get() = when {
            scoreMate != null -> if (scoreMate > 0) 10000 - scoreMate * 100 else -10000 - scoreMate * 100
            scoreCp != null -> scoreCp
            else -> 0
        }
}

data class EngineSettings(
    val depth: Int = 22,
    val timeLimitPerMoveMs: Long = 1500L,
    val threads: Int = 4,
    val hashSizeMb: Int = 128,
    val multiPv: Int = 1,
    val profileName: String = "Deep Grandmaster"
)
