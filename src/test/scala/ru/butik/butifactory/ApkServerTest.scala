package ru.butik.butifactory

import java.io.File

import better.files.Resource
import com.twitter.finagle.http.Response
import com.twitter.util.Future
import net.dongliu.apk.parser.ApkFile
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfter, FunSpec}

class ApkServerTest extends FunSpec
  with BeforeAndAfter
  with MockFactory {

  private val apkFile = Resource.getUrl("apk/app-release.apk").getFile

  private val datastore = mock[Datastore]
  private val storage = mock[ArtifactStorageBackend]
  private val pushService = mock[PushService]
  private val service = new ApkService(datastore, storage, pushService)
  private val apk = new ApkFile(apkFile)
  private val apkContainer = ApkFileContainer(apk, new File(apkFile))

  it("should check for artifact") {
    (datastore.findArtifactByName _).expects(*).returning(None)

    assert(service.uploadFile(apkContainer) === Left("artifact not found"))
  }

  it("should check for version") {
    (datastore.findArtifactByName _).expects(*).returning(Some(Artifact("name")))
    (datastore.findArtifactVersion _).expects(*, *).returning(Some(ArtifactVersion("name", "1.2.3", 6, "file")))

    assert(service.uploadFile(apkContainer) === Left("already exist"))
  }

  it("should create artifact and store in storage") {
    val expect = ArtifactVersion("ru.butik.fitassist", "1.1.3", 6, "ru.butik.fitassist/ru.butik.fitassist-6.apk")

    (datastore.findArtifactByName _).expects(*).returning(Some(Artifact("name")))
    (datastore.findArtifactVersion _).expects(*, *).returning(None)
    (datastore.createArtifactVersion _)
      .expects(expect.name, expect.version, expect.versionCode, expect.filename)
      .returning(expect)
    (datastore.fetchSubscriptions _).expects(expect.name).returns(List(Subscription(expect.name, "123")))
    (pushService.pushDevice _).expects("123", *).returns(Future { Response() })

    (storage.storeArtifact _).expects(expect.filename, apkContainer.file)

    assert(service.uploadFile(apkContainer) === Right(expect))
  }
}
