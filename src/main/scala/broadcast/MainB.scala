package broadcast

import zio.*
import zio.json.*

import com.bilalfazlani.zioMaelstrom.*
import com.bilalfazlani.zioMaelstrom.protocol.*

object MainB extends ZIOAppDefault {

  @jsonDiscriminator("type")
  sealed trait Incoming derives JsonDecoder

  @jsonHint("topology")
  final case class Topology(
      msg_id: MessageId,
      topology: Map[NodeId, List[NodeId]]
  ) extends Incoming,
        NeedsReply

  @jsonHint("broadcast")
  final case class Broadcast(msg_id: MessageId, message: Int) extends Incoming, NeedsReply

  @jsonHint("read")
  final case class Read(msg_id: MessageId) extends Incoming, NeedsReply

  @jsonHint("gossip")
  final case class Gossip(message: Set[Int], `type`: String = "gossip") extends Incoming, Sendable derives JsonEncoder

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

  final case class State(messages: Set[Int] = Set.empty, neighbors: Set[NodeId] = Set.empty) {
    def add(message: Int) = copy(messages = messages + message)
    def addAll(messages: Set[Int]) = copy(messages = this.messages ++ messages)
    def topology(newNeighbors: List[NodeId]) =
      copy(neighbors = neighbors ++ newNeighbors)
  }

  object State {
    val live = ZLayer.fromZIO(Ref.make(State()))
    val messages = ZIO.serviceWithZIO[Ref[State]](_.get.map(_.messages))
    def add(message: Int) =
      ZIO.serviceWithZIO[Ref[State]](_.update(_.add(message)))
    def addAll(messages: Set[Int]) =
      ZIO.serviceWithZIO[Ref[State]](_.update(_.addAll(messages)))
    def topology(newNeighbors: List[NodeId]) =
      ZIO.serviceWithZIO[Ref[State]](_.update(_.topology(newNeighbors)))
    def get = ZIO.serviceWithZIO[Ref[State]](_.get)
  }

  def gossip(state: State) = ZIO.foreach(state.neighbors) { neighbor =>
    neighbor.send(Gossip(message = state.messages))
  }

  val handler = receive[Incoming] {
    case Broadcast(msg_id, message) =>
      for {
        _ <- State.add(message)
        state <- State.get
        _ <- gossip(state).forkScoped
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

    case Gossip(messages, _) =>
      for {
        _ <- State.addAll(messages)
        state <- State.get
        _ <- gossip(state).forkScoped
      } yield ()
  }

  val run =
    handler.provide(MaelstromRuntime.live, State.live, Scope.default)
}
