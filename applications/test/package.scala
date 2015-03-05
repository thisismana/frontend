package test

import java.util.{ List => JList }
import org.scalatest.Suites
import collection.JavaConversions._

object `package` {

  implicit class ListString2FirstNonEmpty(list: JList[String]) {
    lazy val firstNonEmpty: Option[String] = list find { !_.isEmpty }
  }
}

class ApplicationsTestSuite extends Suites (
  new services.ApplicationsHealthcheckTest,
  new common.CombinerControllerTest,
  new common.CombinerFeatureTest,
  new AllIndexControllerTest,
  new AllIndexTemplateTest,
  new GalleryControllerTest,
  new GalleryTemplateTest,
  new ImageContentControllerTest,
  new ImageContentTemplateTest,
  new IndexControllerTest,
  new InteractiveControllerTest,
  new LatestIndexControllerTest,
  new MediaControllerTest,
  new MediaFeatureTest,
  new SectionTemplateTest,
  new TagFeatureTest,
  new TagTemplateTest,
  new ShareLinksTest ) with SingleServerSuite {

  override lazy val port: Int = conf.HealthCheck.testPort
}
