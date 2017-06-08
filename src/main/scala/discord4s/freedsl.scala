package discord4s

import cats._
import cats.implicits._
import cats.effect.{Effect, IO}
import cats.data.{Reader, ReaderT, StateT}
import sx.blah.discord.handle.impl.events.MessageReceivedEvent
import sx.blah.discord.handle.obj.{IChannel, IMessage}

import scala.collection.mutable

object protocols {
  sealed trait Discord
  sealed trait IRC
  sealed trait stdio

  case class Channel(name: String, topic: String, members: List[String])
  case class DebugMessage(author: String, msg: String, channel: Channel)
}

object freedsl {

  import protocols._

  type WithDiscordMessage[M[_], A] = ReaderT[M, MessageReceivedEvent, A]
  type DiscordBot[E[_], ?] = Bot[Discord, WithDiscordMessage[E, ?]]

  type WithDebugMessage[M[_], A] = ReaderT[M, DebugMessage, A]
  type DebugBot[E[_], ?] = Bot[stdio, WithDebugMessage[E, ?]]

  trait Bot[Protocol, F[_]] {
    def getMessage: F[String]
    def getSender: F[String]
    def getChannel: F[String]
    def reply(msg: String): F[Unit]
    def setNickname(nick: String): F[Unit]
    def getChannelTopic: F[String]
    def getChannelMembers: F[List[String]]
    def delay[A](a: => A): F[A]
  }

  object BotInterpreter {
    import scala.collection.JavaConverters._
    implicit def discordbot[M[_]: Effect] = new Bot[protocols.Discord, ReaderT[M, MessageReceivedEvent, ?]] {
      private val io = Effect[M]
      override def getMessage                = ReaderT { m => io.delay { m.getMessage.getContent } }
      override def getSender                 = ReaderT { m => io.delay { m.getMessage.getAuthor.getName } }
      override def getChannel                = ReaderT { m => io.delay { m.getMessage.getChannel.getName } }
      override def reply(msg: String)        = ReaderT { m => io.delay { m.getMessage.getChannel.sendMessage(msg) } }
      override def setNickname(nick: String) = ReaderT { m => io.delay { m.getClient.changeUsername(nick)} }
      override def getChannelTopic           = ReaderT { m => io.delay { m.getMessage.getChannel.getTopic }}
      override def getChannelMembers         = ReaderT { m => io.delay { m.getMessage.getChannel.getUsersHere.asScala.map(_.getName).toList }}
      override def delay[A](a: => A)             = ReaderT { m => io.delay { a }}
    }

    implicit def stdiobot[M[_]: Effect] = new Bot[protocols.stdio, ReaderT[M, DebugMessage, ?]] {
      private val io = Effect[M]
      var nick = "debugbot"
      override def getMessage                = ReaderT { m => io.delay { m.msg } }
      override def getSender                 = ReaderT { m => io.delay { m.author }}
      override def getChannel                = ReaderT { m => io.delay { m.channel.name }}
      override def reply(msg: String)        = ReaderT { m => io.delay { println(s"${this.nick}: $msg") }}
      override def setNickname(nick: String) = ReaderT { m => io.delay { println(s"changing my nick to $nick"); this.nick = nick }}
      override def getChannelTopic           = ReaderT { m => io.delay { m.channel.topic }}
      override def getChannelMembers         = ReaderT { m => io.delay { m.channel.members }}
      override def delay[A](a: => A)             = ReaderT { m => io.delay { a }}
    }
  }

}

object freetest extends App {
  import freedsl._

  case class EchoBotState(name: Option[String] = None)

  // hi I'm very dangerous
  implicit def passthrough[F[_]: Monad, C, A](f: => F[A]): StateT[F, C, A] = StateT { c: C => f.map((c, _)) }

  def echobot[P, F[_]: Monad](implicit bot: Bot[P, F]): StateT[F, EchoBotState, Unit] = {
    for {
      _       <- StateT { s: EchoBotState =>
        if (s.name.isEmpty) {
          bot.setNickname("echobot").map((s.copy(name = Some("echobot")), _))
        } else {
          Monad[F].pure((s, ()))
        }
      }
      msg     <- bot.getMessage
      channel <- bot.getChannel
      author  <- bot.getSender
      _       <- bot.reply(s"I saw $author say $msg in $channel")
    } yield ()
  }


  import freedsl.BotInterpreter._
  import protocols._

  val channel = protocols.Channel("#echo", "type messages here to get them echo'd", List.empty[String])
  val msg = protocols.DebugMessage("bob", "MESSAGE", channel)

  // works
  echobot[stdio, ReaderT[IO, DebugMessage, ?]].run(EchoBotState(Option.empty[String])).run(msg).unsafeRunSync

}

object whobotextreme extends App {
  import sx.blah.discord.api.ClientBuilder
  import sx.blah.discord.api.events._
  import sx.blah.discord.handle.impl.events.{MessageReceivedEvent, ReadyEvent}
  import scalaz.concurrent.Task
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

  val token = ""
  val httpClient = PooledHttp1Client.apply()
  val zkb = new ZKBAPI()
  val esi = new EsiClient("pizza-bot v0.1", httpClient.toHttpService)
  val client = new ClientBuilder()
    .withToken(token)
    .build()
  import freedsl._

  implicit def passthrough[F[_]: Monad, C, A](f: => F[A]): StateT[F, C, A] = StateT { c: C => f.map((c, _)) }

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

  import freedsl.BotInterpreter._
  import protocols._

  val channel = protocols.Channel("#who", "type messages here to get them who'd", List.empty[String])
  val msg = protocols.DebugMessage("bob", "Lucia Denniard", channel)

  // works
  val p = whobot[stdio, IO, ReaderT[IO, DebugMessage, ?]].run(msg)
  println("composed")
  readLine()
  println("running")
  p.unsafeRunSync

  println("ran")



  httpClient.shutdownNow()
}
