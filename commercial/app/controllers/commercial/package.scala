package controllers

import common.{JsonNotFound, JsonComponent}
import model.commercial.{Context, Segment}
import play.api.mvc._
import scala.concurrent.duration._
import play.twirl.api.Html

package object commercial {

  val componentMaxAge = 15.minutes

  def segment(implicit request: RequestHeader) = {
    val params = request.queryString
    val section = params.get("s") map (_.head)
    val keywords = params getOrElse("k", Nil)
    val userSegments = params getOrElse("seg", Nil)
    Segment(Context(section, keywords), userSegments)
  }

  def specificId(implicit request: RequestHeader): Option[String] = request.queryString.get("t").map(_.head)
  def specificIds(implicit request: RequestHeader): Seq[String] = request.queryString.getOrElse("t", Nil).reverse

  trait Relevance[T] {
    def view(ads: Seq[T])(implicit request: RequestHeader): Html
  }

  trait Format {
    def nilResult(implicit request: RequestHeader): Result
    def result(view: Html)(implicit request: RequestHeader): Result
  }

  object htmlFormat extends Format {
    override def nilResult(implicit request: RequestHeader): Result = Results.NotFound
    override def result(view: Html)(implicit request: RequestHeader): Result = Results.Ok(view)
  }

  object jsonFormat extends Format {
    override def nilResult(implicit request: RequestHeader): Result = JsonNotFound.apply()
    override def result(view: Html)(implicit request: RequestHeader): Result = JsonComponent(view)
  }
}
