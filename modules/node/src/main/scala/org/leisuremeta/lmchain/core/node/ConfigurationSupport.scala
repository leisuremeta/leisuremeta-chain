package org.leisuremeta.lmchain.core
package node

import eu.timepit.refined.pureconfig._
import pureconfig.ConfigReader
import shapeless.tag
import shapeless.tag.@@

trait ConfigurationSupport {
  def taggedReader[A: ConfigReader, B]: ConfigReader[A @@ B] =
    ConfigReader[A].map(tag[B][A])

  implicit val networkIdReader: ConfigReader[model.NetworkId] =
    taggedReader[datatype.BigNat, model.NetworkIdTag]
}
