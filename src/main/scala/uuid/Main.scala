package uuid

import zio.json.{JsonEncoder, JsonDecoder}
import zio.{Random, ZIOAppDefault, ZIO}
import com.bilalfazlani.zioMaelstrom.protocol.*
import com.bilalfazlani.zioMaelstrom.*

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

  val generateHandler: ZIO[MaelstromRuntime, Nothing, Unit] =
    receive[Generate](msg =>
      Random.nextUUID.flatMap { uuid =>
        reply(GenerateOk(id = uuid.toString, in_reply_to = msg.msg_id))
      }
    )

  val run = generateHandler.provide(MaelstromRuntime.live)
}
