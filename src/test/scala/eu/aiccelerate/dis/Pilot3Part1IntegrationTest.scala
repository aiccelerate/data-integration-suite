package eu.aiccelerate.dis

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import io.onfhir.api.Resource
import io.onfhir.client.OnFhirNetworkClient
import io.onfhir.path.FhirPathUtilFunctionsFactory
import io.onfhir.util.JsonFormatter._
import io.tofhir.engine.mapping.context.{IMappingContextLoader, MappingContextLoader}
import io.tofhir.engine.mapping.job.FhirMappingJobManager
import io.tofhir.engine.mapping.schema.{IFhirSchemaLoader, SchemaFolderLoader}
import io.tofhir.engine.model.{ArchiveModes, DataProcessingSettings, FhirMappingJob, FhirMappingJobExecution, FhirMappingTask, FhirRepositorySinkSettings, FileSystemSource, FileSystemSourceSettings}
import io.tofhir.engine.repository.mapping.{FhirMappingFolderRepository, IFhirMappingRepository}

import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

class Pilot3Part1IntegrationTest extends PilotTestSpec {

  val mappingRepository: IFhirMappingRepository =
    new FhirMappingFolderRepository(Paths.get("mappings/pilot3-p1").toAbsolutePath.toUri)

  val contextLoader: IMappingContextLoader = new MappingContextLoader

  val schemaLoader: IFhirSchemaLoader = new SchemaFolderLoader(Paths.get("schemas/pilot3-p1").toAbsolutePath.toUri)

  val dataSourceSettings = Map("source" -> FileSystemSourceSettings("test-source-1", "http://opbg.it", Paths.get("test-data/pilot3-p1").toAbsolutePath.toString))

  val mappingUrl = "mocked_mapping_url"
  val jobId = "mocked_job_id"
  val sourceFolderPath = "test-archiver-batch"
  val inputFilePath = "test-input-file"
  val testSourceSettings: FileSystemSourceSettings = FileSystemSourceSettings(name = "test", sourceUri = "test", dataFolderPath = sourceFolderPath)
  val testFileSystemSource: FileSystemSource = FileSystemSource(path = inputFilePath)
  val testMappingTask: FhirMappingTask = FhirMappingTask(sourceBinding = Map("_" -> testFileSystemSource), mappingRef = "test")
  val testSinkSettings: FhirRepositorySinkSettings = FhirRepositorySinkSettings(fhirRepoUrl = "test")
  val testDataProcessingSettings: DataProcessingSettings = DataProcessingSettings(archiveMode = ArchiveModes.ARCHIVE)
  val testJob: FhirMappingJob = FhirMappingJob(id = jobId, dataProcessingSettings = testDataProcessingSettings,
    sourceSettings = Map(("_") -> testSourceSettings), sinkSettings = testSinkSettings, mappings = Seq.empty)

  val fhirMappingJobManager = new FhirMappingJobManager(mappingRepository, contextLoader, schemaLoader, Map(FhirPathUtilFunctionsFactory.defaultPrefix -> FhirPathUtilFunctionsFactory), sparkSession)

  val fhirSinkSetting: FhirRepositorySinkSettings = FhirRepositorySinkSettings(fhirRepoUrl = sys.env.getOrElse("FHIR_REPO_URL", "http://localhost:8080/fhir"))
  implicit val actorSystem = ActorSystem("Pilot3Part1IntegrationTest")
  val onFhirClient = OnFhirNetworkClient.apply(fhirSinkSetting.fhirRepoUrl)

  val fhirServerIsAvailable =
    Try(Await.result(onFhirClient.search("Patient").execute(), FiniteDuration(5, TimeUnit.SECONDS)).httpStatus == StatusCodes.OK)
      .getOrElse(false)

  val patientMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot3-p1/patient-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "patients.csv"))
  )
  val conditionMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot3-p1/condition-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "conditions.csv"))
  )

  val encounterMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot3-p1/encounter-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "encounters.csv"))
  )

  val organizationMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot3-p1/organization-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "organizations.csv"))
  )

  val hospitalUnitMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot3-p1/hospitalUnit-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "hospitalUnits.csv"))
  )

  val diagnosticStudiesMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot3-p1/diagnostic-study-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "diagnostic-studies.csv"))
  )

  val procedureMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot3-p1/procedure-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "procedures.csv"))
  )

//  "patient mapping" should "map test data" in {
//    //Some semantic tests on generated content
//    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(patientMappingTask), job = testJob), sourceSettings = dataSourceSettings) map { mappingResults =>
//      val results = mappingResults.map(r => {
//        r.mappedResource shouldBe defined
//        val resource = r.mappedResource.get.parseJson
//        resource shouldBe a[Resource]
//        resource
//      })
//      results.length shouldBe 5
//      (JArray(results.toList) \ "meta" \ "profile").extract[Seq[Seq[String]]].flatten.toSet shouldBe Set("https://aiccelerate.eu/fhir/StructureDefinition/AIC-Patient")
//    }
//  }
//
//  it should "map test data and write it to FHIR repo successfully" in {
//    //Send it to our fhir repo if they are also validated
//    assume(fhirServerIsAvailable)
//    fhirMappingJobManager
//      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(patientMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
//      .map(unit =>
//        unit shouldBe()
//      )
//  }


//  "condition mapping" should "map test data" in {
//    //Some semantic tests on generated content
//    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(conditionMappingTask), job = testJob), sourceSettings = dataSourceSettings) map { mappingResults =>
//      val results = mappingResults.map(r => {
//        r.mappedResource shouldBe defined
//        val resource = r.mappedResource.get.parseJson
//        resource shouldBe a[Resource]
//        resource
//      })
//      results.length shouldBe 5
//
//      (results.apply(1) \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p2")
//
//      (results.apply(3) \ "code" \ "coding" \ "code").extract[Seq[String]].head shouldBe "M89.9"
//      (results.apply(3) \ "code" \ "coding" \ "display").extract[Seq[String]].head shouldBe "Disorder of bone, unspecified"
//
//      (results.head \ "clinicalStatus" \ "coding" \ "code").extract[Seq[String]].head shouldBe "resolved"
//    }
//  }
//
//  it should "map test data and write it to FHIR repo successfully" in {
//    //Send it to our fhir repo if they are also validated
//    assume(fhirServerIsAvailable)
//    fhirMappingJobManager
//      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(conditionMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
//      .map(unit =>
//        unit shouldBe()
//      )
//  }

//  "encounter mapping" should "map test data" in {
//    //Some semantic tests on generated content
//    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(encounterMappingTask), job = testJob), sourceSettings = dataSourceSettings) map { mappingResults =>
//      val results = mappingResults.map(r => {
//        r.mappedResource shouldBe defined
//        val resource = r.mappedResource.get.parseJson
//        resource shouldBe a[Resource]
//        resource
//      })
//      results.length shouldBe 6
//      (results.head \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p1")
//      (results.head \ "type" \ "coding" \ "code").extract[Seq[String]] shouldBe Seq("00000406")
//    }
//  }
//
//  it should "map test data and write it to FHIR repo successfully" in {
//    //Send it to our fhir repo if they are also validated
//    assume(fhirServerIsAvailable)
//    fhirMappingJobManager
//      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(encounterMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
//      .map(unit =>
//        unit shouldBe()
//      )
//  }

  "organization mapping" should "map test data" in {
    //Some semantic tests on generated content
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(organizationMappingTask), job = testJob), mappingJobSourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 2
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    //Send it to our fhir repo if they are also validated
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(organizationMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "hospital unit mapping" should "map test data" in {
    //Some semantic tests on generated content
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(hospitalUnitMappingTask), job = testJob), mappingJobSourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 2
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    //Send it to our fhir repo if they are also validated
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(hospitalUnitMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "diagnostic studies mapping" should "map test data" in {
    //Some semantic tests on generated content
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(diagnosticStudiesMappingTask), job = testJob), mappingJobSourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 2
    }
  }


  it should "map test data and write it to FHIR repo successfully" in {
    //Send it to our fhir repo if they are also validated
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(diagnosticStudiesMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

//  "procedure mapping" should "map test data" in {
//    //Some semantic tests on generated content
//    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(procedureMappingTask), job = testJob), sourceSettings = dataSourceSettings) map { mappingResults =>
//      val results = mappingResults.map(r => {
//        r.mappedResource shouldBe defined
//        val resource = r.mappedResource.get.parseJson
//        resource shouldBe a[Resource]
//        resource
//      })
//      results.length shouldBe 5
//    }
//  }
//
//
//  it should "map test data and write it to FHIR repo successfully" in {
//    //Send it to our fhir repo if they are also validated
//    assume(fhirServerIsAvailable)
//    fhirMappingJobManager
//      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(procedureMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
//      .map(unit =>
//        unit shouldBe()
//      )
//  }

}
