package com.vonage.numberverification.test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vonage.numberverification.VGNumberVerificationClient
import com.vonage.numberverification.VGNumberVerificationParameters
import com.vonage.numberverification.test.ui.theme.NumberVerificationTheme
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

const val LOGIN_URL = "https://your-backend-url/login"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VGNumberVerificationClient.initializeSdk(this.applicationContext)
        setContent {
            NumberVerificationApp()
        }
    }
}

// Function to send login request
suspend fun sendLogin(url: String, phoneNumber: String): Result<String> {
    val client = OkHttpClient()

    val jsonBody = """
        {
            "phone": "$phoneNumber"
        }
    """.trimIndent()
    val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
    val request = Request.Builder()
        .url(url)
        .post(requestBody)
        .header("Content-Type", "application/json")
        .build()

    return withContext(Dispatchers.IO) {
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(response.body?.string().orEmpty())
            } else {
                Result.failure(IOException("HTTP error: ${response.code}"))
            }
        } catch (e: IOException) {
            Result.failure(e)
        }
    }
}

// Function to send the auth request using the SDK
suspend fun sendAuthRequest(url: String): Result<Boolean> {
    val params = VGNumberVerificationParameters(
        url = url,
        headers = emptyMap(),
        queryParameters = emptyMap(),
        maxRedirectCount = 15
    )

    return withContext(Dispatchers.IO) {
        val response = VGNumberVerificationClient.getInstance()
            .startNumberVerification(params, true)

        val status = response.optInt("http_status")
        val isVerified = response.optJSONObject("response_body")
            ?.optString("devicePhoneNumberVerified", "false")?.toBoolean() ?: false

        if (status == 200 && isVerified) {
            Result.success(true)
        } else {
            Result.failure(IOException("Verification failed"))
        }
    }
}

@Composable
fun NumberVerificationApp() {
    NumberVerificationTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MainScreen()
        }
    }
}

@Composable
fun MainScreen() {
    var phoneInput by remember { mutableStateOf("+99012345678") }
    var authResult by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Number Verification API Demo",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = phoneInput,
            onValueChange = { phoneInput = it },
            label = { Text("Phone Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        authResult = try {
                            val loginResult = sendLogin(LOGIN_URL, phoneInput)
                            loginResult.getOrThrow().let { jsonString ->
                                val authURL = JSONObject(jsonString).optString("url")
                                val authResponse = sendAuthRequest(authURL)
                                if (authResponse.getOrThrow()) "Authentication successful"
                                else "Failed to authenticate"
                            }
                        } catch (e: Exception) {
                            e.localizedMessage ?: "An error occurred"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        authResult?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
