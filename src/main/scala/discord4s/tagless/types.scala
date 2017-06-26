package discord4s.tagless

import cats.data.ReaderT
import sx.blah.discord.handle.impl.events.MessageReceivedEvent

import scala.languageFeature.higherKinds

object types {
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
}
