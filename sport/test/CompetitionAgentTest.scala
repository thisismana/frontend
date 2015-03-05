package test

import feed.{CompetitionSupport, Competitions, CompetitionAgent}
import model.Competition
import org.scalatest.{DoNotDiscover, FlatSpec, Matchers}
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Span}
import org.joda.time.LocalDate


@DoNotDiscover class CompetitionAgentTest extends FlatSpec with Matchers with implicits.Football with Eventually with ConfiguredTestSuite {

  override implicit val patienceConfig = PatienceConfig(timeout = scaled(Span(3000, Millis)), interval = scaled(Span(100, Millis)))

  lazy val seasonStart = Some(new LocalDate(2012, 8, 1))

  "CompetitionAgentTest" should "load fixtures" in {

    object TestCompetitions extends Competitions {
      override val competitionAgents = Seq(
        CompetitionAgent(Competition("100", "/football/premierleague", "Premier League", "Premier League", "English", showInTeamsList = true))
      )

      def matches = CompetitionSupport(competitionAgents.map(_.competition)).matches
    }

    TestCompetitions.competitionAgents.foreach(_.refreshFixtures())

    eventually(
      TestCompetitions.matches.filter(_.isFixture).map(_.id) should contain ("3519484")
    )
  }

  it should "load results" in {

    object TestCompetitions extends Competitions {
      override val competitionAgents = Seq(
        CompetitionAgent(Competition("100", "/football/premierleague", "Premier League", "Premier League", "English", showInTeamsList = true, startDate = seasonStart))
      )
      def matches = CompetitionSupport(competitionAgents.map(_.competition)).matches
    }

    TestCompetitions.competitionAgents.foreach(_.refreshResults())

    eventually(
      TestCompetitions.matches.filter(_.isResult).map(_.id) should contain ("3528302")
    )
  }

  it should "load live matches" in {

    object TestCompetitions extends Competitions {
      override val competitionAgents = Seq(
        CompetitionAgent(Competition("100", "/football/premierleague", "Premier League", "Premier League", "English", showInTeamsList = true))
      )
      def matches = CompetitionSupport(competitionAgents.map(_.competition)).matches
    }

    TestCompetitions.refreshMatchDay()

    eventually(TestCompetitions.matches.filter(_.isLive).map(_.id) should contain ("3518286"))

  }

  it should "load league tables" in {

    object TestCompetitions extends Competitions {
      override val competitionAgents = Seq(
        CompetitionAgent(Competition("100", "/football/premierleague", "Premier League", "Premier League", "English", showInTeamsList = true))
      )
      override def competitions = competitionAgents.map(_.competition)
    }

    TestCompetitions.competitionAgents.foreach(_.refresh())

    eventually(TestCompetitions.competitions(0).leagueTable(0).team.id should be ("4"))
  }

}
