package discord4s.tagless.interpreters

import cats.data.ReaderT
import cats.effect.Effect
import discord4s.tagless.protocols
import discord4s.tagless.types.Bot
import sx.blah.discord.handle.impl.events.MessageReceivedEvent

object discord {
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
}
