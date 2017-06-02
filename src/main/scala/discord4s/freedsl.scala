package discord4s

import freestyle._
import freestyle.implicits._
import cats._
import cats.implicits._
import cats.effect.IO
import cats.data.{Reader, ReaderT}
import sx.blah.discord.handle.impl.events.MessageReceivedEvent
import sx.blah.discord.handle.obj.{IChannel, IMessage}

object freedsl {

  type WithMessage[A] = ReaderT[IO, MessageReceivedEvent, A]

  @tagless trait Bot {
    def getMessage(a: Unit): FS[String]
    def getChannel(a: Unit): FS[String]
    def reply(msg: String): FS[Unit]
  }

  object BotInterpreter {
    implicit val botHandler = new Bot.Handler[WithMessage] {
      override def getMessage(a: Unit) = ReaderT { m => IO { m.getMessage.getContent } }
      override def getChannel(a: Unit) = ReaderT { m => IO { m.getMessage.getChannel.getName } }
      override def reply(msg: String)  = ReaderT { m => IO { m.getMessage.getChannel.sendMessage(msg) } }
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
  program[WithMessage].run(msgevent).unsafeRunSync

  verify(channel).sendMessage("I saw someone say oh hello there in #coolchannel")


}
