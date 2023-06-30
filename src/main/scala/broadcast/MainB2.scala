package broadcast

import zio.*
import zio.json.*

import com.bilalfazlani.zioMaelstrom.*
import com.bilalfazlani.zioMaelstrom.protocol.*

object MainB2 extends ZIOAppDefault {

  @jsonDiscriminator("type")
  sealed trait Incoming derives JsonDecoder

  @jsonHint("topology")
  final case class Topology(
      msg_id: MessageId,
      topology: Map[NodeId, List[NodeId]]
  ) extends Incoming,
        NeedsReply

  @jsonHint("broadcast")
  final case class Broadcast(msg_id: MessageId, message: Int, `type`: String = "broadcast")
      extends Incoming,
        NeedsReply,
        Sendable derives JsonEncoder

  @jsonHint("read")
  final case class Read(msg_id: MessageId) extends Incoming, NeedsReply

  final case class BroadcastOk(in_reply_to: MessageId, `type`: String = "broadcast_ok") extends Sendable, Reply
      derives JsonEncoder

  final case class ReadOk(
      messages: Set[Int],
      in_reply_to: MessageId,
      `type`: String = "read_ok"
  ) extends Sendable,
        Reply
      derives JsonEncoder

  final case class TopologyOk(in_reply_to: MessageId, `type`: String = "topology_ok") extends Sendable, Reply
      derives JsonEncoder

  final case class State(
      messages: Set[Int] = Set.empty,
      neighbors: Set[NodeId] = Set.empty,
      hadBroadcasted: Map[NodeId, Set[Int]] = Map.empty
  ) {
    def add(message: Int, src: NodeId) =
      copy(
        messages = messages + message,
        hadBroadcasted = hadBroadcasted.updated(src, hadBroadcasted.getOrElse(src, Set.empty) + message)
      )

    def topology(newNeighbors: List[NodeId]) =
      copy(neighbors = neighbors ++ newNeighbors)
  }

  object State {
    val live = ZLayer.fromZIO(Ref.make(State()))
    val messages = ZIO.serviceWithZIO[Ref[State]](_.get.map(_.messages))
    def add(message: Int, src: NodeId) =
      ZIO.serviceWithZIO[Ref[State]](_.updateAndGet(_.add(message, src)))

    def topology(newNeighbors: List[NodeId]) =
      ZIO.serviceWithZIO[Ref[State]](_.update(_.topology(newNeighbors)))
    def get = ZIO.serviceWithZIO[Ref[State]](_.get)
  }

  def gossip(state: State, message: Int) =
    ZIO.foreach(state.neighbors) { neighbor =>
      val hadBroadcasted = state.hadBroadcasted.getOrElse(neighbor, Set.empty)
      neighbor.send(Broadcast(msg_id = MessageId(1), message = message)).when(!hadBroadcasted.contains(message))
    }

  val handler = receive[Incoming] {
    case Broadcast(msg_id, message, _) =>
      for {
        state <- State.add(message, src)
        _ <- gossip(state, message).forkScoped
        _ <- reply(BroadcastOk(in_reply_to = msg_id))
      } yield ()
    case Read(msg_id) =>
      for {
        messages <- State.messages
        _ <- reply(ReadOk(in_reply_to = msg_id, messages = messages))
      } yield ()
    case Topology(msg_id, topology) =>
      val newNeighbors = topology.getOrElse(me, List.empty)
      for {
        _ <- State.topology(newNeighbors)
        _ <- reply(TopologyOk(in_reply_to = msg_id))
      } yield ()
  }

  val run =
    handler.provide(MaelstromRuntime.live, State.live, Scope.default)
}
