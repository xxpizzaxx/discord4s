package discord4s.tagless.examples

import cats.Monad
import cats.effect.Effect
import discord4s.tagless.types.Bot
import eveapi.esi.client.EsiClient
import EsiClient._
import argonaut._
import Argonaut._
import moe.pizza.zkapi.ZKBAPI
import eveapi.esi.api.ArgonautCodecs._
import org.http4s.client.blaze.PooledHttp1Client
import sx.blah.discord.api.ClientBuilder
import cats._, cats.syntax._, cats.implicits._, cats.data._
import discord4s.tagless._
import ArgonautShapeless._, shapeless._

import scalaz.concurrent.Task

import scala.language.higherKinds


object WhoBot extends App {

  val token = ""
  val httpClient = PooledHttp1Client.apply()
  val zkb = new ZKBAPI()
  val esi = new EsiClient("pizza-bot v0.1", httpClient.toHttpService)
  val client = new ClientBuilder()
    .withToken(token)
    .build()

  import scala.language.implicitConversions

  def whobot[P, E[_]: Effect, F[_]: Monad](implicit bot: Bot[P, F]) = {
      for {
        name            <- bot.getMessage
        _               <- bot.reply(s"Doing a who query on $name")
        attemptid       <- bot.delay { EsiClient.search.getSearch(List("character"), name, strict = Some(true)).run(esi).unsafePerformSync }
        cid             <- bot.delay {
          for {
            searchResult <- attemptid.toOption
            characterResults <- searchResult.character
            firstId <- characterResults.headOption
          } yield firstId
        }
        id              <- bot.delay { cid.get }
        character       <- bot.delay { EsiClient.character.getCharactersCharacterId(id).run(esi).unsafePerformSync }
        characterInfo   <- bot.delay { character.toOption.get }
        corphistory     <- bot.delay { EsiClient.character.getCharactersCharacterIdCorporationhistory(id).run(esi).unsafePerformSync }
        lastCorpJoin    <- bot.delay { corphistory.toList.flatten.head.start_date.get }
        corporation     <- bot.delay { EsiClient.corporation.getCorporationsCorporationId(characterInfo.corporation_id).run(esi).unsafePerformSync }
        corporationInfo <- bot.delay { corporation.toOption.get }
        // they may not have an alliance so these are all Options
        alliances       <- bot.delay { Task.gatherUnordered(corporationInfo.alliance_id.map{i => EsiClient.alliance.getAlliancesAllianceId(i).run(esi)}.toSeq).unsafePerformSync }
        allianceInfo    <- bot.delay { alliances.headOption.flatMap(_.toOption) }
        kbstats         <- bot.delay { zkb.stats.character(id.toLong)(httpClient).unsafePerformSync }
        result          <- bot.delay {
          s"${characterInfo.name}[${kbstats.shipsDestroyed},${kbstats.shipsLost}]" +
            s" - ${corporationInfo.corporation_name} (${lastCorpJoin})" +
            s"${allianceInfo.map(" - " + _.alliance_name).getOrElse("")}"
        }
        _               <- bot.reply(result)
      } yield ()
  }

  val channel = protocols.Channel("#who", "type messages here to get them who'd", List.empty[String])
  val msg = protocols.DebugMessage("bob", "Lucia Denniard", channel)

  // works
  import discord4s.tagless.protocols.stdio
  import cats.effect.IO
  import discord4s.tagless.protocols.DebugMessage
  import discord4s.tagless.interpreters.stdio._
  val p = whobot[stdio, IO, ReaderT[IO, DebugMessage, ?]].run(msg)
  println("composed")
  readLine()
  println("running")
  p.unsafeRunSync

  println("ran")



  httpClient.shutdownNow()
}
