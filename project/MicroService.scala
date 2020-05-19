import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport._
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import play.routes.compiler.InjectedRoutesGenerator
import uk.gov.hmrc.ServiceManagerPlugin.Keys.itDependenciesList
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning

trait MicroService {

  import uk.gov.hmrc._
  import DefaultBuildSettings._
  import uk.gov.hmrc.{SbtBuildInfo, ShellPrompt}
  import uk.gov.hmrc.SbtAutoBuildPlugin
  import play.sbt.routes.RoutesKeys.routesGenerator
  import TestPhases.oneForkedJvmPerTest
  import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

  val appName: String

  lazy val appDependencies : Seq[ModuleID] = ???
  lazy val plugins : Seq[Plugins] = Seq(play.sbt.PlayScala)
  lazy val playSettings : Seq[Setting[_]] = Seq.empty

  lazy val externalServices = List(
    // ExternalService("AUTH"),
    // ExternalService("USER_DETAILS"),
    // ExternalService("PREFERENCES"),
    // ExternalService("MESSAGE"),

  ExternalService("AUTH"),
  ExternalService("AUTH_LOGIN_API"),
  ExternalService("USER_DETAILS"),
  ExternalService("PREFERENCES"),
  ExternalService("IDENTITY_VERIFICATION", enableTestOnlyEndpoints = true),
    ExternalService("EMAIL"),
    ExternalService("ENTITY_RESOLVER"),
    // ExternalService("HMRCDESKPRO"),
    // ExternalService("SA"),
    ExternalService("DATASTREAM", enableTestOnlyEndpoints = true)
  )


  lazy val microservice = Project(appName, file("."))
    .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
    .settings( majorVersion := 1 )
    .enablePlugins(Seq(play.sbt.PlayScala) ++ plugins : _*)
    .settings(playSettings : _*)
    .settings(scalaSettings: _*)
    .settings(publishingSettings: _*)
    .settings(defaultSettings(): _*)
    .settings(
      targetJvm := "jvm-1.8",
      scalaVersion := "2.11.11",
      libraryDependencies ++= appDependencies,
      parallelExecution in Test := false,
      fork in Test := false,
      retrieveManaged := true,
      evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
      routesGenerator := InjectedRoutesGenerator,
      scalacOptions ++= List(
        "-feature",
        "-language:postfixOps",
        "-language:reflectiveCalls",
        "-Xlint:-missing-interpolator"
      )
    )
    .configs(IntegrationTest)
    .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
    .settings(ServiceManagerPlugin.serviceManagerSettings)
    .settings(itDependenciesList := externalServices)
    .settings(
      Keys.fork in IntegrationTest := false,
      unmanagedSourceDirectories in IntegrationTest <<= (baseDirectory in IntegrationTest)(base => Seq(base / "it")),
      addTestReportOption(IntegrationTest, "int-test-reports"),
      testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
      parallelExecution in IntegrationTest := false
      // inConfig(IntegrationTest)(
      //   scalafmtCoreSettings ++
      //     Seq(
      //       compileInputs in compile := Def.taskDyn {
      //         val task = test in (resolvedScoped.value.scope in scalafmt.key)
      //         val previousInputs = (compileInputs in compile).value
      //         task.map(_ => previousInputs)
      //       }.value
      //     )
      // )
    )
    .settings(resolvers ++= Seq( Resolver.jcenterRepo))
}

private object TestPhases {

  def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
    tests map {
      test => new Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
    }
}
