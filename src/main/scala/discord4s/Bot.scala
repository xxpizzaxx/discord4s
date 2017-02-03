package discord4s

import discord4s.Types.DiscordService
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.api.events.{Event, IListener}

import scalaz.Kleisli
import scalaz.concurrent.Task


object Types {
  type DiscordService = Kleisli[Task, Event, Event]
  object DiscordService {
    def lift(f: Event => Task[Event]): DiscordService = Kleisli.kleisli(f)
    def apply(pf: PartialFunction[Event, Unit]): DiscordService =
      lift(e => Task{pf.apply(e); e })
  }
}

class DiscordBot(c: IDiscordClient)(ds: DiscordService) extends IListener[Event] {
  c.getDispatcher.registerListener(this)
  override def handle(t: Event): Unit = {
    ds(t).unsafePerformSyncAttempt
  }
}