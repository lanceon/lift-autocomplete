package com.webitoria
package snippet

import xml.NodeSeq
import net.liftweb.util.Helpers
import net.liftweb.common.{Logger, Empty}
import net.liftweb.http.js.JsCmds
import ajaxautocompleter.AjaxAutocompleter


class AutocompleteTest extends Logger {

  val allOptions = (10000 to 50000).map(_.toString).toList

  def ac = AjaxAutocompleter.autocompleter(
    "",
    substr => {
      debug("Requesting options for: [%s]".format(substr))
      allOptions.filter(_.toUpperCase.contains(substr.toUpperCase)).take(10)
    },
    toCheck => {
      debug("Validating: [%s]".format(toCheck))
      allOptions.contains(toCheck)
    },
    selected => {
      debug("final selection: [%s]".format(selected))
    },
    ("minLength", "0") :: Nil
  )


  def render(in:NodeSeq) = {
    import Helpers._
    (".custom" #> ac).apply(in)
  }

}
