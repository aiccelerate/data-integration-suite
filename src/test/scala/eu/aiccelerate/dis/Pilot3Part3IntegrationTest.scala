package eu.aiccelerate.dis

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import io.onfhir.api.Resource
import io.onfhir.client.OnFhirNetworkClient
import io.onfhir.util.JsonFormatter._
import io.tofhir.engine.mapping.{FhirMappingFolderRepository, FhirMappingJobManager, IFhirMappingRepository, IFhirSchemaLoader, IMappingContextLoader, MappingContextLoader, SchemaFolderLoader}
import io.tofhir.engine.model.{FhirMappingJobExecution, FhirMappingTask, FhirRepositorySinkSettings, FileSystemSource, FileSystemSourceSettings}
import io.tofhir.engine.util.FhirMappingUtility
import org.json4s.JArray

import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

class Pilot3Part3IntegrationTest extends PilotTestSpec {

  val mappingRepository: IFhirMappingRepository =
    new FhirMappingFolderRepository(Paths.get("mappings/pilot3-p3").toAbsolutePath.toUri)

  val contextLoader: IMappingContextLoader = new MappingContextLoader(mappingRepository)

  val schemaLoader: IFhirSchemaLoader = new SchemaFolderLoader(Paths.get("schemas/pilot3-p3").toAbsolutePath.toUri)

  val dataSourceSettings = Map("source" -> FileSystemSourceSettings("test-source-1", "http://hsjd.es", Paths.get("test-data/pilot3-p3").toAbsolutePath.toString))

  val fhirMappingJobManager = new FhirMappingJobManager(mappingRepository, contextLoader, schemaLoader, sparkSession, mappingErrorHandling)

  val fhirSinkSetting: FhirRepositorySinkSettings = FhirRepositorySinkSettings(fhirRepoUrl = sys.env.getOrElse("FHIR_REPO_URL", "http://localhost:8080/fhir"), errorHandling = Some(fhirWriteErrorHandling))
  implicit val actorSystem: ActorSystem = ActorSystem("Pilot3Part3IntegrationTest")
  val onFhirClient: OnFhirNetworkClient = OnFhirNetworkClient.apply(fhirSinkSetting.fhirRepoUrl)

  val fhirServerIsAvailable: Boolean =
    Try(Await.result(onFhirClient.search("Patient").execute(), FiniteDuration(5, TimeUnit.SECONDS)).httpStatus == StatusCodes.OK)
      .getOrElse(false)

  val patientMappingTask: FhirMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot3-p3/patient-mapping",
    sourceContext = Map("source" -> FileSystemSource(path = "patients.csv")))

  val encounterMappingTask: FhirMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot3-p3/encounter-mapping",
    sourceContext = Map("source" -> FileSystemSource(path = "encounters.csv")))

  val conditionMappingTask: FhirMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot3-p3/condition-mapping",
    sourceContext = Map("source" -> FileSystemSource(path = "conditions.csv")))

  val procedureMappingTask: FhirMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot3-p3/procedure-mapping",
    sourceContext = Map("source" -> FileSystemSource(path = "procedures.csv")))

  val medUsedMappingTask: FhirMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot3-p3/medication-used-mapping",
    sourceContext = Map("source" -> FileSystemSource(path = "medications-used.csv")))

  val assessmentObsMappingTask: FhirMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot3-p3/assessment-observation-mapping",
    sourceContext = Map("source" -> FileSystemSource(path = "assessment-observations.csv")))

  "patient mapping" should "map test data" in {
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(patientMappingTask)), sourceSettings = dataSourceSettings) map { mappingResults =>
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
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(patientMappingTask)), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "encounter mapping" should "map test data" in {
    //Some semantic tests on generated content
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(encounterMappingTask)), sourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 11
      (results.head \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p1")
      (results.apply(1) \ "id").extract[String] shouldBe FhirMappingUtility.getHashedId("Encounter", "e1" + "p1" + "305354007" + "2012-08-24")
      (results.apply(1) \ "class" \ "code").extract[String] shouldBe "IMP"
      (results.apply(1) \ "class" \ "display").extract[String] shouldBe "Inpatient encounter"
      (results.head \ "type" \ "coding" \ "code").extract[Seq[String]].head shouldBe "183452005"
      (results.head \ "type" \ "coding" \ "display").extract[Seq[String]].head shouldBe "ER Visit (Emergency hospital admission)"
      (results.apply(4) \ "period" \ "start").extract[String] shouldBe "2011-05-25"
      (results.apply(4) \ "period" \ "end").extract[String] shouldBe "2011-08-26"
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    //Send it to our fhir repo if they are also validated
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(encounterMappingTask)), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "condition mapping" should "map test data" in {
    //Some semantic tests on generated content
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(conditionMappingTask)), sourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 7
      (results.head \ "clinicalStatus" \ "coding" \ "code").extract[Seq[String]].head shouldBe "active"
      (results.apply(1) \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p1")
      (results.apply(3) \ "code" \ "coding" \ "code").extract[Seq[String]].head shouldBe "Q76.4"
      (results.apply(3) \ "code" \ "coding" \ "display").extract[Seq[String]].head shouldBe "Other congenital malformations of spine, not associated with scoliosis"
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    //Send it to our fhir repo if they are also validated
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(conditionMappingTask)), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "procedure mapping" should "map test data" in {
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(procedureMappingTask)), sourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 5
      (results.head \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p1")
      (results.head \ "category" \ "coding" \ "display").extract[Seq[String]].head shouldBe "Procedure categorized by device involved"
      (results.head \ "code" \ "coding" \ "code").extract[Seq[String]].head shouldBe "5A19054"
      (results.last \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p4")
      (results.last \ "performedDateTime").extract[String] shouldBe "2021-04-23"
      (results.last \ "category" \ "coding" \ "code").extract[Seq[String]].head shouldBe "387713003"
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    //Send it to our fhir repo if they are also validated
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(procedureMappingTask)), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "medication used mapping" should "map test data" in {
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(medUsedMappingTask)), sourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 5
      (results.apply(1) \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p2")
      (results.apply(1) \ "medicationCodeableConcept" \ "coding" \ "display").extract[Seq[String]].head shouldBe "cefuroxime"
      (results.apply(2) \ "effectivePeriod" \ "start").extract[String] shouldBe "2015-02-11"
      (results.apply(2) \ "effectivePeriod" \ "end").extract[String] shouldBe "2015-03-11"
      (results.last \ "status").extract[String] shouldBe "active"
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    //Send it to our fhir repo if they are also validated
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(medUsedMappingTask)), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "assessment observation mapping" should "map test data" in {
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(assessmentObsMappingTask)), sourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 6
      (results.head \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p1")
      (results.head \ "effectiveDateTime").extract[String] shouldBe "2020-02-03"
      (results.head \ "code" \ "coding" \ "code").extract[Seq[String]].head shouldBe "707621005"
      (results.head \ "code" \ "coding" \ "system").extract[Seq[String]].head shouldBe "http://snomed.info/sct"
      (results.head \ "valueQuantity" \ "value").extract[Int] shouldBe 3
      (results.head \ "valueQuantity" \ "unit").extract[String] shouldBe "{score}"

      (results.apply(2) \ "code" \ "coding" \ "code").extract[Seq[String]].head shouldBe "zarit"
      (results.apply(2) \ "code" \ "coding" \ "system").extract[Seq[String]].head shouldBe "https://aiccelerate.eu/fhir/CodeSystem/pediatric-assessments"
      (results.apply(2) \ "valueQuantity" \ "value").extract[Int] shouldBe 85

      (results.apply(4) \ "code" \ "coding" \ "code").extract[Seq[String]].head shouldBe "8310-5"
      (results.apply(4) \ "code" \ "coding" \ "system").extract[Seq[String]].head shouldBe "http://loinc.org"
      (results.apply(4) \ "valueQuantity" \ "value").extract[Double] shouldBe 37.2
      (results.apply(4) \ "valueQuantity" \ "unit").extract[String] shouldBe "Cel"
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(assessmentObsMappingTask)), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

}
