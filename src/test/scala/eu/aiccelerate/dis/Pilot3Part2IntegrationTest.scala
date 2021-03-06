package eu.aiccelerate.dis

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import io.onfhir.client.OnFhirNetworkClient
import io.onfhir.tofhir.config.MappingErrorHandling
import io.onfhir.tofhir.engine._
import io.onfhir.tofhir.model._
import io.onfhir.tofhir.util.FhirMappingUtility
import io.onfhir.util.JsonFormatter.formats
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

  val schemaRepository = new SchemaFolderRepository(Paths.get("schemas/pilot3-p2").toAbsolutePath.toUri)

  val dataSourceSettings = Map("source" -> FileSystemSourceSettings("test-source-1", "http://hus.fi", Paths.get("test-data/pilot3-p2").toAbsolutePath.toString))

  val fhirMappingJobManager = new FhirMappingJobManager(mappingRepository, contextLoader, schemaRepository, sparkSession, mappingErrorHandling)

  val fhirSinkSetting: FhirRepositorySinkSettings = FhirRepositorySinkSettings(fhirRepoUrl = "http://localhost:8081/fhir", writeErrorHandling = MappingErrorHandling.CONTINUE)
  implicit val actorSystem = ActorSystem("Pilot3Part2IntegrationTest")
  val onFhirClient = OnFhirNetworkClient.apply(fhirSinkSetting.fhirRepoUrl)

  val fhirServerIsAvailable =
    Try(Await.result(onFhirClient.search("Patient").execute(), FiniteDuration(5, TimeUnit.SECONDS)).httpStatus == StatusCodes.OK)
      .getOrElse(false)

  val patientMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot3-p2/patient-mapping",
    sourceContext = Map("source" -> FileSystemSource(path = "patients.csv"))
  )

  val conditionMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot3-p2/condition-mapping",
    sourceContext = Map("source" -> FileSystemSource(path = "conditions.csv"))
  )

  val labResultsMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot3-p2/lab-results-mapping",
    sourceContext = Map("source" -> FileSystemSource(path = "lab-results.csv"))
  )

  val symptomMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot3-p2/symptom-observation-mapping",
    sourceContext = Map("source" -> FileSystemSource(path = "symptoms.csv"))
  )

  val vitalSignsMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot3-p2/vital-signs-mapping",
    sourceContext = Map("source" -> FileSystemSource(path = "vitalsigns.csv"))
  )

  val neuroObsMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot3-p2/neurooncological-observation-mapping",
    sourceContext = Map("source" -> FileSystemSource(path = "neurooncological-observations.csv"))
  )

  val medAdministrationMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot3-p2/medication-administration-mapping",
    sourceContext = Map("source" -> FileSystemSource(path = "medication-administrations.csv"))
  )

  val medUsedMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot3-p2/medication-used-mapping",
    sourceContext = Map("source" -> FileSystemSource(path = "medications-used.csv"))
  )

  "patient mapping" should "map test data" in {
    //Some semantic tests on generated content
    fhirMappingJobManager.executeMappingTaskAndReturn(task = patientMappingTask, sourceSettings = dataSourceSettings) map { results =>
      results.length shouldBe 10
      (JArray(results.toList) \ "meta" \ "profile").extract[Seq[Seq[String]]].flatten.toSet shouldBe Set("https://aiccelerate.eu/fhir/StructureDefinition/AIC-Patient")
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    //Send it to our fhir repo if they are also validated
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(tasks = Seq(patientMappingTask), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "condition mapping" should "map test data" in {
    //Some semantic tests on generated content
    fhirMappingJobManager.executeMappingTaskAndReturn(task = conditionMappingTask, sourceSettings = dataSourceSettings) map { results =>
      results.length shouldBe 5

      (results.apply(1) \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p2")

      (results.apply(3) \ "code" \ "coding" \ "code").extract[Seq[String]].head shouldBe "M89.9"
      (results.apply(3) \ "code" \ "coding" \ "display").extract[Seq[String]].head shouldBe "Disorder of bone, unspecified"

      (results.head \ "clinicalStatus" \ "coding" \ "code").extract[Seq[String]].head shouldBe "resolved"
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    //Send it to our fhir repo if they are also validated
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(tasks = Seq(conditionMappingTask), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "lab results mapping" should "map test data" in {
    //Some semantic tests on generated content
    fhirMappingJobManager.executeMappingTaskAndReturn(task = labResultsMappingTask, sourceSettings = dataSourceSettings) map { results =>
      results.length shouldBe 33
      (results.apply(25) \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p21")
      (results.apply(25) \ "code" \ "coding" \ "code").extract[Seq[String]].head shouldBe "2141-0"
      (results.apply(25) \ "code" \ "coding" \ "display").extract[Seq[String]].head shouldBe "Corticotropin [Mass/volume] in Plasma (P-ACTH)"
      (results.apply(25) \ "valueQuantity" \ "value").extract[Double] shouldBe 18.16060583
      (results.apply(25) \ "valueQuantity" \ "unit").extract[String] shouldBe "pg/mL"

      (results.apply(24) \ "valueQuantity" \ "value").extract[Double] shouldBe 123.06613514285
      (results.apply(24) \ "valueQuantity" \ "unit").extract[String] shouldBe "ng/mL"
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    //Send it to our fhir repo if they are also validated
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(tasks = Seq(labResultsMappingTask), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "symptoms mapping" should "map test data" in {
    //Some semantic tests on generated content
    fhirMappingJobManager.executeMappingTaskAndReturn(task = symptomMappingTask, sourceSettings = dataSourceSettings) map { results =>
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
      .executeMappingJob(tasks = Seq(symptomMappingTask), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "vital signs mapping" should "map test data" in {
    //Some semantic tests on generated content
    fhirMappingJobManager.executeMappingTaskAndReturn(task = vitalSignsMappingTask, sourceSettings = dataSourceSettings) map { results =>
      results.length shouldBe 8
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    //Send it to our fhir repo if they are also validated
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(tasks = Seq(vitalSignsMappingTask), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "neurooncological observations mapping" should "map test data" in {
    //Some semantic tests on generated content
    fhirMappingJobManager.executeMappingTaskAndReturn(task = neuroObsMappingTask, sourceSettings = dataSourceSettings) map { results =>
      results.length shouldBe 21
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
      .executeMappingJob(tasks = Seq(neuroObsMappingTask), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "medication administration mapping" should "map test data" in {
    //Some semantic tests on generated content
    fhirMappingJobManager.executeMappingTaskAndReturn(task = medAdministrationMappingTask, sourceSettings = dataSourceSettings) map { results =>
      results.length shouldBe 12

      (results.apply(4) \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p3")
      (results.apply(4) \ "medicationCodeableConcept" \ "coding" \ "display").extract[Seq[String]].head shouldBe "etoposide"
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    //Send it to our fhir repo if they are also validated
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(tasks = Seq(medAdministrationMappingTask), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "medication used mapping" should "map test data" in {
    //Some semantic tests on generated content
    fhirMappingJobManager.executeMappingTaskAndReturn(task = medUsedMappingTask, sourceSettings = dataSourceSettings) map { results =>
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
      .executeMappingJob(tasks = Seq(medUsedMappingTask), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }


}
