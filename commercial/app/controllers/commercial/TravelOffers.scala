package controllers.commercial

import model.commercial.travel.{TravelOffer, TravelOffersAgent}
import model.{NoCache, Cached}
import performance.MemcachedAction
import play.api.mvc._
import scala.concurrent.Future

object TravelOffers extends Controller with implicits.Requests {

  def renderTravel = MemcachedAction { implicit request =>
    Future.successful {
      (TravelOffersAgent.specificTravelOffers(specificIds) ++ TravelOffersAgent.offersTargetedAt(segment)).distinct match {
        case Nil => NoCache(jsonFormat.nilResult)
        case offers => Cached(componentMaxAge) {
          val clickMacro = request.getParameter("clickMacro")
          val omnitureId = request.getParameter("omnitureId")

          request.getParameter("layout") match {
            case Some("prominent") => jsonFormat.result(views.html.travel.travelProminent(offers.take(4), omnitureId, clickMacro))
            case _ => jsonFormat.result(views.html.travel.travelStandard(offers.take(4), omnitureId, clickMacro))
          }
        }
      }
    }
  }

}
