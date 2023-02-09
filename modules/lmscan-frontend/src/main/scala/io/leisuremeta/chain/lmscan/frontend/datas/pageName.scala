package io.leisuremeta.chain.lmscan.frontend

enum PageName:
  case DashBoard, Blocks, Transactions, NoPage
  case TransactionDetail(hash: String) extends PageName
  case BlockDetail(hash: String)       extends PageName
  case NftDetail(hash: String)         extends PageName
  case AccountDetail(hash: String)     extends PageName
  case Page64                          extends PageName
  case None                            extends PageName

object CustomMap:
  val PageMap = Map(
    "40" -> PageName.AccountDetail,
    25   -> PageName.NftDetail,
    64   -> PageName.Page64,
  )

  def getPage(search: PageName): PageName =
    search match
      case PageName.DashBoard    => search
      case PageName.Blocks       => search
      case PageName.Transactions => search
      case _ =>
        search.toString().length() match
          case 40 => PageName.AccountDetail(search.toString())
          case 25 => PageName.NftDetail(search.toString())
          case 64 => PageName.Page64
          case _  => PageName.None

  def getUrl(page: Int) = List(40, 25, 64).contains(page) match
    case true  => PageMap(page)
    case false => PageName.None
