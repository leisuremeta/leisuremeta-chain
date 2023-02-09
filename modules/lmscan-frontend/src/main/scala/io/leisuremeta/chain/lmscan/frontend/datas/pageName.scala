package io.leisuremeta.chain.lmscan.frontend

enum PageName:
  case DashBoard, Blocks, Transactions, NoPage
  case TransactionDetail(hash: String) extends PageName
  case BlockDetail(hash: String)       extends PageName
  case NftDetail(hash: String)         extends PageName
  case AccountDetail(hash: String)     extends PageName
  case Page64                          extends PageName
  case None                            extends PageName

// object PageNumber:
//   // 해시 자릿수에 따른 페이지 렌더링
//   val accountDetail: Int = 40
//   val nftDetail: Int     = 25
//   val handle_64: Int     = 64

object CustomMap:
  val PageMap = Map(
    40 -> PageName.AccountDetail,
    25 -> PageName.NftDetail,
    64 -> PageName.Page64,
  )

  def getPage(search: String) =
    search match
      case PageName.Blocks => PageName.Blocks
      //   List(40, 25, 64).contains(num) match
      // case true  => PageMap(num)
      // case false => PageName.None

  def getUrl(page: Int) = List(40, 25, 64).contains(num) match
    case true  => PageMap(num)
    case false => PageName.None
