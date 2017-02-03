package discord4s

import discord4s.Types.DiscordService
import sx.blah.discord.api.ClientBuilder
import sx.blah.discord.api.events._
import sx.blah.discord.handle.impl.events.{MessageReceivedEvent, ReadyEvent}
import discord4s.dsl._
import eveapi.esi.client.EsiClient
import moe.pizza.zkapi.ZKBAPI
import org.http4s.client.blaze.PooledHttp1Client
import EsiClient._
import eveapi.esi.api.ArgonautCodecs._
import argonaut._, Argonaut._, ArgonautShapeless._

object Main extends App {
  val token = ""
  val httpClient = PooledHttp1Client.apply()
  val zkb = new ZKBAPI()
  val esi = new EsiClient("pizza-bot v0.1", httpClient.toHttpService)
  val client = new ClientBuilder()
    .withToken(token)
    .build()
  val helloSayer = DiscordService {
    case _: ReadyEvent =>
      println("helo")
  }
  val nickChanger = DiscordService {
    case e: ReadyEvent =>
      e.getClient.changeUsername("COOL BOT 420")
  }
  val whoBot = DiscordService {
    case MessageReceived(m) if m.getContent.startsWith("!who") =>
      val name = m.getContent.stripPrefix("!who").trim()
      val message = EsiClient.search
        .getSearch(name, categories = List("character"), strict = Some(true))
        .run(esi)
        .unsafePerformSyncAttempt
        .toOption
        .flatMap(_.toOption)
        .flatMap(_.character.flatMap(_.headOption))
        .flatMap(
          charid =>
            zkb.stats
              .character(charid.toInt)(httpClient)
              .unsafePerformSyncAttempt
              .toOption)
        .map { stat =>
          s"${stat.info.name} [${stat.shipsDestroyed},${stat.shipsLost}]"
        }
        .getOrElse {
          EsiClient.search
            .getSearch(name,
                       categories = List("corporation"),
                       strict = Some(true))
            .run(esi)
            .unsafePerformSyncAttempt
            .toOption
            .flatMap(_.toOption)
            .flatMap(_.corporation.flatMap(_.headOption))
            .flatMap(charid =>
              zkb.stats
                .corporation(charid.toInt)(httpClient)
                .unsafePerformSyncAttempt
                .toOption)
            .map { stat =>
              val activecharacters = stat.activepvp
                .flatMap(_.characters.map(_.count))
                .getOrElse(0L)
                .toDouble
              val charactercount = stat.info.memberCount
              val activePercentage =
                ((activecharacters / charactercount) * 100).toInt
              s"${stat.info.name} [${stat.shipsDestroyed.toInt},${stat.shipsLost.toInt}] ${activePercentage}% active"
            }
            .getOrElse {
              "Unable to find a character or corporation called that"
            }
        }
      m.getChannel.sendMessage(message)
  }

  val b = new DiscordBot(client)(whoBot)
  client.login()
}
