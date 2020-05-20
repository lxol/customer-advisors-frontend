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
    "uk.gov.hmrc"            %% "bootstrap-play-26"            % "1.1.0",
    "uk.gov.hmrc"            %% "govuk-template"               % "5.42.0-play-26",
    "uk.gov.hmrc"            %% "domain"                       % "5.6.0-play-26",
    "uk.gov.hmrc"            %% "play-ui"                      % "8.3.0-play-26",
    "uk.gov.hmrc"            %% "play-partials"                % "6.9.0-play-26",
    "uk.gov.hmrc"            %% "play-filters"                 % "5.18.0",
    "com.typesafe.play"      %% "play-json-joda"               % "2.6.13",
    "org.jsoup"              % "jsoup"                         % "1.12.1",
    "org.skyscreamer"        % "jsonassert"                    % "1.4.0",
    "net.codingwell"         %% "scala-guice"                  % "4.2.6",
    "com.github.tomakehurst" % "wiremock-jre8"                 % "2.21.0"               % "test,it",
    "org.pegdown"            % "pegdown"                       % "1.6.0"                % "test,it",
    "org.mockito"            % "mockito-all"                   % "1.10.19"              % "test",
    "org.scalatestplus.play" %% "scalatestplus-play"           % "3.1.0"                % "test,it",
    "uk.gov.hmrc"            %% "service-integration-test"     % "0.9.0-play-26"        % "test, it"
  )
}
