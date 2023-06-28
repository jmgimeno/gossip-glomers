package broadcast

import zio.*
import zio.json.*

import com.bilalfazlani.zioMaelstrom.*
import com.bilalfazlani.zioMaelstrom.protocol.*

@jsonDiscriminator("type")
enum Incoming extends NeedsReply derives JsonDecoder {

  @jsonHint("topology") case Topology(
      msg_id: MessageId,
      topology: Map[NodeId, List[NodeId]]
  )

  @jsonHint("broadcast") case Broadcast(msg_id: MessageId, message: Int)

  @jsonHint("read") case Read(msg_id: MessageId)
}

case class BroadcastOk(in_reply_to: MessageId, `type`: String = "broadcast_ok")
    extends Sendable,
      Reply derives JsonEncoder

case class ReadOk(
    messages: Set[Int],
    in_reply_to: MessageId,
    `type`: String = "read_ok"
) extends Sendable,
      Reply
    derives JsonEncoder

case class TopologyOk(in_reply_to: MessageId, `type`: String = "topology_ok")
    extends Sendable,
      Reply derives JsonEncoder

object MainA extends ZIOAppDefault {

  import Incoming.*

  val handler = receive[Incoming] {
    case Broadcast(msg_id, message) =>
      for {
        ref <- ZIO.service[Ref[Set[Int]]]
        _   <- ref.update(_ + message)
        _   <- reply(BroadcastOk(in_reply_to = msg_id))
      } yield ()
    case Read(msg_id) =>
      for {
        ref      <- ZIO.service[Ref[Set[Int]]]
        messages <- ref.get
        _        <- reply(ReadOk(in_reply_to = msg_id, messages = messages))
      } yield ()
    case Topology(msg_id, _) =>
      for {
        _ <- reply(TopologyOk(in_reply_to = msg_id))
      } yield ()
  }

  val run =
    handler.provide(MaelstromRuntime.live, ZLayer(Ref.make(Set.empty[Int])))
}
