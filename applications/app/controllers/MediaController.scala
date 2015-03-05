package controllers

import com.gu.contentapi.client.model.{Content => ApiContent, ItemResponse}
import common._
import conf.Configuration.commercial.expiredAdFeatureUrl
import conf.LiveContentApi.getResponse
import conf._
import model._
import play.api.mvc.{Content => _, _}
import views.support.RenderOtherStatus

import scala.concurrent.Future

case class MediaPage(media: Media, related: RelatedContent)

object MediaController extends Controller with RendersItemResponse with Logging with ExecutionContexts {

  def renderJson(path: String) = render(path)
  def render(path: String) = Action.async { implicit request => renderItem(path) }

  private def lookup(path: String)(implicit request: RequestHeader) = {
    val edition = Edition(request)

    log.info(s"Fetching media: $path for edition $edition")
    val response: Future[ItemResponse] = getResponse(
      LiveContentApi.item(path, edition)
        .showFields("all")
    )

    val result = response map { response =>
      val mediaOption: Option[Media] = response.content.filter(isSupported).map {
        case a if a.isAudio => Audio(a)
        case v => Video(v)
      }
      val model = mediaOption map { media => MediaPage(media, RelatedContent(media, response)) }

      if (mediaOption.exists(_.isExpiredAdvertisementFeature)) {
        Right(MovedPermanently(expiredAdFeatureUrl))
      } else {
        ModelOrResult(model, response)
      }

    }

    result recover convertApiExceptions
  }

  private def renderMedia(model: MediaPage)(implicit request: RequestHeader): Result = {
    val htmlResponse = () => views.html.media(model)
    val jsonResponse = () => views.html.fragments.mediaBody(model)
    renderFormat(htmlResponse, jsonResponse, model.media, Switches.all)
  }

  override def renderItem(path: String)(implicit request: RequestHeader): Future[Result] = lookup(path) map {
    case Left(model) => renderMedia(model)
    case Right(other) => RenderOtherStatus(other)
  }

  private def isSupported(c: ApiContent) = c.isVideo || c.isAudio
  override def canRender(i: ItemResponse): Boolean = i.content.exists(isSupported)
}
