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
import argonaut._
import Argonaut._
import ArgonautShapeless._
import eveapi.esi.model._
import shapeless._

import scalaz.concurrent.Task

object Main extends App {
  val token = ""
  val httpClient = PooledHttp1Client.apply()
  val zkb = new ZKBAPI()
  val esi = new EsiClient("pizza-bot v0.1", httpClient.toHttpService)
  val client = new ClientBuilder()
    .withToken(token)
    .build()
  /*
  val helloSayer = DiscordService {
    case _: ReadyEvent =>
      println("helo")
  }
  */
  val nickChanger = DiscordService {
    case e: ReadyEvent =>
      Task { e.getClient.changeUsername("whobot420") }
  }
  val whoCommand = "!who ([\\w ]+)".r
  val whoBot = DiscordService {
    case msg @ MessageReceived(whoCommand(name)) =>
      val channel = msg.getMessage.getChannel
      for {
        _               <- Task { channel.sendMessage(s"doing a who query on $name") }
        attemptid       <- EsiClient.search.getSearch(List("character"), name, strict = Some(true)).run(esi)
        cid             <- Task {
          for {
            searchResult <- attemptid.toOption
            characterResults <- searchResult.character
            firstId <- characterResults.headOption
          } yield firstId
        }
        id              <- Task { cid.get }
        character       <- EsiClient.character.getCharactersCharacterId(id).run(esi)
        characterInfo   <- Task { character.toOption.get }
        corphistory     <- EsiClient.character.getCharactersCharacterIdCorporationhistory(id).run(esi)
        lastCorpJoin    <- Task { corphistory.toList.flatten.head.start_date.get }
        corporation     <- EsiClient.corporation.getCorporationsCorporationId(characterInfo.corporation_id).run(esi)
        corporationInfo <- Task { corporation.toOption.get }
        // they may not have an alliance so these are all Options
        alliances       <- Task.gatherUnordered(corporationInfo.alliance_id.map{i => EsiClient.alliance.getAlliancesAllianceId(i).run(esi)}.toSeq)
        allianceInfo    <- Task { alliances.headOption.flatMap(_.toOption) }
        kbstats         <- zkb.stats.character(id.toLong)(httpClient)
        result          <- Task {
          s"${characterInfo.name}[${kbstats.shipsDestroyed},${kbstats.shipsLost}]" +
            s" - ${corporationInfo.corporation_name} (${lastCorpJoin})" +
            s"${allianceInfo.map(" - " + _.alliance_name).getOrElse("")}"
        }
        _               <- Task { msg.getMessage.getChannel.sendMessage(result) }
      } yield ()
  }
  val b = new DiscordBot(client)(whoBot)
  client.login()
}
