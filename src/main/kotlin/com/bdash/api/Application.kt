package com.bdash.api

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.bdash.api.plugins.*
import com.bdash.api.variable.clientOrigin
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import kotlinx.serialization.json.Json
import kotlin.time.DurationUnit
import kotlin.time.toDuration

val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
        })
    }
}

suspend fun main() {

    embeddedServer(Netty, port = 8080) {
        configureSecurity()
        configureRouting()
        install(CORS) {
            allowCredentials = true
            allowHost(clientOrigin)

            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.Authorization)
        }

        install(Sessions) {
            cookie<UserSession>("user_session") {
                val secretEncryptKey = hex(System.getenv("ENCRYPT_KEY"))
                val secretSignKey = hex(System.getenv("SIGN_KEY"))

                cookie.path = "/"
                cookie.maxAge = 3.toDuration(DurationUnit.DAYS)
                transform(SessionTransportTransformerEncrypt(secretEncryptKey, secretSignKey))
            }
        }

        install(Authentication) {
            oauth("discord-oauth2") {
                urlProvider = { "http://localhost:8080/callback" }
                providerLookup = {
                    OAuthServerSettings.OAuth2ServerSettings(
                        name = "discord",
                        authorizeUrl = "https://discord.com/api/oauth2/authorize",
                        accessTokenUrl = "https://discord.com/api/oauth2/token",
                        requestMethod = HttpMethod.Post,
                        clientId = System.getenv("CLIENT_ID"),
                        clientSecret = System.getenv("CLIENT_SECRET"),
                        defaultScopes = listOf("guilds", "identify")
                    )
                }
                client = httpClient
            }
        }
    }.start(wait = true)
}

data class UserSession(val token: String, val token_type: String)