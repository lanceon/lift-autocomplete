package com.webitoria
package ajaxautocompleter

import net.liftweb.http.{JsonResponse, LiftRules, S, PlainTextResponse}
import net.liftweb.http.js.{JsCmds, JsCmd}
import xml.{NodeSeq, Elem}
import net.liftweb.util.Helpers
import net.liftweb.http.js.JE.{Str, JsArray, JsObj, JsRaw}
import net.liftweb.http.js.JsCmds.{Run, Script}
import net.liftweb.http.SHtml._
import net.liftweb.http.S._
import Helpers._
import net.liftweb.common.{Logger, Box}
import net.liftweb.json.JsonAST.JString


object AjaxAutocompleter extends Logger {

  def autocompleteScriptAddon = NodeSeq.Empty // implies jQuery and jQuery UI with Autocomplete are already embedded

  def autocompleter(default: String,
                    filteredOptions: String => List[String],
                    isValid: String => Boolean,
                    onSelect: String => Unit,
                    //onBlur: String => JsCmd,
                    jqOptions: List[(String, String)],
                    attrs: (String, String)*) =
  {
    val id = Helpers.nextFuncName

    def jsonResponder(param: String) = {
      //appLogger.debug("q: " + S.params("q"))
      val q = S.param("q").openOr("")
      val fo = filteredOptions(q)
      val offers = fo.map(_.toString)
      val resp = JsArray(offers.map(Str(_)))
      debug("JSON responder: query: [%s], f.options: [%s], resp: [%s]".format(param, fo, resp))
      JsonResponse(resp)
    }

    def onBlur(s: String) : JsCmd = {
      debug("blur server handler: [%s] ".format(s))
      if (isValid(s)) {
        debug("value is valid, keeping input")
        onSelect(s) // else INVALID VALUE IS LEFT
        JsCmds.Noop
      } else {
        debug("value is not valid - clearing input")
        Run(""" jQuery("#""" + id + """").val('');  """)
      }
    }

    def onSelectWrapper(s: String) {
      //appLogger.debug("onSelectWrapper: [%s]".format(s))
      onSelect(s)
    }

    fmapFunc(SFuncHolder(jsonResponder)) { jsonResponderFuncName =>
    fmapFunc(SFuncHolder(onBlur)) { blurFuncName =>
    fmapFunc(SFuncHolder(onSelectWrapper)) { selectFuncName =>

      def ajaxGetOptionsUrl(param: String) = encodeURL(
        S.contextPath + "/" + LiftRules.ajaxPath + "?" + jsonResponderFuncName + "&q="
      ).encJs + "+encodeURIComponent(requestStr)"

      // blur ajax handler
      val rawBlur = (funcName: String, value: String) => JsRaw("'" +  funcName + "=' + encodeURIComponent(" + value + ".value)")
      val onBlur: (String, String) = ("onblur", makeAjaxCall(rawBlur(blurFuncName, "this")).toJsCmd)

      // final selection ajax handler
      val onSelectJsFunc = makeAjaxCall(  JsRaw("'" +  selectFuncName + "=' + encodeURIComponent(v)")  ).toJsCmd

      val encodedJqOptions = jqOptions.map(opt => ", %s: %s".format(opt._1, opt._2)).mkString(", ")
      val onLoad = JsRaw("""

        jQuery(document).ready(function() {

          var submitSelectedValue = function (v) {
              """ + onSelectJsFunc  + """
          }

          jQuery("#""" + id + """").autocomplete({
             change: function(ev, ui) {
               // Triggered when the field is blurred, if the value has changed.
               var str = (ui && ui.item && ui.item.hasOwnProperty("value")) ? ui.item.value : '';
               submitSelectedValue(str);
             },
             select: function(ev, ui) {
               // Triggered when an item is selected from the menu. The default action is to
               // replace the text field's value with the value of the selected item. Canceling this
               // event prevents the value from being updated, but does not prevent the menu from closing.
               var term = ui.item.value || "";
               //console.log("Setting selection for submit: ");
               submitSelectedValue(term);
               //console.log(ui);
             },
             source: function(request, responseCb) {
               // get matching options from server via ajax call
               var requestStr = (request && request.hasOwnProperty('term')) ? request.term : '';
               $.get(""" + ajaxGetOptionsUrl("requestStr") + """, function(data, textStatus, jqXHR) {
                  if (textStatus=="success") {
                    responseCb(data);
                  } else {
                    responseCb([]);
                    alert("Failed to get autocomplete options");
                  }
               });
             }
             """ + encodedJqOptions  + """
          }).focus(function() { $(this).autocomplete('search', '');  });
        });""").cmd // TODO: fix get request

      <span>
        <head_merge>
          { autocompleteScriptAddon }
          { Script(onLoad) }
        </head_merge>
        { attrs.foldLeft(<input type="text" id={id} value={default} />)(_ % _) % onBlur }
      </span>

    }
    }
    }

  }

}
