package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ZisprGeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun getDjResponse(userPrompt: String, availableTracks: List<Track>): DjResponse = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e("ZisprGeminiService", "GEMINI_API_KEY is not configured or is a placeholder.")
            return@withContext DjResponse(
                message = "Olá! O DJ Zispr está offline porque a chave de API do Gemini ainda não está configurada no painel de segredos (Secrets). Mas você ainda pode criar playlists, favoritar suas músicas e curtir muito som!",
                suggestedTrackIds = emptyList()
            )
        }

        // Build track references for context
        val trackContext = availableTracks.joinToString("\n") { 
            "- ID: \"${it.id}\", Título: \"${it.title}\", Artista: \"${it.artist}\", Álbum: \"${it.album}\", Gênero: \"${it.genre}\", Tipo: ${if (it.isPodcast) "Podcast" else "Música"}"
        }

        val promptBody = """
            Você é o "DJ Zispr", o DJ de IA ultra-carismático e inteligente do aplicativo de streaming Zispr (um app de alta fidelidade como o Spotify). Suas respostas devem ser descontraídas, empolgantes, engraçadas e em português brasileiro, usando gírias do mundo da música e ritmo de rádio premium!
            
            O usuário disse: "$userPrompt"
            
            Com base no que o usuário disse, recomende de 1 a 4 faixas da nossa biblioteca disponível abaixo. 
            Você DEVE responder obrigatoriamente em formato JSON estruturado com duas chaves:
            1. "message": Seu comentário de DJ super animado e carismático, contextualizando suas escolhas e estimulando o ouvinte. Explique em poucos parágrafos por que escolheu essas músicas!
            2. "recommendedTrackIds": Uma lista de strings com os IDs das faixas que você escolheu recomendar. Escolha apenas faixas da lista abaixo!
            
            Biblioteca de faixas do Zispr disponível:
            $trackContext
            
            Responda APENAS com o objeto JSON puro, sem marcações markdown como ```json, apenas as chaves "message" e "recommendedTrackIds".
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", promptBody)
                        })
                    })
                })
            })
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            .post(jsonRequest.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e("ZisprGeminiService", "API Error: ${response.code} - $errBody")
                    return@withContext DjResponse(
                        message = "Epa, deu uma pequena interferência na transmissão aqui! 📻 Mas relaxa, o som não para. Vamos continuar ouvindo nossa biblioteca premium!",
                        suggestedTrackIds = emptyList()
                    )
                }

                val resBody = response.body?.string() ?: ""
                Log.d("ZisprGeminiService", "Gemini Response: $resBody")
                
                val rootJson = JSONObject(resBody)
                val candidates = rootJson.optJSONArray("candidates")
                val textResponse = if (candidates != null && candidates.length() > 0) {
                    candidates.getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .get("text") as String
                } else {
                    ""
                }

                // Parse structured JSON returned by Gemini
                val cleanText = textResponse.trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                try {
                    val parsedJson = JSONObject(cleanText)
                    val message = parsedJson.optString("message", "E aí galera! DJ Zispr na área enviando as melhores vibrações!")
                    val trackIdsJson = parsedJson.optJSONArray("recommendedTrackIds")
                    val trackIds = mutableListOf<String>()
                    if (trackIdsJson != null) {
                        for (i in 0 until trackIdsJson.length()) {
                            trackIds.add(trackIdsJson.getString(i))
                        }
                    }
                    DjResponse(message = message, suggestedTrackIds = trackIds)
                } catch (pe: Exception) {
                    Log.e("ZisprGeminiService", "Failed to parse inner JSON: ${pe.message}. Content: $cleanText")
                    // Fallback to plain text if JSON extraction failed
                    DjResponse(
                        message = cleanText.ifEmpty { "Curta este som incrível preparado especialmente para você pelo seu DJ Zispr!" },
                        suggestedTrackIds = emptyList()
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("ZisprGeminiService", "Request Exception", e)
            DjResponse(
                message = "Tivemos um problema de rede ao consultar o DJ de IA. Certifique-se de que está conectado à internet! 🌐",
                suggestedTrackIds = emptyList()
            )
        }
    }
}

data class DjResponse(
    val message: String,
    val suggestedTrackIds: List<String>
)
