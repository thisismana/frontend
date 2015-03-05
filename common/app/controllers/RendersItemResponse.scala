package controllers


import com.gu.contentapi.client.model.ItemResponse
import common.{Edition, ExecutionContexts}
import conf.LiveContentApi
import model.NoCache
import play.api.mvc.{Action, Controller, RequestHeader, Result}

import scala.concurrent.Future
import scala.concurrent.Future._

trait RendersItemResponse {

  def renderItem(path: String)(implicit request: RequestHeader): Future[Result]

  def canRender(item: ItemResponse): Boolean

  def canRender(path: String): Boolean = false

}

class ItemResponseController(val controllers: RendersItemResponse*) extends Controller with ExecutionContexts {

  def render(path: String) = Action.async{ implicit request =>
    val itemRequest = LiveContentApi.item(path, Edition(request))

    controllers.find(_.canRender(path)).map(_.renderItem(path)).getOrElse {
      LiveContentApi.getResponse(itemRequest).flatMap { response =>
        controllers.find(_.canRender(response))
          .map(_.renderItem(path))
          .getOrElse(successful(NoCache(NotFound)))
      }
    }
  }
}
