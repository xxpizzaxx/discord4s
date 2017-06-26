package discord4s.tagless

object protocols {
  sealed trait Discord
  sealed trait IRC
  sealed trait stdio

  case class Channel(name: String, topic: String, members: List[String])
  case class DebugMessage(author: String, msg: String, channel: Channel)
}
