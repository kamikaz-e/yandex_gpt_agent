package dev.kamikaze.yandexgpttest.domain

import dev.kamikaze.yandexgpttest.data.ExpertAgentResponse
import dev.kamikaze.yandexgpttest.data.MessageRequest
import dev.kamikaze.yandexgpttest.data.YandexApi
import dev.kamikaze.yandexgpttest.data.prompt.AgentType
import dev.kamikaze.yandexgpttest.data.prompt.parseExpertResponses

class ChatInteractor(private val api: YandexApi) {

    // Отправляем запрос к обоим агентам
    suspend fun sendMessage(
        userMessage: String,
        conversationHistory: List<MessageRequest.Message>,
    ): String {
        val results = mutableListOf<ExpertAgentResponse>()
        // Запрос к yandexgpt/rc
        results.add(
            api.sendMessages(
                userMessage = userMessage,
                conversationHistory = conversationHistory,
                agentType = AgentType.YANDEXGPT_RC
            )
        )
        // Запрос к yandexgpt/latest
        results.add(
            api.sendMessages(
                agentType = AgentType.YANDEXGPT_LATEST,
                userMessage = userMessage,
                conversationHistory = conversationHistory
            )
        )
        return formatExpertAgentResponses(results)
    }

    private fun formatExpertAgentResponses(expertResponses: List<ExpertAgentResponse>): String {
        return buildString {
            expertResponses.forEachIndexed { index, agentResponse ->
                val experts = parseExpertResponses(agentResponse.response)

                if (experts.isNotEmpty()) {
                    appendLine("🤖 ${agentResponse.agentType.displayName}")
                    appendLine()

                    experts.forEachIndexed { expertIndex, expert ->
                        val expertNumber = expertIndex + 1
                        appendLine("Эксперт $expertNumber: ${expert.expertName}")
                        appendLine()

                        if (expert.directAnswer.isNotBlank()) {
                            appendLine("📋 Прямой ответ:")
                            appendLine(expert.directAnswer)
                            appendLine()
                        }

                        if (expert.stepByStepSolution.isNotBlank()) {
                            appendLine("🔍 Пошаговое решение:")
                            appendLine(expert.stepByStepSolution)
                            appendLine()
                        }

                        if (expert.crossModelAnswer.isNotBlank()) {
                            appendLine("🌐 Ответ на cross-модель промпт:")
                            appendLine(expert.crossModelAnswer)
                        }
                        if (expertIndex == experts.lastIndex && expertResponses.lastIndex == index) return@buildString
                        appendLine()
                        appendLine("---")
                        appendLine()
                    }
                } else {
                    // Если не удалось распарсить экспертов, выводим сырой ответ
                    appendLine()
                    appendLine("🤖 ${agentResponse.agentType.displayName}")
                    appendLine()
                    appendLine(agentResponse.response)
                    appendLine()
                    appendLine("---")
                }
            }
        }
    }
}