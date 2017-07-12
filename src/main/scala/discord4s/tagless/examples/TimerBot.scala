package discord4s.tagless.examples

import cats.Monad
import cats.data.{ReaderT, StateT}
import cats.effect.IO
import cats.implicits._
import discord4s.tagless.protocols
import discord4s.tagless.types.Bot
import java.time.{Instant, Duration}
import fastparse.all._

object TimerBot extends App {
  case class Timer(time: Instant, location: String, sortOf: String)
  case class TimerBotState(timers: List[Timer] = List.empty[Timer])

  val addTimerParser: P[Timer] = {
    val number = P(CharIn('0' to '9').rep(min=1).!)
    val time = P(number.! ~ CharIn("hms").!)
    P(P("!addtimer ") ~ time.rep(sep=" ")).map { times =>
      val duration = times.map { t =>
        val (digits, unit) = t
        val n = digits.toInt
        unit match {
          case "h" => Duration.ofHours(n)
          case "m" => Duration.ofMinutes(n)
          case "s" => Duration.ofSeconds(n)
        }
      }.reduce(_.plus(_))
      Timer(Instant.now().plus(duration), "", "")
    }
  }

  def echobot[P, F[_]: Monad](implicit bot: Bot[P, F]): StateT[F, TimerBotState, Unit] = {
    for {
      msg     <- StateT.lift(bot.getMessage)
      channel <- StateT.lift(bot.getChannel)
      author  <- StateT.lift(bot.getSender)
      _       <- StateT { s: TimerBotState =>

        msg match {
          case addTimerParser(timer) =>
            bot.delay { timer }.map(t => (s.copy(timers = t :: s.timers),()))
          case _ =>
            println("nop")
            StateT.lift(bot.delay(())).run(s)
        }
      }
      _       <- StateT { s: TimerBotState =>
        if (msg.startsWith("!timers"))
          bot.reply(s.timers.toString).map((s,_))
        else
          bot.reply(s"I didn't see anything").map((s,_))
      }
    } yield ()
  }


  import discord4s.tagless.interpreters.stdio._
  import discord4s.tagless.protocols._

  val channel = protocols.Channel("#echo", "type messages here to get them echo'd", List.empty[String])
  val messages = List(
    protocols.DebugMessage("bob", "!addtimer 10m", channel),
    protocols.DebugMessage("bob", "!timers", channel)
  )

  // works
  import fs2._
  val bot = echobot[stdio, ReaderT[IO, DebugMessage, ?]] //.run(msg).unsafeRunSync

  val s = Stream.emits(messages).scan(TimerBotState()) { (state, message) =>
    bot.run(state).run(message).unsafeRunSync._1
  }
  s.run
  /*
  messages.foldLeft(TimerBotState()) { (state, message) =>
    bot.run(state).run(message).unsafeRunSync._1
  }*/

}
