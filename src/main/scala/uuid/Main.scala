package uuid

import zio.*
import zio.json.*

import com.bilalfazlani.zioMaelstrom.*
import com.bilalfazlani.zioMaelstrom.protocol.*

case class Generate(msg_id: MessageId) extends NeedsReply derives JsonDecoder

case class GenerateOk(
    id: String,
    in_reply_to: MessageId,
    `type`: String = "generate_ok"
) extends Sendable,
      Reply
    derives JsonEncoder
//}

object Main extends ZIOAppDefault {

  val handler = receive[Generate] { case request =>
    for {
      ref       <- ZIO.service[Ref[Int]]
      generated <- ref.updateAndGet(_ + 1)
      combinedId = s"${me}_${generated}"
      _ <- reply(GenerateOk(id = combinedId, in_reply_to = request.msg_id))
    } yield ()
  }

  val run = handler.provide(MaelstromRuntime.live, ZLayer(Ref.make(0)))

}
