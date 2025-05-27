package io.leisuremeta.chain.lmscan.agent
package service

import doobie.util.transactor.Transactor
import doobie.*
import doobie.implicits.*
import cats.*
import cats.effect.*
import io.leisuremeta.chain.lmscan.agent.apps.*
import io.leisuremeta.chain.lmscan.agent.apps.NftApp.{Nft, NftFile, NftOwner}

given nftWrite: Write[Nft] =
  Write[(String, String, String, String, String, Long)].contramap(nft =>
    (
      nft.txHash,
      nft.tokenId,
      nft.action,
      nft.fromAddr,
      nft.toAddr,
      nft.eventTime,
    ),
  )
given nftFileWrite: Write[NftFile] = Write[
  (
      String,
      String,
      String,
      String,
      String,
      String,
      String,
      String,
      String,
      Long,
  ),
].contramap(nft =>
  (
    nft.tokenId,
    nft.tokenDefId,
    nft.collectionName,
    nft.nftName,
    nft.nftUri,
    nft.creatorDescription,
    nft.dataUrl,
    nft.rarity,
    nft.creator,
    nft.eventTime,
  ),
)
given nftOwnerWrite: Write[NftOwner] =
  Write[(String, String, Long)].contramap(nft =>
    (nft.tokenId, nft.ownerAddr, nft.eventTime),
  )

case class NftRepository[F[_]: MonadCancelThrow](xa: Transactor[F]):
  def putNftList(nft: List[Nft]) =
    Update[Nft](
      """insert into nft (tx_hash, token_id, action, from_addr, to_addr, event_time) 
        values(?, ?, ?, ?, ?, ?) on conflict (tx_hash) do nothing""",
    ).updateMany(nft).transact(xa).attemptSql

  def putNftFileList(nft: List[NftFile]) =
    Update[NftFile](
      "insert into nft_file (token_id, token_def_id, collection_name, nft_name, nft_uri, creator_description, data_url, rarity, creator, event_time) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?) on conflict (token_id) do nothing",
    ).updateMany(nft).transact(xa).attemptSql

  def putNftOwnerList(nft: List[NftOwner]) =
    Update[NftOwner](
      "insert into nft_owner (token_id, owner, event_time) values(?,?,?) on conflict (token_id) do update set owner = excluded.owner where excluded.event_time > nft_owner.event_time",
    ).updateMany(nft).transact(xa).attemptSql

object NftRepository:
  def build[F[_]: MonadCancelThrow](xa: Transactor[F]) = NftRepository(xa)
