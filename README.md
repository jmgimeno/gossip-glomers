## Gossip Glomers

Playing with [Gossip Glomers Challenges](https://fly.io/dist-sys/) using [zio-maesltrom](https://zio-maelstrom.bilal-fazlani.com/)

### Challenge #1: Echo

```shell
./maelstrom test -w echo --bin ~/Projects-Scala/gossip-glomers/launchers/uuid.sh --node-count 1 --time-limit 10
```

### Challenge #2: Unique ID Generation

```shell
./maelstrom test -w unique-ids --bin ~/Projects-Scala/gossip-glomers/launchers/uuid.sh --time-limit 30 --rate 1000 --node-count 3 --availability total --nemesis partition
```

### Challenge #3a: Single-Node Broadcast

```shell
./maelstrom test -w broadcast --bin ~/Projects-Scala/gossip-glomers/launchers/broadcastA.sh --node-count 1 --time-limit 20 --rate 10
```

### Challenge #3b: Multi-Node Broadcast (ONGOING)

```shell
./maelstrom test -w broadcast --bin ~/Projects-Scala/gossip-glomers/launchers/broadcastB.sh --node-count 5 --time-limit 20 --rate 10
```

```shell
./maelstrom test -w broadcast --bin ~/Projects-Scala/gossip-glomers/launchers/broadcastB2.sh --node-count 5 --time-limit 20 --rate 10
```
