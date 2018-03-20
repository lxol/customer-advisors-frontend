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
    "uk.gov.hmrc"                 %% "frontend-bootstrap"       % "8.19.0",
    "uk.gov.hmrc"                 %% "play-partials"            % "6.1.0",
    "org.jsoup"                   %  "jsoup"                    % "1.10.2",
    "com.github.tomakehurst"      %  "wiremock"                 % "1.58"  % "test",
    "uk.gov.hmrc"                 %% "hmrctest"                 % "2.2.0" % "test",
    "org.scalatest"               %% "scalatest"                % "2.2.6" % "test",
    "org.pegdown"                 %  "pegdown"                  % "1.6.0" % "test",
    "org.mockito"                 %  "mockito-core"             % "1.9.0" % "test",
    "org.scalatestplus.play"      %% "scalatestplus-play"       % "1.5.1" % "test",
    "com.typesafe.play"           %% "play-test"                % PlayVersion.current % "test"
  )
}


