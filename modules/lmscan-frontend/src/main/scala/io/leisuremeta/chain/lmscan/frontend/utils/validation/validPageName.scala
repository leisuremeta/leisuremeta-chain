package io.leisuremeta.chain.lmscan.frontend
import scala.util.chaining.*
import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*
import io.leisuremeta.chain.lmscan.frontend.Log.log
import io.leisuremeta.chain.lmscan.frontend.Log.log2

object ValidPageName:
  val subtypeList =
    List(
      ":MintFungibleToken",
      ":AddAccounts",
      ":CreateAccount",
      ":UpdateAccount",
      ":AddPublicKeySummaries",
      ":CreateGroup",
      ":AddAccounts",
      ":DefineToken",
      ":BurnFungibleToken",
      ":MintNFT",
      ":BurnNFT",
      ":EntrustNFT",
      ":TransferNFT",
      ":DisposeEntrustedNFT",
      ":EntrustFungibleToken",
      ":MintFungibleToken",
      ":TransferFungibleToken",
      ":DisposeEntrustedFungibleToken",
      ":OfferReward",
      ":RegisterDao",
      ":UpdateDao",
      ":RecordActivity",
      ":BuildSnapshot",
      ":ExecuteOwnershipReward",
      ":SuggestSimpleAgenda",
      ":VoteSimpleAgenda",
    )
  def getPageFromString(search: String): PageCase | CommandCaseMode |
    CommandCaseLink =
    search match
      case ":on"    => CommandCaseMode.Development
      case ":off"   => CommandCaseMode.Production
      case ":prod"  => CommandCaseLink.Production
      case ":dev"   => CommandCaseLink.Development
      case ":local" => CommandCaseLink.Local
      case subtype if subtypeList.contains(subtype.replaceAll(" ", "")) =>
        Transactions(pubs =
          List(PubCase.TxPub(subtype = subtype.replaceAll(" ", "").tail)),
        )
      case _ =>
        search.length() match
          case 40 =>
            AccountDetail(
              name = AccountDetail().name,
              url = s"account/${search}",
              pubs = List(
                PubCase.AccountDetailPub(hash = search),
                PubCase.TxPub(
                  page = 1,
                  accountAddr = search,
                  sizePerRequest = 10,
                ),
              ),
            )
          case 42 =>
            AccountDetail(
              name = AccountDetail().name,
              url = s"account/${search}",
              pubs = List(
                PubCase.AccountDetailPub(hash = search),
                PubCase.TxPub(
                  page = 1,
                  accountAddr = search,
                  sizePerRequest = 10,
                ),
              ),
            )
          case 25 =>
            NftDetail(
              url = s"nft/${search}",
              pubs = List(PubCase.NftDetailPub(hash = search)),
            )
          case 64 =>
            TxDetail(
              name = Transactions().name,
              url = s"transaction/${search}",
              pubs = List(PubCase.TxDetailPub(hash = search)),
            )
          case _ =>
            AccountDetail(
              name = AccountDetail().name,
              url = s"account/${search}",
              pubs = List(
                // PubCase.BoardPub(),
                PubCase.AccountDetailPub(hash = search),
                PubCase.TxPub(
                  page = 1,
                  accountAddr = search,
                  sizePerRequest = 10,
                ),
              ),
            )

  def getPageCaseFromUrl(url: String): PageCase =
    url match
      case s"/dashboard" => DashBoard()
      case s"/blocks"    => Blocks()
      case s"/blocks/${page}"
          if page.forall(
            Character.isDigit,
          ) =>
        Blocks(
          url = s"blocks/${limit_value(page)}",
          pubs = List(PubCase.BlockPub(page = limit_value(page))),
        )
      case s"/transactions" => Transactions()
      case s"/transactions/${page}"
          if page.forall(
            Character.isDigit,
          ) =>
        Transactions(
          url = s"transactions/${limit_value(page)}",
          pubs = List(PubCase.TxPub(page = limit_value(page))),
        )
      case s"/txs" => Transactions()
      case s"/txs/${page}"
          if page.forall(
            Character.isDigit,
          ) =>
        Transactions(
          url = s"transactions/${limit_value(page)}",
          pubs = List(PubCase.TxPub(page = limit_value(page))),
        )

      case s"/transaction/${hash}" if hash.length() == 64 =>
        TxDetail(
          name = Transactions().name,
          url = s"transaction/${hash}",
          pubs = List(PubCase.TxDetailPub(hash = hash)),
        )
      case s"/tx/${hash}" if hash.length() == 64 =>
        TxDetail(
          name = Transactions().name,
          url = s"transaction/${hash}",
          pubs = List(PubCase.TxDetailPub(hash = hash)),
        )
      case s"/block/${hash}" if hash.length() == 64 =>
        BlockDetail(
          name = Blocks().name,
          url = s"block/${hash}",
          pubs = List(
            PubCase.BlockDetailPub(
              hash = hash,
            ),
            PubCase.TxPub(
              page = 1,
              blockHash = hash,
              sizePerRequest = 10,
            ),
          ),
        )

      case s"/nft/${hash}" if hash.length() == 25 =>
        NftDetail(
          url = s"nft/${hash}",
          pubs = List(PubCase.NftDetailPub(hash = hash)),
        )
      case s"/account/${hash}" =>
        AccountDetail(
          name = AccountDetail().name,
          url = s"account/${hash}",
          pubs = List(
            PubCase.AccountDetailPub(hash = hash),
            PubCase.BoardPub(),
            PubCase.TxPub(
              page = 1,
              accountAddr = hash,
              sizePerRequest = 10,
            ),
          ),
        )

      case _ => DashBoard()
