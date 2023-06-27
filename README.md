## Gossip Glomers

Playing with [Gossip Glomers Challenges](https://fly.io/dist-sys/) using [zio-maesltrom](https://zio-maelstrom.bilal-fazlani.com/)

### echo

```shell
./maelstrom test -w echo --bin ~/Projects-Scala/gossip-glomers/launchers/uuid.sh --node-count 1 --time-limit 10
```

### uuid

```shell
./maelstrom test -w unique-ids --bin ~/Projects-Scala/gossip-glomers/launchers/uuid.sh --time-limit 30 --rate 1000 --node-count 3 --availability total --nemesis partition
```
