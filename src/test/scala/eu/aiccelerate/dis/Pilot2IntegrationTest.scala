package eu.aiccelerate.dis

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import io.onfhir.api.Resource
import io.onfhir.client.OnFhirNetworkClient
import io.onfhir.path.FhirPathUtilFunctionsFactory
import io.tofhir.engine.mapping.{FhirMappingFolderRepository, FhirMappingJobManager, IFhirMappingRepository, IFhirSchemaLoader, IMappingContextLoader, MappingContextLoader, SchemaFolderLoader}
import io.tofhir.engine.model.{ArchiveModes, DataProcessingSettings, FhirMappingJob, FhirMappingJobExecution, FhirMappingTask, FhirRepositorySinkSettings, FileSystemSource, FileSystemSourceSettings}
import io.tofhir.engine.util.FhirMappingUtility
import io.onfhir.util.JsonFormatter._
import io.tofhir.engine.execution.RunningJobRegistry
import org.json4s.JsonAST.JArray

import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

class Pilot2IntegrationTest extends PilotTestSpec {

  val mappingRepository: IFhirMappingRepository =
    new FhirMappingFolderRepository(Paths.get("mappings/pilot2").toAbsolutePath.toUri)

  val contextLoader: IMappingContextLoader = new MappingContextLoader(mappingRepository)

  val schemaLoader: IFhirSchemaLoader = new SchemaFolderLoader(Paths.get("schemas/pilot2").toAbsolutePath.toUri)

  val dataSourceSettings = Map("source" -> FileSystemSourceSettings("test-source-1", "http://hus.fi", Paths.get("test-data/pilot2").toAbsolutePath.toString))

  val mappingUrl = "mocked_mapping_url"
  val jobId = "mocked_job_id"
  val sourceFolderPath = "test-archiver-batch"
  val inputFilePath = "test-input-file"
  val testSourceSettings: FileSystemSourceSettings = FileSystemSourceSettings(name = "test", sourceUri = "test", dataFolderPath = sourceFolderPath)
  val testFileSystemSource: FileSystemSource = FileSystemSource(path = inputFilePath)
  val testMappingTask: FhirMappingTask = FhirMappingTask(sourceContext = Map("_" -> testFileSystemSource), mappingRef = "test")
  val testSinkSettings: FhirRepositorySinkSettings = FhirRepositorySinkSettings(fhirRepoUrl = "test")
  val testDataProcessingSettings: DataProcessingSettings = DataProcessingSettings(archiveMode = ArchiveModes.ARCHIVE)
  val testJob: FhirMappingJob = FhirMappingJob(id = jobId, dataProcessingSettings = testDataProcessingSettings,
    sourceSettings = Map(("_") -> testSourceSettings), sinkSettings = testSinkSettings, mappings = Seq.empty)

  val runningJobRegistry: RunningJobRegistry = new RunningJobRegistry(sparkSession)

  val fhirMappingJobManager = new FhirMappingJobManager(mappingRepository, contextLoader, schemaLoader, Map(FhirPathUtilFunctionsFactory.defaultPrefix -> FhirPathUtilFunctionsFactory), sparkSession, runningJobRegistry)

  val fhirSinkSetting: FhirRepositorySinkSettings = FhirRepositorySinkSettings(fhirRepoUrl = sys.env.getOrElse("FHIR_REPO_URL", "http://localhost:8080/fhir"))
  implicit val actorSystem = ActorSystem("Pilot2IntegrationTest")
  val onFhirClient = OnFhirNetworkClient.apply(fhirSinkSetting.fhirRepoUrl)

  val fhirServerIsAvailable =
    Try(Await.result(onFhirClient.search("Patient").execute(), FiniteDuration(5, TimeUnit.SECONDS)).httpStatus == StatusCodes.OK)
      .getOrElse(false)

  val patientMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot2/patient-mapping",
    sourceContext = Map("source" -> FileSystemSource(path = "patients.csv"))
  )

  val encounterMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot2/encounter-mapping",
    sourceContext = Map("source" -> FileSystemSource(path = "encounters.csv"))
  )

  val symptomsAssMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot2/symptom-assessment-mapping",
    sourceContext = Map("source" -> FileSystemSource(path = "parkinson-symptom-assessments.csv"))
  )

  val symptomsExistenceMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot2/symptom-existence-mapping",
    sourceContext = Map("source" -> FileSystemSource(path = "parkinson-symptom-existence.csv"))
  )

  val otherAssMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot2/other-assessments-mapping",
    sourceContext = Map("source" -> FileSystemSource(path = "other-assessments.csv"))
  )

  val conditionMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot2/condition-mapping",
    sourceContext = Map("source" -> FileSystemSource(path = "conditions.csv"))
  )

  val medicationUsedMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot2/medication-used-mapping",
    sourceContext = Map("source" -> FileSystemSource(path = "medication-used.csv"))
  )

  val deviceUsedMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot2/device-used-mapping",
    sourceContext = Map("source" -> FileSystemSource(path = "device-used.csv"))
  )

  val clinicalNoteMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot2/clinical-note-mapping",
    sourceContext = Map("source" -> FileSystemSource(path = "clinical-notes.csv"))
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
      //10 patients and 4 motorSymptomsOnset
       results.length shouldBe 14

      val patients= results.filter(r => (r \ "meta" \ "profile").extract[Seq[String]].head == "https://aiccelerate.eu/fhir/StructureDefinition/AIC-Patient")
      (JArray(patients.toList) \ "id").extract[Seq[String]].toSet shouldBe (1 to 10).map(i => FhirMappingUtility.getHashedId("Patient", "p"+i)).toSet

      (JArray(patients.toList) \ "identifier" \ "value").extract[Seq[String]].toSet shouldBe (1 to 10).map(i => s"p$i").toSet
      (patients.apply(3) \ "gender").extract[String] shouldBe "male"
      (patients.apply(7) \ "gender").extract[String] shouldBe "female"
      (patients.apply(3) \ "birthDate").extract[String] shouldBe "1999-05-06"

      val conditions = results.filter(r => (r \ "meta" \ "profile").extract[Seq[String]].head == "https://aiccelerate.eu/fhir/StructureDefinition/AIC-PatientReportedCondition")
     (JArray(conditions.toList) \ "subject" \"reference").extract[Seq[String]].toSet shouldBe Set("p2", "p3", "p4", "p7").map(p => FhirMappingUtility.getHashedReference("Patient", p))
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

//  "encounter mapping" should "map test data" in {
//    //Some semantic tests on generated content
//    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(encounterMappingTask), job = testJob), sourceSettings = dataSourceSettings) map { mappingResults =>
//      val results = mappingResults.map(r => {
//        r.mappedResource shouldBe defined
//        val resource = r.mappedResource.get.parseJson
//        resource shouldBe a[Resource]
//        resource
//      })
//      results.length shouldBe 3
//      (results.head \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p1")
//      (results.apply(1) \ "id").extract[String] shouldBe FhirMappingUtility.getHashedId("Encounter", "e2")
//      (results.apply(1) \ "class" \ "code").extract[String] shouldBe "EMER"
//      (results.apply(1) \ "class" \ "display").extract[String] shouldBe "Emergency visit"
//      (results.head \ "type" \ "coding" \ "code").extract[Seq[String]].head shouldBe "225398001"
//      (results.head \ "type" \ "coding" \ "display").extract[Seq[String]].head shouldBe "Neurological Assessment"
//      (results.head \ "period" \ "start").extract[String] shouldBe "2012-08-23"
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

  "symptom assessment mapping" should "map test data" in {
    //Some semantic tests on generated content
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(symptomsAssMappingTask), job = testJob), sourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 12
      (results.apply(2) \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p1")
      (results.apply(2) \ "encounter" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Encounter", "e1")
      (results.apply(2) \ "code" \ "coding" \ "code").extract[Seq[String]].head shouldBe "271587009"
      (results.apply(2) \ "code" \ "coding" \ "display").extract[Seq[String]].head shouldBe "Stiffness, rigidity"
      (results.apply(2) \ "meta" \ "profile").extract[Seq[String]].head shouldBe "https://aiccelerate.eu/fhir/StructureDefinition/AIC-ParkinsonStiffnessScore"
      (results.apply(2) \ "method" \ "coding" \ "code").extract[Seq[String]].head shouldBe "updrs"
      (results.apply(2) \ "method" \ "coding" \ "display").extract[Seq[String]].head shouldBe "UPDRS Questionnaire"
      (results.apply(5) \ "effectivePeriod" \ "start").extract[String] shouldBe "2012-02-07"
      (results.apply(5) \ "effectivePeriod" \ "end").extract[String] shouldBe "2012-02-13"
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    //Send it to our fhir repo if they are also validated
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(symptomsAssMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "symptom existence mapping" should "map test data" in {
    //Some semantic tests on generated content
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(symptomsExistenceMappingTask), job = testJob), sourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 6
      (results.head \ "identifier" \ "value").extract[Seq[String]] shouldBe Seq("se1")
      (results.apply(1) \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p1")
      (results.apply(1) \ "encounter" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Encounter", "e1")

      (results.head \ "code" \ "coding" \ "code").extract[Seq[String]].head shouldBe "443544006"
      (results.head \ "code" \ "coding" \ "display").extract[Seq[String]].head shouldBe "Freezing of gait"
      (results.apply(4) \ "valueBoolean").extract[Boolean] shouldBe (true)
      (results.last \ "code" \ "coding" \ "code").extract[Seq[String]].head shouldBe "39898005"
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    //Send it to our fhir repo if they are also validated
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(symptomsExistenceMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "other assessment mapping" should "map test data" in {
    //Some semantic tests on generated content
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(otherAssMappingTask), job = testJob), sourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 5
      (results.head \ "identifier" \ "value").extract[Seq[String]] shouldBe Seq("oo1")
      (results.apply(1) \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p1")
      (results.apply(1) \ "encounter" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Encounter", "e1")
      (results.apply(2) \ "code" \ "coding" \ "code").extract[Seq[String]].head shouldBe "bis-11"
      (results.apply(2) \ "code" \ "coding" \ "display").extract[Seq[String]].head shouldBe "Barratt Impulsiveness Scale-11 score"
      (results.apply(3) \ "code" \ "coding" \ "code").extract[Seq[String]].head shouldBe "tmt-a"
      (results.apply(3) \ "valueQuantity" \ "code").extract[String] shouldBe "{Zscore}"
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    //Send it to our fhir repo if they are also validated
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(otherAssMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "condition mapping" should "map test data" in {
    //Some semantic tests on generated content
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(conditionMappingTask), job = testJob), sourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 10

      (results.apply(1) \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p2")
      (results.apply(1) \ "encounter" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Encounter", "e2")
      (results.apply(2) \ "code" \ "coding" \ "code").extract[Seq[String]].head shouldBe "K76.8"
//      (results.apply(2) \ "code" \ "coding" \ "display").extract[Seq[String]].head shouldBe "Other specified diseases of liver"

      (results.head \ "onsetDateTime").extract[String] shouldBe "2010-10-15"
      (results.head \ "abatementDateTime").extract[String] shouldBe "2010-11-15"
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    //Send it to our fhir repo if they are also validated
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(conditionMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

//  "medication used mapping" should "map test data" in {
//    //Some semantic tests on generated content
//    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(medicationUsedMappingTask), job = testJob), sourceSettings = dataSourceSettings) map { mappingResults =>
//      val results = mappingResults.map(r => {
//        r.mappedResource shouldBe defined
//        val resource = r.mappedResource.get.parseJson
//        resource shouldBe a[Resource]
//        resource
//      })
//      results.length shouldBe 16
//      (results.apply(1) \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p1")
//      (results.apply(2) \ "medicationCodeableConcept" \ "coding" \ "code").extract[Seq[String]].head shouldBe "N06AB03"
//      (results.apply(2) \ "medicationCodeableConcept" \ "coding" \ "display").extract[Seq[String]].head shouldBe "fluoxetine"
//      (results.apply(14) \ "dosage" \ "timing" \ "repeat" \ "frequency").extract[Seq[Int]].head shouldBe 2
//      (results.apply(14) \ "dosage" \ "doseAndRate" \ "doseRange" \ "low" \ "value").extract[Seq[Int]].head shouldBe 600 //(30 * 20)
//      (results.last \ "dosage" \ "doseAndRate" \ "doseRange" \ "high" \ "value").extract[Seq[Float]].head shouldBe 250.0 //(0.5 * 500)
//    }
//  }
//
//  it should "map test data and write it to FHIR repo successfully" in {
//    //Send it to our fhir repo if they are also validated
//    assume(fhirServerIsAvailable)
//    fhirMappingJobManager
//      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(medicationUsedMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
//      .map(unit =>
//        unit shouldBe()
//      )
//  }

  "device used mapping" should "map test data" in {
    //Some semantic tests on generated content
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(deviceUsedMappingTask), job = testJob), sourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 2
      (results.last \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p2")
      (results.last \ "device" \ "identifier" \ "value").extract[String] shouldBe "levodopa-infusion-pump"
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    //Send it to our fhir repo if they are also validated
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(deviceUsedMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "clinical note mapping" should "map test data" in {
    //Some semantic tests on generated content
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(clinicalNoteMappingTask), job = testJob), sourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 1
      (results.last \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p1")
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    //Send it to our fhir repo if they are also validated
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(clinicalNoteMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }


}
