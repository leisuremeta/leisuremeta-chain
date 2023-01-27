package io.leisuremeta.chain.lmscan.backend.model

import sttp.tapir.EndpointIO.annotations.endpointInput
import sttp.tapir.EndpointIO.annotations.query
import io.getquill.Ord
import io.getquill.ast.PropertyOrdering
import io.getquill.ast.Asc
import io.getquill.ast.Desc

case class PageNavigation(
    @query
    pageNo: Int,
    @query
    sizePerRequest: Int,
    // @query
    // orderByProperty: Option[String], // ex) id:desc
)
// ):
//   def orderBy(): OrderBy =
//     val items = this.orderByProperty.get.split(":")
//     val x = items(1) match
//       case "asc"  => Ord(Asc)
//       case "desc" => Ord(Desc)
//     val direction = OrderBy.toDirection(items(1))
//     new OrderBy(items(0), direction)

case class OrderBy(
    property: String,
    direction: Ord[Any],
)

object OrderBy:
  def toDirection(order: String): Ord[Any] =
    order match
      case "asc"  => Ord(Asc)
      case "desc" => Ord(Desc)
