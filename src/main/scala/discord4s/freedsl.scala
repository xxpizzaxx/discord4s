package discord4s

import freestyle._
import freestyle.implicits._
import cats._
import cats.implicits._
import cats.effect.IO
import cats.data.Reader
import sx.blah.discord.handle.impl.events.MessageReceivedEvent
import sx.blah.discord.handle.obj.{IChannel, IMessage}

object freedsl {

  type WithMessage[A] = Reader[MessageReceivedEvent, A]

  @tagless trait Bot {
    // the macro dies if these don't have argument lists, make it take Unit for now I guess
    def getMessage(a: Unit): FS[String]
    def getChannel(a: Unit): FS[String]
    def reply(msg: String): FS[Unit]
  }

  object BotInterpreter {
    implicit val botHandler = new Bot.Handler[WithMessage] {
      override def getMessage(a: Unit) = Reader { m => m.getMessage.getContent }
      override def getChannel(a: Unit) = Reader { m => m.getMessage.getChannel.getName }
      override def reply(msg: String) = Reader { m => m.getMessage.getChannel.sendMessage(msg) }
    }
  }

}

object freetest extends App {
  import freedsl._

  def program[F[_] : Monad](implicit bot: Bot[F]) = {

    for {
      msg <- bot.getMessage(())
      channel <- bot.getChannel(())
      // I'm an echo bot!
      _ <- bot.reply(s"I saw someone say $msg in $channel")
    } yield ()
  }

  import freedsl.BotInterpreter._

  import org.mockito.Mockito._

  // some kind of unit test, just go with it
  val msg = mock(classOf[IMessage])
  when(msg.getContent).thenReturn("oh hello there")
  val channel = mock(classOf[IChannel])
  when(msg.getChannel).thenReturn(channel)
  when(channel.getName).thenReturn("#coolchannel")

  val msgevent = new MessageReceivedEvent(msg)
  program[WithMessage].run(msgevent)

  verify(channel).sendMessage("I saw someone say oh hello there in #coolchannel")


}
