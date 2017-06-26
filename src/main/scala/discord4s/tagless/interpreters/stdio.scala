package discord4s.tagless.interpreters

import cats.data.ReaderT
import cats.effect.Effect
import discord4s.tagless.protocols
import discord4s.tagless.protocols.DebugMessage
import discord4s.tagless.types.Bot

object stdio {
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
