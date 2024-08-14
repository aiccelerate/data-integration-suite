package eu.aiccelerate.dis

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import io.onfhir.api.Resource
import io.onfhir.client.OnFhirNetworkClient
import io.onfhir.path.FhirPathUtilFunctionsFactory
import io.onfhir.util.JsonFormatter._
import io.tofhir.engine.execution.RunningJobRegistry
import io.tofhir.engine.mapping.{FhirMappingFolderRepository, FhirMappingJobManager, IFhirMappingRepository, IFhirSchemaLoader, IMappingContextLoader, MappingContextLoader, SchemaFolderLoader}
import io.tofhir.engine.model.{ArchiveModes, DataProcessingSettings, FhirMappingJob, FhirMappingJobExecution, FhirMappingTask, FhirRepositorySinkSettings, FileSystemSource, FileSystemSourceSettings}
import io.tofhir.engine.util.FhirMappingUtility
import org.json4s.JsonAST.JArray

import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

class Pilot3Part2IntegrationTest extends PilotTestSpec {

  val mappingRepository: IFhirMappingRepository =
    new FhirMappingFolderRepository(Paths.get("mappings/pilot3-p2").toAbsolutePath.toUri)

  val contextLoader: IMappingContextLoader = new MappingContextLoader(mappingRepository)

  val schemaLoader: IFhirSchemaLoader = new SchemaFolderLoader(Paths.get("schemas/pilot3-p2").toAbsolutePath.toUri)

  val dataSourceSettings = Map("source" -> FileSystemSourceSettings("test-source-1", "http://hus.fi", Paths.get("test-data/pilot3-p2").toAbsolutePath.toString))

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

  val runningJobRegistry: RunningJobRegistry = new RunningJobRegistry(sparkSession)

  val fhirMappingJobManager = new FhirMappingJobManager(mappingRepository, contextLoader, schemaLoader, Map(FhirPathUtilFunctionsFactory.defaultPrefix -> FhirPathUtilFunctionsFactory), sparkSession, runningJobRegistry)

  val fhirSinkSetting: FhirRepositorySinkSettings = FhirRepositorySinkSettings(fhirRepoUrl = sys.env.getOrElse("FHIR_REPO_URL", "http://localhost:8080/fhir"))
  implicit val actorSystem = ActorSystem("Pilot3Part2IntegrationTest")
  val onFhirClient = OnFhirNetworkClient.apply(fhirSinkSetting.fhirRepoUrl)

  val fhirServerIsAvailable =
    Try(Await.result(onFhirClient.search("Patient").execute(), FiniteDuration(5, TimeUnit.SECONDS)).httpStatus == StatusCodes.OK)
      .getOrElse(false)

  val patientMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot3-p2/patient-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "patients.csv"))
  )

  val conditionMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot3-p2/condition-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "conditions.csv"))
  )

  val labResultsMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot3-p2/lab-results-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "lab-results.csv"))
  )

  val symptomMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot3-p2/symptom-observation-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "symptoms.csv"))
  )

  val vitalSignsMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot3-p2/vital-signs-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "vitalsigns.csv"))
  )

  val neuroObsMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot3-p2/neurooncological-observation-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "neurooncological-observations.csv"))
  )

  val medAdministrationMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot3-p2/medication-administration-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "medication-administrations.csv"))
  )

  val medUsedMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot3-p2/medication-used-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "medications-used.csv"))
  )

  "patient mapping" should "map test data" in {
    //Some semantic tests on generated content
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(patientMappingTask), job = testJob), sourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 10
      (JArray(results.toList) \ "meta" \ "profile").extract[Seq[Seq[String]]].flatten.toSet shouldBe Set("https://aiccelerate.eu/fhir/StructureDefinition/AIC-Patient")
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    //Send it to our fhir repo if they are also validated
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(patientMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

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

  "lab results mapping" should "map test data" in {
    //Some semantic tests on generated content
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(labResultsMappingTask), job = testJob), sourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 33
      (results.apply(25) \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p21")
      (results.apply(25) \ "code" \ "coding" \ "code").extract[Seq[String]].head shouldBe "2141-0"
      (results.apply(25) \ "code" \ "coding" \ "display").extract[Seq[String]].head shouldBe "Corticotropin [Mass/volume] in Plasma (P-ACTH)"
      (results.apply(25) \ "valueQuantity" \ "value").extract[Double] shouldBe 18.16060583
      (results.apply(25) \ "valueQuantity" \ "code").extract[String] shouldBe "ng/L"

      (results.apply(24) \ "valueQuantity" \ "value").extract[Double] shouldBe 16.08917965
      (results.apply(24) \ "valueQuantity" \ "code").extract[String] shouldBe "nmol/L"
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    //Send it to our fhir repo if they are also validated
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(labResultsMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "symptoms mapping" should "map test data" in {
    //Some semantic tests on generated content
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(symptomMappingTask), job = testJob), sourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 12

      val patient1 = results.filter(r => (r \ "subject" \ "reference").extract[String] == FhirMappingUtility.getHashedReference("Patient", "p1"))
      patient1.length shouldBe 7
      (JArray(patient1.toList) \ "valueBoolean").extract[Seq[Boolean]] shouldBe (1 to 7).map(_ => false)
      (patient1.apply(4) \ "code" \ "coding" \ "code").extract[Seq[String]].head shouldBe "699281009"
      (patient1.apply(4) \ "code" \ "coding" \ "display").extract[Seq[String]].head shouldBe "Motor Weakness"

      val patient2 = results.filter(r => (r \ "subject" \ "reference").extract[String] == FhirMappingUtility.getHashedReference("Patient", "p2"))
      (JArray(patient2.toList) \ "valueBoolean").extract[Seq[Boolean]] shouldBe (8 to 12).map(_ => true)
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    //Send it to our fhir repo if they are also validated
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(symptomMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "vital signs mapping" should "map test data" in {
    //Some semantic tests on generated content
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(vitalSignsMappingTask), job = testJob), sourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 8
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    //Send it to our fhir repo if they are also validated
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(vitalSignsMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "neurooncological observations mapping" should "map test data" in {
    //Some semantic tests on generated content
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(neuroObsMappingTask), job = testJob), sourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 22
      (results.apply(9) \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p2")
      (results.apply(9) \ "code" \ "coding" \ "code").extract[Seq[String]].head shouldBe "18156-0"
      (results.apply(9) \ "code" \ "coding" \ "display").extract[Seq[String]].head shouldBe "Posterior wall thickness (Left ventricular posterior wall Thickness during systole by US)"
      (results.apply(9) \ "valueQuantity" \ "value").extract[Double] shouldBe 9.035436143
      (results.apply(9) \ "valueQuantity" \ "code").extract[String] shouldBe "mm"


      (results.apply(17) \ "valueCodeableConcept" \ "coding" \ "code").extract[Seq[String]].head shouldBe "LA28366-5"
      (results.apply(17) \ "valueCodeableConcept" \ "coding" \ "display").extract[Seq[String]].head shouldBe "Complete response"
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    //Send it to our fhir repo if they are also validated
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(neuroObsMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "medication administration mapping" should "map test data" in {
    //Some semantic tests on generated content
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(medAdministrationMappingTask), job = testJob), sourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 12

      (results.apply(4) \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p3")
      (results.apply(4) \ "medicationCodeableConcept" \ "coding" \ "display").extract[Seq[String]].head shouldBe "etoposide"
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    //Send it to our fhir repo if they are also validated
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(medAdministrationMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "medication used mapping" should "map test data" in {
    //Some semantic tests on generated content
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(medUsedMappingTask), job = testJob), sourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 10
      (results.apply(4) \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p5")
      (results.apply(4) \ "medicationCodeableConcept" \ "coding" \ "display").extract[Seq[String]].head shouldBe "vancomycin"
      (results.apply(4) \ "effectivePeriod" \ "start").extract[String] shouldBe "2014-05-10"
      (results.apply(4) \ "effectivePeriod" \ "end").extract[String] shouldBe "2014-06-10"

      (results.last \ "status").extract[String] shouldBe "active"
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    //Send it to our fhir repo if they are also validated
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(medUsedMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }


}
