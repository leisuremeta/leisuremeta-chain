package io.leisuremeta.chain.lib.util.refined

import eu.timepit.refined.api.Validate
import eu.timepit.refined.collection.Size
import eu.timepit.refined.internal.Resources
import scodec.bits.BitVector

object bitVector extends BitVectorValidate

private[refined] trait BitVectorValidate:
  given bitVectorSizeValidate[P, RP](using
      v: Validate.Aux[Long, P, RP],
  ): Validate.Aux[BitVector, Size[P], Size[v.Res]] =
    new Validate[BitVector, Size[P]]:

      override type R = Size[v.Res]

      override def validate(t: BitVector): Res =
        val r = v.validate(t.size)
        r.as(Size(r))

      override def showExpr(t: BitVector): String = v.showExpr(t.size)

      override def showResult(t: BitVector, r: Res): String =
        val size   = t.size
        val nested = v.showResult(size, r.detail.p)
        Resources.predicateTakingResultDetail(s"size($t) = $size", r, nested)
