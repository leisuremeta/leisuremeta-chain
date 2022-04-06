namespace java org.leisuremeta.lmchain.core.gossip.thrift
#@namespace scala org.leisuremeta.lmchain.core.gossip.thrift

typedef binary uint256

struct TxAndBlockSuggestion {
  1: binary tx,
  2: binary block
}

struct TxAndBlockVote {
  1: binary tx,
  2: binary vote
}

service GossipService {
  binary bestConfirmedBlock();
  binary block(uint256 blockHash);
  binary nameState(binary argz);
  binary tokenState(binary argz);
  binary balanceState(binary argz);
  TxAndBlockSuggestion newTxAndBlockSuggestions(
    binary bloomFilter,
    i16 numberOfItems
  );
  TxAndBlockSuggestion allNewTxAndBlockSuggestions();
  TxAndBlockVote newTxAndBlockVotes(
    binary bloomFilter,
    i16 numberOfItems,
    set<binary> knownBlockVoteKeys,
  );
  TxAndBlockVote allNewTxAndBlockVotes(
    set<binary> knownBlockVoteKeys,
  );
  set<binary> txs(set<uint256> txHashes);
}
