package dev.kamikaze.yandexgpttest.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class SpeechRecognitionHelper(
    private val context: Context,
    private val onResultCallback: (String) -> Unit,
    private val onErrorCallback: (String) -> Unit,
    private val onReadyForSpeechCallback: () -> Unit = {},
    private val onEndOfSpeechCallback: () -> Unit = {}
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isInitializing = false
    private val handler = Handler(Looper.getMainLooper())
    private var pendingStartRunnable: Runnable? = null

    init {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onErrorCallback("Распознавание речи недоступно на этом устройстве")
        }
    }

    fun startListening() {
        if (isListening || isInitializing) {
            return
        }

        // Отменяем предыдущий отложенный запуск, если есть
        pendingStartRunnable?.let { handler.removeCallbacks(it) }

        isInitializing = true

        // Уничтожаем предыдущий экземпляр, если он существует
        cleanupRecognizer()

        // Даем системе время для полной очистки (700 мс)
        pendingStartRunnable = Runnable {
            startListeningInternal()
        }
        handler.postDelayed(pendingStartRunnable!!, 700)
    }

    private fun startListeningInternal() {
        if (isListening) {
            isInitializing = false
            return
        }

        // Создаем новый экземпляр для каждой сессии записи
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createRecognitionListener())
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        isListening = true
        isInitializing = false
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        if (!isListening) {
            return
        }

        isListening = false
        isInitializing = false

        // Отменяем отложенный запуск, если пользователь отменил до старта
        pendingStartRunnable?.let { handler.removeCallbacks(it) }

        speechRecognizer?.stopListening()
    }

    private fun cleanupRecognizer() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    fun destroy() {
        // Отменяем все отложенные задачи
        pendingStartRunnable?.let { handler.removeCallbacks(it) }

        cleanupRecognizer()
        isListening = false
        isInitializing = false
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            onReadyForSpeechCallback()
        }

        override fun onBeginningOfSpeech() {
            // Пользователь начал говорить
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Изменение уровня звука (можно использовать для визуальной индикации)
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            // Получены данные аудио буфера
        }

        override fun onEndOfSpeech() {
            isListening = false
            onEndOfSpeechCallback()
        }

        override fun onError(error: Int) {
            isListening = false
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Ошибка записи аудио"
                SpeechRecognizer.ERROR_CLIENT -> "Ошибка клиента"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Нет разрешения на запись аудио"
                SpeechRecognizer.ERROR_NETWORK -> "Ошибка сети"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Таймаут сети"
                SpeechRecognizer.ERROR_NO_MATCH -> "Не удалось распознать речь"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Распознаватель занят"
                SpeechRecognizer.ERROR_SERVER -> "Ошибка сервера"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Таймаут речи"
                else -> "Неизвестная ошибка: $error"
            }

            // НЕ очищаем здесь - дадим время на "остывание"
            // Очистка произойдет при следующем startListening
            onErrorCallback(errorMessage)
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognizedText = matches?.firstOrNull()

            // НЕ очищаем здесь - дадим время на "остывание"
            // Очистка произойдет при следующем startListening

            if (recognizedText.isNullOrBlank()) {
                onErrorCallback("Не удалось распознать речь")
            } else {
                onResultCallback(recognizedText)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            // Частичные результаты (можно использовать для отображения промежуточного текста)
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            // Дополнительные события
        }
    }

    fun isCurrentlyListening(): Boolean = isListening || isInitializing
}
