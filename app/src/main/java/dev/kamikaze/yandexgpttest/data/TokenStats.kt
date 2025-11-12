package dev.kamikaze.yandexgpttest.data

data class TokenStats(
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val totalTokens: Int = 0
) {
    companion object {
        fun fromUsage(usage: MessageResponse.Usage?): TokenStats {
            return TokenStats(
                inputTokens = usage?.inputTextTokens?.toIntOrNull() ?: 0,
                outputTokens = usage?.completionTokens?.toIntOrNull() ?: 0,
                totalTokens = usage?.totalTokens?.toIntOrNull() ?: 0
            )
        }
    }
}

data class ApiResponse(
    val text: String,
    val tokens: TokenStats
)