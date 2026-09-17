package com.example.data.gemini

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

data class ClientSummaryData(
    val clientName: String,
    val totalHours: Double,
    val totalBilled: Double,
    val hourlyRate: Double
)

class GeminiRepository(
    private val apiKeyProvider: () -> String = { BuildConfig.GEMINI_API_KEY }
) {
    private val apiService: GeminiApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun analyzeMonthlyReport(
        monthName: String,
        totalEarnings: Double,
        totalHours: Double,
        clientSummaries: List<ClientSummaryData>
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider().trim()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException(
                    "Chave do Gemini não configurada.\n\nPara ativar a IA:\n1. Adicione sua chave no arquivo .env como GEMINI_API_KEY=sua_chave\n2. Recompile o aplicativo."
                )
            )
        }

        val clientsBreakdown = clientSummaries.joinToString("\n") {
            "- Cliente: ${it.clientName} | Horas: %.2fh | Taxa: R$ %.2f/h | Total: R$ %.2f".format(
                it.totalHours, it.hourlyRate, it.totalBilled
            )
        }

        val prompt = """
            Você é um consultor financeiro e estrategista de produtividade para profissionais autônomos e freelancers.
            Analise os dados de faturamento e horas do mês de $monthName:

            - Total Faturado no Mês: R$ %.2f
            - Total de Horas Trabalhadas: %.2fh
            - Média Efetiva da Hora: R$ %.2f/h

            Discriminação por Cliente:
            $clientsBreakdown

            Por favor, forneça uma análise estruturada em Português do Brasil com:
            1. 📊 Visão Geral do Mês (resumo executivo direto)
            2. 💡 Destaques de Rentabilidade (qual cliente gerou mais valor por hora e qual exigiu mais esforço)
            3. 🎯 Recomendações Estratégicas para o próximo mês (precificação, retenção ou distribuição de tempo)

            Mantenha o tom profissional, direto e encorajador. Use tópicos com emojis.
        """.trimIndent().format(
            totalEarnings,
            totalHours,
            if (totalHours > 0) totalEarnings / totalHours else 0.0
        )

        try {
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = prompt))
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.7,
                    maxOutputTokens = 1200
                )
            )

            val response = apiService.generateContent(apiKey, request)

            if (response.error != null) {
                return@withContext Result.failure(
                    Exception("Erro retornado pelo Gemini: ${response.error.message ?: "Desconhecido"}")
                )
            }

            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(Exception("O Gemini não retornou nenhum texto de resposta."))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun parseSessionFromNaturalText(
        userText: String,
        availableClients: List<com.example.data.Client>
    ): Result<ParsedQuickSession> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider().trim()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Configure sua chave GEMINI_API_KEY no arquivo .env para usar o lançamento rápido por IA.")
            )
        }

        val clientsListStr = availableClients.joinToString("\n") { "- ID: ${it.id}, Nome: ${it.name}" }

        val prompt = """
            Você é um assistente inteligente de gerenciamento de tempo. O usuário ditou ou digitou uma frase descrevendo um trabalho realizado:
            "$userText"

            Clientes cadastrados no app:
            $clientsListStr

            Tags disponíveis: #Dev, #Reunião, #Design, #Suporte, #Consultoria, #Outro

            Extraia as informações e responda ESTRITAMENTE em formato JSON (sem blocos markdown ```json):
            {
              "clientId": <número do ID do cliente correspondente ou null>,
              "clientName": "<nome do cliente identificado ou string vazia>",
              "durationMinutes": <total em minutos como inteiro, ex: 90 para 1h30>,
              "description": "<descrição concisa e profissional do que foi feito>",
              "tag": "<uma das tags disponíveis acima com #>"
            }
        """.trimIndent()

        try {
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = prompt))
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.2,
                    maxOutputTokens = 300
                )
            )

            val response = apiService.generateContent(apiKey, request)
            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?.replace("```json", "")?.replace("```", "")?.trim()

            if (!rawText.isNullOrBlank()) {
                val json = org.json.JSONObject(rawText)
                val clientId = if (json.isNull("clientId")) null else json.optLong("clientId")
                val clientName = json.optString("clientName", "")
                val durationMinutes = json.optLong("durationMinutes", 60L)
                val description = json.optString("description", userText)
                val tag = json.optString("tag", "#Outro")

                Result.success(
                    ParsedQuickSession(
                        clientId = clientId,
                        clientName = clientName,
                        durationMinutes = maxOf(1L, durationMinutes),
                        description = description,
                        tag = tag
                    )
                )
            } else {
                Result.failure(Exception("O Gemini não retornou dados para a frase."))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}

data class ParsedQuickSession(
    val clientId: Long?,
    val clientName: String,
    val durationMinutes: Long,
    val description: String,
    val tag: String
)
