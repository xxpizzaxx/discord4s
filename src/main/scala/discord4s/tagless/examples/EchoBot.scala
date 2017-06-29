package discord4s
package tagless
package examples

import cats.Monad
import cats.data.{ReaderT, StateT}
import cats.effect.IO
import discord4s.tagless.protocols
import discord4s.tagless.types.Bot
import cats._, cats.implicits._, cats.syntax._

object EchoBot extends App {
  case class EchoBotState(name: Option[String] = None)

  def echobot[P, F[_]: Monad](implicit bot: Bot[P, F]): StateT[F, EchoBotState, Unit] = {
    for {
      _       <- StateT { s: EchoBotState =>
        if (s.name.isEmpty) {
          bot.setNickname("echobot").map((s.copy(name = Some("echobot")), _))
        } else {
          Monad[F].pure((s, ()))
        }
      }
      msg     <- StateT.lift(bot.getMessage)
      channel <- StateT.lift(bot.getChannel)
      author  <- StateT.lift(bot.getSender)
      _       <- StateT.lift(bot.reply(s"I saw $author say $msg in $channel"))
    } yield ()
  }


  import discord4s.tagless.interpreters.stdio._
  import discord4s.tagless.types._
  import discord4s.tagless.protocols._

  val channel = protocols.Channel("#echo", "type messages here to get them echo'd", List.empty[String])
  val msg = protocols.DebugMessage("bob", "MESSAGE", channel)

  // works
  echobot[stdio, ReaderT[IO, DebugMessage, ?]].run(EchoBotState(Option.empty[String])).run(msg).unsafeRunSync

}
