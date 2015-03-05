package services

import org.scalatest.{DoNotDiscover, Matchers, FlatSpec}
import play.api.libs.ws.WS
import test._

import scala.concurrent.duration._
import scala.concurrent.Await

@DoNotDiscover class FaciaHealthcheckTest extends FlatSpec with Matchers with ConfiguredTestSuite {

  "Healthchecks" should "pass" in goTo("/uk"){ _ =>
    Await.result(WS.url(s"http://localhost:$port/_healthcheck").get(), 10.seconds).status should be (200)
  }

  "Cdn Healthcheck" should "pass once fronts can be served" in goTo("/uk"){ _ =>
    Await.result(WS.url(s"http://localhost:$port/_fronts_cdn_healthcheck").get(), 10.seconds).status should be (200)
  }
}
