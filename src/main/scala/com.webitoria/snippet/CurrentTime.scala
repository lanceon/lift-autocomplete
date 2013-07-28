package com.webitoria
package snippet

import xml.NodeSeq
import net.liftweb.util.Helpers
import Helpers._

class CurrentTimeSnippet {

  val date = new java.util.Date().toString

  def render(in:NodeSeq) : NodeSeq = (".time" #> date).apply(in)

}
