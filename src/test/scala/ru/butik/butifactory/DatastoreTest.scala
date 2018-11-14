package ru.butik.butifactory

import org.scalatest.FunSpec
import org.flywaydb.core.Flyway

class DatastoreTest extends FunSpec {

  it("should save artifacts") {

    val flyway: Flyway = Flyway.configure().dataSource("jdbc:h2:mem:test1;DB_CLOSE_DELAY=-1", null, null).load
    flyway.migrate

    val db = DatastoreDoobie.init(DatastoreDoobie.testUrl)

    val artifact = Artifact("test")
    assert(db.createArtifact("test") === Artifact("test"))

    assert(db.findArtifactByName("test") === Some(artifact))
    assert(db.findArtifactByName("not exists") === None)

    assert(db.artifacts() === List(artifact))
  }

  it("should save artifact versions") {
    val flyway: Flyway = Flyway.configure().dataSource("jdbc:h2:mem:test1;DB_CLOSE_DELAY=-1", null, null).load
    flyway.migrate

    val db = DatastoreDoobie.init(DatastoreDoobie.testUrl)

    val artifact = "test"
    val version = "1.2.3+Test"
    val filename = "apktest.apk"

    assert(db.createArtifactVersion(artifact, version, filename) === ArtifactVersion(artifact, version, filename))
    assert(db.findArtifactVersion(artifact, version) === Some(ArtifactVersion(artifact, version, filename)))
    assert(db.findArtifactVersion("unknown", version) === None)
  }

  it("should fetch artifact versions") {
    val flyway: Flyway = Flyway.configure().dataSource("jdbc:h2:mem:test1;DB_CLOSE_DELAY=-1", null, null).load
    flyway.migrate

    val db = DatastoreDoobie.init(DatastoreDoobie.testUrl)

    val artifact = "test.one"
    val version = "1.2.3+Test"
    val filename = "apktest.apk"

    assert(db.createArtifactVersion(artifact, version, filename) === ArtifactVersion(artifact, version, filename))
    assert(db.findVersionsBy("test.one") ===
      List(ArtifactVersion(artifact, version, filename)))
  }
}
