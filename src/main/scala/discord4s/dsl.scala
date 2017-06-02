package discord4s

import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.api.events.Event
import sx.blah.discord.handle.impl.events._
import sx.blah.discord.handle.obj.{IChannel, IMessage}

import scala.util.matching.Regex

object dsl {
  object MessageReceived { def unapply(arg: MessageReceivedEvent): Option[String] = Option(arg.getMessage.getContent) }
  object ChannelCreate { def unapply(arg: ChannelCreateEvent): Option[IChannel] = Option(arg.getChannel) }
  object ChannelDelete { def unapply(arg: ChannelDeleteEvent): Option[IChannel] = Option(arg.getChannel) }
  object ChannelUpdate { def unapply(arg: ChannelUpdateEvent): Option[(IChannel, IChannel)] = Option(arg.getOldChannel, arg.getNewChannel)}
  object Client { def unapply(arg: Event): Option[IDiscordClient] = Option(arg.getClient) }

  // http://stackoverflow.com/questions/2261358/pattern-matching-with-conjunctions-patterna-and-patternb
  object && {
    def unapply[A](a: A) = Some((a, a))
  }
  implicit class RegexContext(sc: StringContext) {
    def r = new Regex(sc.parts.mkString, sc.parts.tail.map(_ => "x"): _*)
  }
}
