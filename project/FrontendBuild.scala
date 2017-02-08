import sbt._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning

object FrontendBuild extends Build with MicroService {

  val appName = "customer-advisors-frontend"

  override lazy val plugins: Seq[Plugins] = Seq(
    SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin
  )

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  import play.sbt.PlayImport._
  import play.core.PlayVersion

  def apply() = Seq(

    ws,
    "uk.gov.hmrc"                 %% "frontend-bootstrap"    % "7.10.0",
    "uk.gov.hmrc"                 %% "play-authorised-frontend" % "6.2.0",
    "uk.gov.hmrc"                 %% "play-health"            % "2.0.0",
    "uk.gov.hmrc"                 %% "play-config"            % "3.0.0",
    "uk.gov.hmrc"                 %% "logback-json-logger"    % "3.1.0",
    "uk.gov.hmrc"                 %% "play-partials"          % "5.2.0",
    "uk.gov.hmrc"                 %% "govuk-template"         % "5.0.0",
    "uk.gov.hmrc"                 %% "play-ui"                % "5.2.0",
    "org.jsoup"                   %  "jsoup"                  % "1.9.2" % "test",
    "com.github.tomakehurst"      %  "wiremock"               % "1.58"  % "test",
    "uk.gov.hmrc"                 %% "hmrctest"               % "2.1.0" % "test",
    "org.scalatest"               %% "scalatest"              % "2.2.6" % "test",
    "org.pegdown"                 %  "pegdown"                % "1.6.0" % "test",
    "org.mockito"                 %  "mockito-core"           % "1.9.0" % "test",
    "org.scalatestplus.play"      %% "scalatestplus-play"     % "1.5.1" % "test",
    "com.typesafe.play"           %% "play-test"              % PlayVersion.current % "test"
  )
}


