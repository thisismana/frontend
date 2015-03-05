package test

import common.ExecutionContexts
import conf.{LiveContentApi, Configuration}
import java.net.URLEncoder
import contentapi.Http
import org.scalatestplus.play._
import play.api.test._
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import java.io.File
import recorder.ContentApiHttpRecorder
import play.api.GlobalSettings
import org.apache.commons.codec.digest.DigestUtils
import com.gargoylesoftware.htmlunit.BrowserVersion

trait TestSettings {
  def globalSettingsOverride: Option[GlobalSettings] = None
  def testPlugins: Seq[String] = Nil
  def disabledPlugins: Seq[String] = Seq(
    "conf.SwitchBoardPlugin"
  )

  val recorder = new ContentApiHttpRecorder {
    override lazy val baseDir = new File(System.getProperty("user.dir"), "data/database")
  }

  private def verify(property: String, hash: String, message: String) {
    if (DigestUtils.sha256Hex(property) != hash) {

      // the println makes it easier to spot what is wrong in tests
      println()
      println(s"----------- $message -----------")
      println()

      throw new RuntimeException(message)
    }
  }

  private def toRecorderHttp(http: Http) = new Http {

    val originalHttp = http

    verify(
      Configuration.contentApi.contentApiLiveHost,
      "37f3bee67d016a9fec7959aa5bc5e53fa7fdc688f987c0dea6fa0f6af6979079",
      "YOU ARE NOT USING THE CORRECT ELASTIC SEARCH LIVE CONTENT API HOST"
    )

    Configuration.contentApi.key.map { k =>
        verify(
          k,
          "a4eb3e728596c7d6ba43e3885c80afcb16bc24d22fc0215409392bac242bed96",
          "YOU ARE NOT USING THE CORRECT CONTENT API KEY"
        )
    }

    override def GET(url: String, headers: Iterable[(String, String)]) = {
      recorder.load(url, headers.toMap) {
        originalHttp.GET(url, headers)
      }
    }
  }

  LiveContentApi._http = toRecorderHttp(LiveContentApi._http)
}

trait ConfiguredTestSuite extends ConfiguredServer with ConfiguredBrowser with ExecutionContexts {
  this: ConfiguredTestSuite with org.scalatest.Suite =>

  lazy val host = s"http://localhost:${port}"
  lazy val htmlUnitDriver = webDriver.asInstanceOf[HtmlUnitDriver]
  lazy val testBrowser = TestBrowser(webDriver, None)

  def apply[T](path: String)(block: TestBrowser => T): T = UK(path)(block)

  def UK[T](path: String)(block: TestBrowser => T): T = goTo(path)(block)

  def US[T](path: String)(block: TestBrowser => T): T = {
      val editionPath = if (path.contains("?")) s"$path&_edition=US" else s"$path?_edition=US"
      goTo(editionPath)(block)
  }

  protected def goTo[T](path: String)(block: TestBrowser => T): T = {
      // http://stackoverflow.com/questions/7628243/intrincate-sites-using-htmlunit
      htmlUnitDriver.setJavascriptEnabled(false)
      testBrowser.goTo(host + path)
      block(testBrowser)
  }

  def withHost(path: String) = s"http://localhost:$port$path"

}

trait SingleServerSuite extends OneServerPerSuite with TestSettings with OneBrowserPerSuite with HtmlUnitFactory {
  this: SingleServerSuite with org.scalatest.Suite =>

  BrowserVersion.setDefault(BrowserVersion.CHROME)

  implicit override lazy val app = FakeApplication(
    withoutPlugins = disabledPlugins,
      withGlobal = globalSettingsOverride,
      additionalPlugins = testPlugins,
      additionalConfiguration = Map(
        ("application.secret", "this_is_not_a_real_secret_just_for_tests"),
        ("guardian.projectName", "test-project"),
        ("ws.compressionEnabled", true)
      )
  )
}

object TestRequest {
  // MOST of the time we do not care what path is set on the request - only need to override where we do
  def apply(path: String = "/does-not-matter"): FakeRequest[play.api.mvc.AnyContentAsEmpty.type] = {
    FakeRequest("GET", if (!path.startsWith("/")) s"/$path" else path)
  }
}
