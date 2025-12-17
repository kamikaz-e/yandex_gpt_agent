package dev.kamikaze.yandexgpttest

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import dev.kamikaze.yandexgpttest.data.ApiClientFactory
import dev.kamikaze.yandexgpttest.data.ApiSettings
import dev.kamikaze.yandexgpttest.domain.ChatInteractor
import dev.kamikaze.yandexgpttest.ui.theme.ChatScreen
import dev.kamikaze.yandexgpttest.ui.theme.YandexGptTestTheme

class MainActivity : ComponentActivity() {

    private val hasAudioPermission = mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission.value = isGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkAudioPermission()

        setContent {
            val apiClient = ApiClientFactory.create(ApiSettings())
            val chatInteractor = ChatInteractor(apiClient)
            val viewModel = ChatViewModel(chatInteractor, applicationContext)
            YandexGptTestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ChatScreen(
                        viewModel = viewModel,
                        hasAudioPermission = hasAudioPermission.value,
                        onRequestAudioPermission = { requestAudioPermission() },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun checkAudioPermission() {
        hasAudioPermission.value = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestAudioPermission() {
        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
}
