package com.bdash.api.discord

import com.bdash.api.UserSession
import com.bdash.api.bot.Info
import com.bdash.api.discord.models.Guild
import com.bdash.api.discord.models.GuildExists
import com.bdash.api.httpClient
import com.bdash.api.utils.toError
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

object DiscordApi {
    suspend fun getGuild(user: UserSession, id: String): Guild? {
        return getGuilds(user).find { it.id == id }
    }

    suspend fun getGuilds(user: UserSession): Array<Guild> {
        val res = httpClient.get(Routes.guilds) {
            headers {
                append(HttpHeaders.Authorization, "${user.token_type} ${user.token}")
            }
        }

        return if (res.status == HttpStatusCode.OK) {
            res.body()
        } else {
            throw res.toError()
        }
    }

    /**
     * @return false if not authenticated
     */
    suspend fun checkToken(user: UserSession): Boolean {
        val result = httpClient.head(Routes.verify) {
            headers {
                append(HttpHeaders.Authorization, "${user.token_type} ${user.token}")
            }
        }

        return result.status != HttpStatusCode.Unauthorized
    }

    suspend fun getGuildsExists(user: UserSession): List<GuildExists> {
        val res = httpClient.get(Routes.guilds) {
            headers {
                append(HttpHeaders.Authorization, "${user.token_type} ${user.token}")
            }
        }

        return if (res.status == HttpStatusCode.OK) {
            val guilds = res.body<Array<GuildExists>>()

            guilds.filter {
                it.exist = Info.jda.getGuildById(it.id) != null

                it.owner ?: false
            }
        } else {
            throw res.toError()
        }
    }
}

@Serializable
class Error(
    val message: String,
    val code: Int? = 0,
)
