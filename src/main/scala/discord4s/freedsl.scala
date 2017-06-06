package discord4s

import cats._
import cats.implicits._
import cats.effect.{Effect, IO}
import cats.data.{Reader, ReaderT}
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
  }

  object BotInterpreter {
    import scala.collection.JavaConverters._
    implicit def discordbot[M[_]: Effect] = new Bot[protocols.Discord, ReaderT[M, MessageReceivedEvent, ?]] {
      private val io = Effect[M]
      override def getMessage = ReaderT { m => io.delay { m.getMessage.getContent } }
      override def getSender = ReaderT { m => io.delay { m.getMessage.getAuthor.getName } }
      override def getChannel = ReaderT { m => io.delay { m.getMessage.getChannel.getName } }
      override def reply(msg: String)  = ReaderT { m => io.delay { m.getMessage.getChannel.sendMessage(msg) } }
      override def setNickname(nick: String) = ReaderT { m => io.delay { m.getClient.changeUsername(nick)} }
      override def getChannelTopic = ReaderT { m => io.delay { m.getMessage.getChannel.getTopic }}
      override def getChannelMembers = ReaderT { m => io.delay { m.getMessage.getChannel.getUsersHere.asScala.map(_.getName).toList }}
    }

    implicit def stdiobot[M[_]: Effect] = new Bot[protocols.stdio, ReaderT[M, DebugMessage, ?]] {
      private val io = Effect[M]
      var nick = "debugbot"
      override def getMessage = ReaderT { m => io.delay { m.msg } }
      override def getSender = ReaderT { m => io.delay { m.author }}
      override def getChannel = ReaderT { m => io.delay { m.channel.name }}
      override def reply(msg: String) = ReaderT { m => io.delay { println(s"${this.nick}: $msg") }}
      override def setNickname(nick: String) = ReaderT { m => io.delay { println(s"changing my nick to $nick"); this.nick = nick }}
      override def getChannelTopic = ReaderT { m => io.delay { m.channel.topic }}
      override def getChannelMembers = ReaderT { m => io.delay { m.channel.members }}
    }
  }

}

object freetest extends App {
  import freedsl._


  def echobot2[P, F[_]](bot: Bot[P, F])(implicit fm: Monad[F]) = {
    implicit val b = bot
    echobot[P, F]
  }
  def echobot[P, F[_]: Monad](implicit bot: Bot[P, F]) = {
    for {
      _       <- bot.setNickname("echobot")
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
  echobot[stdio, ReaderT[IO, DebugMessage, ?]].run(msg).unsafeRunSync

  // doesn't work
  echobot2(discordbot[IO]).run(msg).unsafeRunSync


}
