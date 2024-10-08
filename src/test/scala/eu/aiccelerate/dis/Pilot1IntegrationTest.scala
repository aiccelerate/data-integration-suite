package eu.aiccelerate.dis

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import io.onfhir.api.Resource
import io.onfhir.client.OnFhirNetworkClient
import io.onfhir.path.FhirPathUtilFunctionsFactory
import io.tofhir.engine.model.{ArchiveModes, DataProcessingSettings, FhirMappingJob, FhirMappingJobExecution, FhirMappingTask, FhirRepositorySinkSettings, FileSystemSource, FileSystemSourceSettings}
import io.tofhir.engine.util.FhirMappingUtility
import io.onfhir.util.JsonFormatter._
import io.tofhir.engine.mapping.context.{IMappingContextLoader, MappingContextLoader}
import io.tofhir.engine.mapping.job.FhirMappingJobManager
import io.tofhir.engine.mapping.schema.{IFhirSchemaLoader, SchemaFolderLoader}
import io.tofhir.engine.repository.mapping.{FhirMappingFolderRepository, IFhirMappingRepository}
import org.json4s.JArray
import org.json4s.JsonAST.JObject

import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

class Pilot1IntegrationTest extends PilotTestSpec {

  val mappingRepository: IFhirMappingRepository =
    new FhirMappingFolderRepository(Paths.get("mappings/pilot1").toAbsolutePath.toUri)

  val contextLoader: IMappingContextLoader = new MappingContextLoader

  val schemaLoader: IFhirSchemaLoader = new SchemaFolderLoader(Paths.get("schemas/pilot1").toAbsolutePath.toUri)

  val dataSourceSettings = Map("source" -> FileSystemSourceSettings("test-source-1", "http://hus.fi", Paths.get("test-data/pilot1").toAbsolutePath.toString))

  val mappingUrl = "mocked_mapping_url"
  val jobId = "mocked_job_id"
  val sourceFolderPath = "test-archiver-batch"
  val inputFilePath = "test-input-file"
  val testSourceSettings: FileSystemSourceSettings = FileSystemSourceSettings(name = "test", sourceUri = "test", dataFolderPath = sourceFolderPath)
  val testFileSystemSource: FileSystemSource = FileSystemSource(path = inputFilePath, contentType = "csv")
  val testMappingTask: FhirMappingTask = FhirMappingTask(sourceBinding = Map("_" -> testFileSystemSource), mappingRef = "test", name = "test")
  val testSinkSettings: FhirRepositorySinkSettings = FhirRepositorySinkSettings(fhirRepoUrl = "test")
  val testDataProcessingSettings: DataProcessingSettings = DataProcessingSettings(archiveMode = ArchiveModes.ARCHIVE)
  val testJob: FhirMappingJob = FhirMappingJob(id = jobId, dataProcessingSettings = testDataProcessingSettings,
    sourceSettings = Map(("_") -> testSourceSettings), sinkSettings = testSinkSettings, mappings = Seq.empty)

  val fhirMappingJobManager = new FhirMappingJobManager(mappingRepository, contextLoader, schemaLoader, Map(FhirPathUtilFunctionsFactory.defaultPrefix -> FhirPathUtilFunctionsFactory), sparkSession)

  val fhirSinkSetting: FhirRepositorySinkSettings = FhirRepositorySinkSettings(fhirRepoUrl = sys.env.getOrElse("FHIR_REPO_URL", "http://localhost:8080/fhir"))
  implicit val actorSystem = ActorSystem("Pilot1IntegrationTest")

  val onFhirClient = OnFhirNetworkClient.apply(fhirSinkSetting.fhirRepoUrl)

  val fhirServerIsAvailable =
    Try(Await.result(onFhirClient.search("Patient").execute(), FiniteDuration(5, TimeUnit.SECONDS)).httpStatus == StatusCodes.OK)
      .getOrElse(false)

  val patientMappingTask = FhirMappingTask(
    name = "patient-mapping",
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot1/patient-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "patients.csv", contentType = "csv"))
  )
  val encounterMappingTask =
    FhirMappingTask(
      name = "operation-episode-encounter-mapping",
      mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot1/operation-episode-encounter-mapping",
      sourceBinding = Map("source" -> FileSystemSource(path = "operation-episode-encounters.csv", contentType = "csv"))
    )
  val surgeryPlanMappingTask = FhirMappingTask(
    name = "surgery-plan-mapping",
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot1/surgery-plan-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "surgery-plans.csv", contentType = "csv"))
  )

  val surgeryDetailsMappingTask = FhirMappingTask(
    name = "surgery-details-mapping",
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot1/surgery-details-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "surgery-details.csv", contentType = "csv"))
  )

  val preopAssMappingTask = FhirMappingTask(
    name = "preoperative-assessment-mapping",
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot1/preoperative-assessment-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "preoperative-assessment.csv", contentType = "csv"))
  )

  val healthBehaviorMappingTask = FhirMappingTask(
    name = "health-behavior-assessment-mapping",
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot1/health-behavior-assessment-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "health-behavior-assessment.csv", contentType = "csv"
    ))
  )

  val otherObsMappingTask = FhirMappingTask(
    name = "other-observation-mapping",
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot1/other-observation-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "other-observations.csv", contentType = "csv"))
  )

  val medUsedMappingTask = FhirMappingTask(
    name = "medication-used-mapping",
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot1/medication-used-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "medication-used.csv", contentType = "csv"
    ))
  )

  val medAdmMappingTask = FhirMappingTask(
    name = "medication-administration-mapping",
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot1/medication-administration-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "medication-administration.csv", contentType = "csv"))
  )

  val conditionMappingTask = FhirMappingTask(
    name = "condition-mapping",
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot1/condition-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "conditions.csv", contentType = "csv"))
  )

  val vitalSignsMappingTask = FhirMappingTask(
    name = "vital-signs-mapping",
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot1/vital-signs-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "vitalsigns.csv", contentType = "csv"))
  )

  val labResultsMappingTask = FhirMappingTask(
    name = "lab-results-mapping",
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot1/lab-results-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "lab-results.csv", contentType = "csv"))
  )

  val radStudiesMappingTask = FhirMappingTask(
    name = "radiological-studies-mapping",
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot1/radiological-studies-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "radiological-studies.csv", contentType = "csv"))
  )

  val hospitalUnitMappingTask = FhirMappingTask(
    name = "hospital-unit-mapping",
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot1/hospital-unit-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "hospital-unit.csv", contentType = "csv"))
  )

  val practitionerMappingTask = FhirMappingTask(
    name = "practitioner-mapping",
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot1/practitioner-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "practitioners.csv", contentType = "csv"))
  )

  val workshiftMappingTask = FhirMappingTask(
    name = "workshift-mapping",
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot1/workshift-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "workshift.csv", contentType = "csv"))
  )

  val backgroundInfMappingTask = FhirMappingTask(
    name = "background-information-mapping",
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot1/background-information-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "background-information.csv", contentType = "csv"))
  )

  val preopRisksMappingTask = FhirMappingTask(
    name = "preoperative-risks-mapping",
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot1/preoperative-risks-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "preoperative-risk-factors.csv", contentType = "csv"))
  )

  val patientReportedConditionsMappingTask = FhirMappingTask(
    name = "patient-reported-conditions-mapping",
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot1/patient-reported-conditions-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "patient-reported-conditions.csv", contentType = "csv"))
  )

  val preopSymptomsMappingTask = FhirMappingTask(
    name = "preoperative-symptoms-mapping",
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot1/preoperative-symptoms-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "preoperative-symptoms.csv", contentType = "csv"))
  )

  val anestObsMappingTask = FhirMappingTask(
    name = "anesthesia-observations-mapping",
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot1/anesthesia-observations-mapping",
    sourceBinding = Map("source" -> FileSystemSource(path = "anesthesia-observations.csv", contentType = "csv"))
  )

  "patient mapping" should "map test data" in {
    //Some semantic tests on generated content
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(patientMappingTask), job = testJob), mappingJobSourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        r.mappedResource.get.parseJson
      })
      val genders = (JArray(results.toList) \ "gender").extract[Seq[String]]
      genders shouldBe ((1 to 5).map(_ => "male") ++ (6 to 10).map(_ => "female"))

      (results.apply(2) \ "id").extract[String] shouldBe FhirMappingUtility.getHashedId("Patient", "p3")
      (results.apply(2) \ "identifier" \ "value").extract[Seq[String]] shouldBe Seq("p3")
      (results.apply(2) \ "birthDate").extract[String] shouldBe ("1997-02")
      (results.apply(3) \ "address" \ "postalCode").extract[Seq[String]] shouldBe (Seq("H10564"))
      (results.apply(4) \ "deceasedDateTime").extract[String] shouldBe ("2019-04-21")
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


  "operation episode encounter mapping" should "map test data" in {
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(encounterMappingTask), job = testJob), mappingJobSourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.size shouldBe (10)
      (results.apply(1) \ "id").extract[String] shouldBe FhirMappingUtility.getHashedId("Encounter", "e2")
      (results.apply(1) \ "identifier" \ "value").extract[Seq[String]] shouldBe Seq("e2")
      (results.apply(1) \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p2")
      (results.apply(3) \ "episodeOfCare" \ "reference").extract[Seq[String]] shouldBe Seq(FhirMappingUtility.getHashedReference("EpisodeOfCare", "ep4"))
      (results.apply(9) \ "status").extract[String] shouldBe "cancelled"
      (results.apply(5) \ "type" \ "coding" \ "code").extract[Seq[String]] shouldBe Seq("305354007")
      (results.apply(5) \ "type" \ "coding" \ "display").extract[Seq[String]] shouldBe Seq("Ward stay")
      (results.apply(6) \ "period" \ "start").extract[String] shouldBe "2014-10-20"
      (results.apply(3) \ "period" \ "end").extractOpt[String] shouldBe empty
      (results.apply(7) \ "location" \ "location" \ "reference").extract[Seq[String]] shouldBe Seq(FhirMappingUtility.getHashedReference("Location", "ward2"))
      (results.head \ "participant" \ "individual" \ "reference").extract[Seq[String]] shouldBe Seq(FhirMappingUtility.getHashedReference("PractitionerRole", "pr4"))
    }
  }
  it should "map test data and write it to FHIR repo successfully" in {
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(encounterMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

//  "surgery plan mapping" should "map test data" in {
//    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(surgeryPlanMappingTask), job = testJob) , sourceSettings = dataSourceSettings) map { mappingResults =>
//      val results = mappingResults.map(r => {
//        r.mappedResource shouldBe defined
//        val resource = r.mappedResource.get.parseJson
//        resource shouldBe a[Resource]
//        resource
//      })
//      results.size shouldBe 4
//      //ServiceRequests
//      val sr1 = results.find(r => (r \ "id").extractOpt[String].contains(FhirMappingUtility.getHashedId("ServiceRequest", "sp1")))
//      val sr2 = results.find(r => (r \ "id").extractOpt[String].contains(FhirMappingUtility.getHashedId("ServiceRequest", "sp2")))
//      sr1.isDefined shouldBe true
//      sr2.isDefined shouldBe true
//      //Encounters
//      val e1 = results.find(r => (r \ "episodeOfCare" \ "reference").extractOpt[Seq[String]].contains(Seq(FhirMappingUtility.getHashedReference("EpisodeOfCare", "ep11"))))
//      val e2 = results.find(r => (r \ "episodeOfCare" \ "reference").extractOpt[Seq[String]].contains(Seq(FhirMappingUtility.getHashedReference("EpisodeOfCare", "ep12"))))
//      e1.isDefined shouldBe true
//      e2.isDefined shouldBe true
//
//      (sr1.get \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p1")
//      (sr2.get \ "encounter" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Encounter", "e22")
//      (sr1.get \ "occurrencePeriod" \ "start").extract[String] shouldBe "2007-09-22T10:00:00+01:00"
//      (sr2.get \ "occurrencePeriod" \ "end").extract[String] shouldBe "2007-09-22T16:00:00+01:00"
//      (sr1.get \ "requester" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Practitioner", "pr1")
//      (sr1.get \ "performer" \ "reference").extractOpt[Seq[String]] shouldBe Some(Seq(FhirMappingUtility.getHashedReference("PractitionerRole", "pr1")))
//      (sr2.get \ "code" \ "coding" \ "code").extract[Seq[String]] shouldBe Seq("AAA27")
//
//      (e1.get \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p1")
//      (e2.get \ "type" \ "coding" \ "code").extract[Seq[String]] shouldBe Seq("305351004")
//    }
//
//  }

//  it should "map test data and write it to FHIR repo successfully" in {
//    assume(fhirServerIsAvailable)
//    fhirMappingJobManager
//      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(surgeryPlanMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
//      .map(unit =>
//        unit shouldBe()
//      )
//  }

//  "surgery details mapping" should "map test data" in {
//    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(surgeryDetailsMappingTask), job = testJob), sourceSettings = dataSourceSettings) map { mappingResults =>
//      val results = mappingResults.map(r => {
//        r.mappedResource shouldBe defined
//        val resource = r.mappedResource.get.parseJson
//        resource shouldBe a[Resource]
//        resource
//      })
//      results.size shouldBe 15
//      val surgeryEncounters = results.filter(r => (r \ "meta" \ "profile").extract[Seq[String]].head == "https://aiccelerate.eu/fhir/StructureDefinition/AIC-SurgeryEncounter")
//      surgeryEncounters.size shouldBe 2
//      (surgeryEncounters.head \ "id").extract[String] shouldBe FhirMappingUtility.getHashedId("Encounter", "e11")
//      (surgeryEncounters.head \ "serviceType" \ "coding" \ "code").extract[Seq[String]] shouldBe Seq("394609007")
//      (surgeryEncounters.head \ "serviceType" \ "coding" \ "display").extract[Seq[String]] shouldBe Seq("General surgery")
//      (surgeryEncounters.last \ "episodeOfCare" \ "reference").extract[Seq[String]] shouldBe Seq(FhirMappingUtility.getHashedReference("EpisodeOfCare", "ep2"))
//      (surgeryEncounters.last \ "location" \ "location" \ "reference").extract[Seq[String]] shouldBe Seq(FhirMappingUtility.getHashedReference("Location", "or1"))
//      (surgeryEncounters.head \ "basedOn" \ "reference").extract[Seq[String]] shouldBe Seq(FhirMappingUtility.getHashedReference("ServiceRequest", "sp1"))
//      (surgeryEncounters.last \ "basedOn" \ "reference").extract[Seq[String]] shouldBe empty
//      (surgeryEncounters.head \ "participant" \ "individual" \ "reference").extract[Seq[String]].length shouldBe 5
//      //p1 has all 3 phases
//      val otherPhases = results.filter(r => (r \ "meta" \ "profile").extract[Seq[String]].head == "https://aiccelerate.eu/fhir/StructureDefinition/AIC-OperationPhaseDetails")
//      otherPhases.length shouldBe 3
//      //Enrollment
//      val enrollment = otherPhases.find(p => (p \ "category" \ "coding" \ "code").extract[Seq[String]] == Seq("305408004"))
//      enrollment should not be empty
//      (enrollment.head \ "performedPeriod" \ "start").extract[String] shouldBe "2015-05-17T10:05:00+01:00"
//      //Both patients has anesthesia data
//      val anesthesiaPhases = results.filter(r => (r \ "meta" \ "profile").extract[Seq[String]].head == "https://aiccelerate.eu/fhir/StructureDefinition/AIC-AnesthesiaPhaseDetails")
//      anesthesiaPhases.length shouldBe 2
//      val patient1AnesthesiaProcedure = anesthesiaPhases.find(r => (r \ "subject" \ "reference").extract[String] == FhirMappingUtility.getHashedReference("Patient", "p1"))
//      patient1AnesthesiaProcedure should not be empty
//      (patient1AnesthesiaProcedure.head \ "code" \ "coding" \ "code").extract[Seq[String]].toSet shouldBe Set("50697003", "WX402")
//      (patient1AnesthesiaProcedure.head \ "code" \ "coding" \ "display").extract[Seq[String]].toSet shouldBe Set("General Anesthesia", "General anesthesis")
//
//      val surgeryPhases = results.filter(r => (r \ "meta" \ "profile").extract[Seq[String]].head == "https://aiccelerate.eu/fhir/StructureDefinition/AIC-SurgeryPhaseDetails")
//      surgeryPhases.length shouldBe 2
//      val patient1Surgery = surgeryPhases.find(r => (r \ "subject" \ "reference").extract[String] == FhirMappingUtility.getHashedReference("Patient", "p1"))
//      patient1Surgery should not be empty
//      (patient1Surgery.head \ "code" \ "coding" \ "code").extract[Seq[String]] shouldBe Seq("AAC00")
//      (patient1Surgery.head \ "code" \ "coding" \ "display").extract[Seq[String]] shouldBe Seq("Ligature of intracranial aneurysm")
//      //3 surgeons for p1
//      FhirPathEvaluator().evaluateString("Procedure.performer.where(function.coding.where(code='304292004').exists()).actor.reference", patient1Surgery.head).length shouldBe 3
//
//      val patient2Surgery = surgeryPhases.find(r => (r \ "subject" \ "reference").extract[String] == FhirMappingUtility.getHashedReference("Patient", "p2"))
//      patient2Surgery should not be empty
//      //For patient 2, 2 procedure from other procedures and 1 from intubation
//      val otherProcedures = results.filter(r => (r \ "meta" \ "profile").extract[Seq[String]].head == "https://aiccelerate.eu/fhir/StructureDefinition/AIC-ProcedureRelatedWithSurgicalWorkflow")
//      otherProcedures.length shouldBe 3
//      (JArray(otherProcedures.toList) \ "code" \ "coding" \ "code").extract[Seq[String]].toSet shouldBe Set("ADA99", "ADB", "232678001")
//      (JArray(otherProcedures.toList) \ "partOf" \ "reference").extract[Seq[String]].toSet should contain("Procedure/" + (patient2Surgery.head \ "id").extract[String])
//      //both patient has this data
//      val surgicalWounds = results.filter(r => (r \ "meta" \ "profile").extract[Seq[String]].head == "https://aiccelerate.eu/fhir/StructureDefinition/AIC-SurgicalWoundClassificationObservation")
//      surgicalWounds.length shouldBe 2
//      //Both has classification 2
//      (JArray(surgicalWounds.toList) \ "valueCodeableConcept" \ "coding" \ "code").extract[Seq[String]].toSet shouldBe Set("418115006")
//      (JArray(surgicalWounds.toList) \ "valueCodeableConcept" \ "coding" \ "display").extract[Seq[String]].toSet shouldBe Set("Clean-contaminated (Class II)")
//      //Only patient 2 has this data
//      val punctures = results.filter(r => (r \ "meta" \ "profile").extract[Seq[String]].head == "https://aiccelerate.eu/fhir/StructureDefinition/AIC-IntraOperativeObservation")
//      punctures.length shouldBe 1
//      (punctures.head \ "encounter" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Encounter", "e12")
//      (punctures.head \ "valueQuantity" \ "value").extract[Int] shouldBe 2
//    }
//  }

//  it should "map test data and write it to FHIR repo successfully" in {
//    assume(fhirServerIsAvailable)
//    fhirMappingJobManager
//      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(surgeryDetailsMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
//      .map(unit =>
//        unit shouldBe()
//      )
//  }

  "preoperative assessment mapping" should "map test data" in {
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(preopAssMappingTask), job = testJob), mappingJobSourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results should not be empty
      val asaObs = results.filter(r => (r \ "meta" \ "profile").extract[Seq[String]].head == "https://aiccelerate.eu/fhir/StructureDefinition/AIC-ASAClassification")
      asaObs.length shouldBe 10
      (JArray(asaObs.toList) \ "subject" \ "reference").extract[Seq[String]].toSet shouldBe (1 to 10).map(i => s"p$i").map(pid => FhirMappingUtility.getHashedReference("Patient", pid)).toSet
      (JArray(asaObs.toList) \ "encounter" \ "reference").extract[Seq[String]].toSet shouldBe (1 to 10).map(i => s"e$i").map(pid => FhirMappingUtility.getHashedReference("Encounter", pid)).toSet

      val patient3Asa = asaObs.find(r => (r \ "subject" \ "reference").extract[String] == FhirMappingUtility.getHashedReference("Patient", "p3"))
      (patient3Asa.head \ "valueCodeableConcept" \ "coding" \ "code").extract[Seq[String]].head shouldBe "413499007"
      (patient3Asa.head \ "valueCodeableConcept" \ "coding" \ "display").extract[Seq[String]].head shouldBe "ASA5: A moribund patient who is not expected to survive without the operation"
      (patient3Asa.head \ "effectiveDateTime").extract[String] shouldBe "2011-01-07"

      val urgObs = results.filter(r => (r \ "meta" \ "profile").extract[Seq[String]].head == "https://aiccelerate.eu/fhir/StructureDefinition/AIC-OperationUrgency")
      urgObs.length shouldBe 10
      (JArray(urgObs.toList) \ "subject" \ "reference").extract[Seq[String]].toSet shouldBe (1 to 10).map(i => s"p$i").map(pid => FhirMappingUtility.getHashedReference("Patient", pid)).toSet
      (JArray(urgObs.toList) \ "encounter" \ "reference").extract[Seq[String]].toSet shouldBe (1 to 10).map(i => s"e$i").map(pid => FhirMappingUtility.getHashedReference("Encounter", pid)).toSet
      val patient7Urg = urgObs.find(r => (r \ "subject" \ "reference").extract[String] == FhirMappingUtility.getHashedReference("Patient", "p7"))
      (patient7Urg.head \ "valueCodeableConcept" \ "coding" \ "code").extract[Seq[String]].head shouldBe "R3"
      (patient7Urg.head \ "valueCodeableConcept" \ "coding" \ "display").extract[Seq[String]].head shouldBe "Elective patient: (in 6 months)"

      val pregnancyObs = results.filter(r => (r \ "meta" \ "profile").extract[Seq[String]].head == "https://aiccelerate.eu/fhir/StructureDefinition/AIC-PregnancyStatus")
      pregnancyObs.length shouldBe 5
      (JArray(pregnancyObs.toList) \ "subject" \ "reference").extract[Seq[String]].toSet shouldBe (6 to 10).map(i => s"p$i").map(pid => FhirMappingUtility.getHashedReference("Patient", pid)).toSet
      val pregnantPatients = pregnancyObs.filter(r => (r \ "valueCodeableConcept" \ "coding" \ "code").extract[Seq[String]].head == "77386006")
      pregnantPatients.length shouldBe 1
      (pregnantPatients.head \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p9")

      val operationFlags = results.filter(r => (r \ "meta" \ "profile").extract[Seq[String]].head == "https://aiccelerate.eu/fhir/StructureDefinition/AIC-OperationFlag")
      operationFlags.length shouldBe 3
      (JArray(operationFlags.toList) \ "code" \ "coding" \ "code").extract[Seq[String]].toSet shouldBe Set("40174006", "day-case-surgery", "169741004")
      //Post menstrual age
      val pmage = results.filter(r => (r \ "meta" \ "profile").extract[Seq[String]].head == "https://aiccelerate.eu/fhir/StructureDefinition/AIC-PostMenstrualAge")
      pmage.length shouldBe 7
      (JArray(pmage.toList) \ "valueQuantity" \ "value").extract[Seq[Int]].toSet shouldBe Set(24, 27, 19, 35, 19, 26, 21, 29)
      //Post gestational age
      val pgage = results.filter(r => (r \ "meta" \ "profile").extract[Seq[String]].head == "https://aiccelerate.eu/fhir/StructureDefinition/AIC-PostGestationalAge")
      pgage.length shouldBe 6

      val bloodGroupObs = results.filter(r => (r \ "meta" \ "profile").extract[Seq[String]].head == "https://aiccelerate.eu/fhir/StructureDefinition/AIC-BloodGroupObservation")
      bloodGroupObs.length shouldBe 8
      val patient5BloodGroup = bloodGroupObs.find(r => (r \ "subject" \ "reference").extract[String] == FhirMappingUtility.getHashedReference("Patient", "p5"))
      patient5BloodGroup should not be empty
      (patient5BloodGroup.head \ "valueCodeableConcept" \ "coding" \ "code").extract[Seq[String]].head shouldBe "278151004"
      (patient5BloodGroup.head \ "valueCodeableConcept" \ "coding" \ "display").extract[Seq[String]].head shouldBe "AB Rh(D) positive"
    }
  }
  it should "map test data and write it to FHIR repo successfully" in {
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(preopAssMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "health behaviour assessment mapping" should "map test data" in {
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(healthBehaviorMappingTask), job = testJob), mappingJobSourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 9
      (results.apply(1) \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p2")
      //patient 3 has smoking status info
      (results.apply(2) \ "code" \ "coding" \ "code").extract[Seq[String]].head shouldBe "72166-2"

      (results.apply(2) \ "valueCodeableConcept" \ "coding" \ "code").extract[Seq[String]].head shouldBe "266919005"
      (results.apply(1) \ "valueCodeableConcept" \ "coding" \ "display").extract[Seq[String]].head shouldBe "Former smoker"

      (results.apply(4) \ "valueCodeableConcept" \ "coding" \ "display").extract[Seq[String]].head shouldBe "Current some day user"
      (results.apply(2) \ "encounter" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Encounter", "e1")
      (results.apply(8) \ "effectiveDateTime").extract[String] shouldBe "2000-10-09"
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(healthBehaviorMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "other observation mapping" should "map test data" in {
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(otherObsMappingTask), job = testJob), mappingJobSourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 14
      (results.apply(1) \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p2")
      (results.apply(2) \ "encounter" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Encounter", "e3")

      (results.apply(4) \ "valueQuantity" \ "value").extract[Double] shouldBe 43.2
      (results.apply(4) \ "meta" \ "profile").extract[Seq[String]].head shouldBe "https://aiccelerate.eu/fhir/StructureDefinition/AIC-IntraOperativeObservation"
      (results.apply(4) \ "valueQuantity" \ "unit").extract[String] shouldBe "mL"
      (results.apply(4) \ "code" \ "coding" \ "code").extract[Seq[String]].head shouldBe "1298-9"
      (results.apply(4) \ "code" \ "coding" \ "display").extract[Seq[String]].head shouldBe "RBC given"

      (results.apply(10) \ "meta" \ "profile").extract[Seq[String]].head shouldBe "https://aiccelerate.eu/fhir/StructureDefinition/AIC-MedicationAdministration"
      (results.apply(8) \ "meta" \ "profile").extract[Seq[String]].head shouldBe "https://aiccelerate.eu/fhir/StructureDefinition/AIC-PEWSScore"

      (results.apply(13) \ "component" \ "valueQuantity" \ "value").extract[Seq[Int]] shouldBe Seq(3, 5, 4)
      (results.apply(13) \ "valueQuantity" \ "value").extract[Int] shouldBe 4
    }
  }
  it should "map test data and write it to FHIR repo successfully" in {
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(otherObsMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "medication used mapping" should "map test data" in {
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(medUsedMappingTask), job = testJob), mappingJobSourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 5

      (results.apply(1) \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p2")
      (results.head \ "context" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Encounter", "e1")

      (results.apply(2) \ "medicationCodeableConcept" \ "coding" \ "code").extract[Seq[String]].head shouldBe "J01DD02"
      (results.apply(2) \ "medicationCodeableConcept" \ "coding" \ "display").extract[Seq[String]].head shouldBe "medication2"
      (results.apply(2) \ "dosage" \ "timing" \ "repeat" \ "frequency").extract[Seq[Int]].head shouldBe 1
      (results.apply(2) \ "dosage" \ "doseAndRate" \ "doseQuantity" \ "value").extract[Seq[Double]].head shouldBe 20.0
      (results.apply(2) \ "dosage" \ "doseAndRate" \ "doseQuantity" \ "unit").extract[Seq[String]].head shouldBe "mg"
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(medUsedMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "medication administration mapping" should "map test data" in {
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(medAdmMappingTask), job = testJob), mappingJobSourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 5
      (results.apply(2) \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p3")
      (results.apply(3) \ "context" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Encounter", "e3")

      (results.apply(2) \ "medicationCodeableConcept" \ "coding" \ "code").extract[Seq[String]].head shouldBe "L01CA02"
      (results.apply(2) \ "medicationCodeableConcept" \ "coding" \ "display").extract[Seq[String]].head shouldBe "medication2"
      (results.apply(1) \ "medicationCodeableConcept" \ "coding" \ "display").extract[Seq[String]] shouldBe empty
      (results.apply(1) \ "effectivePeriod" \ "start").extract[String] shouldBe "2005-05-29T10:20:00+01:00"
      (results.apply(4) \ "dosage" \ "dose" \ "value").extract[Double] shouldBe 40.5
      (results.apply(4) \ "dosage" \ "dose" \ "code").extract[String] shouldBe "mg"
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(medAdmMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "conditions mapping" should "map test data" in {
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(conditionMappingTask), job = testJob), mappingJobSourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 5
      (results.apply(2) \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p3")
      (results.apply(1) \ "encounter" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Encounter", "e2")
      (results.apply(4) \ "code" \ "coding" \ "code").extract[Seq[String]].head shouldBe "G40.419"
      (results.apply(4) \ "code" \ "coding" \ "display").extract[Seq[String]].head shouldBe "Other generalized epilepsy and epileptic syndromes, intractable, without status epilepticus"
      (results.apply(1) \ "onsetDateTime").extract[String] shouldBe "2013-05-07"
      (results.head \ "category" \ "coding" \ "code").extract[Seq[String]].head shouldBe "problem-list-item"
      (results.apply(2) \ "verificationStatus" \ "coding" \ "code").extract[Seq[String]].head shouldBe "unconfirmed"

      (results.apply(4) \ "asserter" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Practitioner", "pr2")
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(conditionMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "vital signs mapping" should "map test data" in {
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(vitalSignsMappingTask), job = testJob), mappingJobSourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 8
      (results.apply(5) \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p2")
      (results.head \ "encounter" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Encounter", "e1")
      (results.apply(4) \ "code" \ "coding" \ "code").extract[Seq[String]].head shouldBe "8867-4"
      (results.apply(4) \ "code" \ "coding" \ "display").extract[Seq[String]].head shouldBe "Heart rate"

      (results.apply(2) \ "valueQuantity" \ "value").extract[Double] shouldBe 25.5
      (results.apply(2) \ "valueQuantity" \ "code").extract[String] shouldBe "kg/m2"

      (results.apply(5) \ "component" \ "valueQuantity" \ "value").extract[Seq[Double]] shouldBe Seq(132, 95)
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(vitalSignsMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

//  "lab results mapping" should "map test data" in {
//    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(labResultsMappingTask), job = testJob), sourceSettings = dataSourceSettings) map { mappingResults =>
//      val results = mappingResults.map(r => {
//        r.mappedResource shouldBe defined
//        val resource = r.mappedResource.get.parseJson
//        resource shouldBe a[Resource]
//        resource
//      })
//      results.length shouldBe 26
//      (results.apply(1) \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p2")
//      (results.apply(1) \ "encounter" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Encounter", "e2")
//      (results.head \ "effectiveDateTime").extract[String] shouldBe "2012-05-10T10:00:00+01:00"
//
//      (results.head \ "code" \ "coding" \ "code").extract[Seq[String]].toSet shouldBe Set("1552", "718-7")
//      (results.head \ "valueQuantity" \ "value").extract[Double] shouldBe 10.5
//      (results.head \ "valueQuantity" \ "unit").extract[String] shouldBe "g/l"
//
//      (results.apply(6) \ "valueQuantity" \ "value").extract[Double] shouldBe 15.0
//      (results.apply(6) \ "valueQuantity" \ "unit").extract[String] shouldBe "kPa"
//
//      (results.apply(3) \ "interpretation" \ "coding" \ "code").extract[Seq[String]].head shouldBe "N"
//
//      (results.last \ "code" \ "coding" \ "code").extract[Seq[String]] shouldBe Seq("99999")
//      (results.last \ "code" \ "coding" \ "display").extract[Seq[String]] shouldBe Seq("Blabla")
//    }
//  }


//  it should "map test data and write it to FHIR repo successfully" in {
//    assume(fhirServerIsAvailable)
//    fhirMappingJobManager
//      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(labResultsMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
//      .map(unit =>
//        unit shouldBe()
//      )
//  }

  "radiological studies mapping" should "map test data" in {
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(radStudiesMappingTask), job = testJob), mappingJobSourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 5
      (results.head \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p1")
      (results.head \ "encounter" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Encounter", "e1")
      (results.apply(1) \ "effectiveDateTime").extract[String] shouldBe "2012-05-10"
      (results.apply(1) \ "code" \ "coding" \ "code").extract[Seq[String]].head shouldBe "PA2AT"
      (results.apply(1) \ "code" \ "coding" \ "display").extract[Seq[String]].head shouldBe "Cerebral artery PTA"
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(radStudiesMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "hospital unit mapping" should "map test data" in {
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(hospitalUnitMappingTask), job = testJob), mappingJobSourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 4
      (results.apply(1) \ "type" \ "coding" \ "code").extract[Seq[String]] shouldBe Seq("309904001")
      (results.apply(1) \ "physicalType" \ "coding" \ "code").extract[Seq[String]] shouldBe Seq("ro")
      (results.apply(2) \ "id").extract[String] shouldBe FhirMappingUtility.getHashedId("Location", "ward1")

      (results.last \ "partOf" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Location", "ward1")
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(hospitalUnitMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "practitioner mapping" should "map test data" in {
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(practitionerMappingTask), job = testJob), mappingJobSourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 8

      val practitioners = results.filter(r => (r \ "meta" \ "profile").extract[Seq[String]].head == "https://aiccelerate.eu/fhir/StructureDefinition/AIC-Practitioner")
      practitioners.length shouldBe 4
      (practitioners.head \ "name" \ "family").extract[Seq[String]] shouldBe Seq("NAMLI")
      (practitioners.last \ "qualification" \ "code" \ "coding" \ "code").extract[Seq[String]].head shouldBe "RN"

      val practitionerRoles = results.filter(r => (r \ "meta" \ "profile").extract[Seq[String]].head == "https://aiccelerate.eu/fhir/StructureDefinition/AIC-PractitionerRoleForSurgicalWorkflow")
      (practitionerRoles.head \ "code" \ "coding" \ "display").extract[Seq[String]].head shouldBe "Specialized surgeon"
      (practitionerRoles.head \ "specialty" \ "coding" \ "display").extract[Seq[String]].head shouldBe "Cardiology"
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(practitionerMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "workshift mapping" should "map test data" in {
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(workshiftMappingTask), job = testJob), mappingJobSourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 2
      (results.head \ "actor" \ "reference").extract[Seq[String]].toSet shouldBe Set(FhirMappingUtility.getHashedReference("PractitionerRole", "pr1"), FhirMappingUtility.getHashedReference("Location", "loc1"))
      (results.head \ "extension" \ "valueBoolean").extract[Seq[Boolean]].head shouldBe false
      (results.head \ "planningHorizon" \ "start").extract[String] shouldBe "2017-01-18"
      (results.head \ "planningHorizon" \ "end").extract[String] shouldBe "2017-01-19"
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(workshiftMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "background information mapping" should "map test data" in {
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(backgroundInfMappingTask), job = testJob), mappingJobSourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 30
      val patient1Flags = results.filter(r => (r \ "subject" \ "reference").extract[String] == FhirMappingUtility.getHashedReference("Patient", "p1"))
      (JArray(patient1Flags.toList) \ "period" \ "start").extract[Seq[String]].toSet shouldBe Set("2021-10-05T10:00:00+01:00")

      val flag8 = patient1Flags.find(r => (r \ "code" \ "coding" \ "code").extract[Seq[String]].contains("366248005"))
      flag8 should not be empty
      (flag8.head \ "code" \ "coding" \ "display").extract[Seq[String]].head shouldBe "Pivot tooth, partial denture"
      (flag8.head \ "code" \ "coding" \ "system").extract[Seq[String]].head shouldBe "http://snomed.info/sct"
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(backgroundInfMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "preoperative risk factors mapping" should "map test data" in {
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(preopRisksMappingTask), job = testJob), mappingJobSourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 3
      (results.apply(1) \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p2")
      (results.apply(1) \ "encounter" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Encounter", "e2")
      (results.head \ "effectivePeriod" \ "start").extract[String] shouldBe "2017-10-05T10:00:00+01:00"
      (results.apply(1) \ "code" \ "coding" \ "code").extract[Seq[String]].head shouldBe "62014003+366667001"
      (results.apply(1) \ "code" \ "coding" \ "display").extract[Seq[String]].head shouldBe "Adverse reaction caused by drug (disorder) + Skin Reaction (finding)"
      val comps = (results.apply(1) \ "component").extract[Seq[JObject]]
      comps.length shouldBe 3
      (comps.head \ "code" \ "coding" \ "code").extract[Seq[String]].head shouldBe "246112005"
      (comps.head \ "valueBoolean").extract[Boolean] shouldBe true
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(preopRisksMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "patient reported conditions mapping" should "map test data" in {
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(patientReportedConditionsMappingTask), job = testJob), mappingJobSourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 20
      val patient1Conds = results.filter(r => (r \ "subject" \ "reference").extract[String] == FhirMappingUtility.getHashedReference("Patient", "p1"))
      val circDisease = patient1Conds.find(r => (r \ "code" \ "coding" \ "code").extract[Seq[String]].head == "400047006")
      circDisease should not be empty
      (circDisease.head \ "code" \ "coding" \ "display").extract[Seq[String]].head shouldBe "circulation diseases in lower extremities"
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(patientReportedConditionsMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "preoperative symptoms mapping" should "map test data" in {
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(preopSymptomsMappingTask), job = testJob), mappingJobSourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 38
      val patient1Symptoms = results.filter(r => (r \ "subject" \ "reference").extract[String] == FhirMappingUtility.getHashedReference("Patient", "p1"))
      patient1Symptoms.length shouldBe 19
      (patient1Symptoms.head \ "encounter" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Encounter", "e1")

      (patient1Symptoms.apply(11) \ "code" \ "coding" \ "code").extract[Seq[String]].head shouldBe "267036007:371881003=870595007"
      (patient1Symptoms.apply(11) \ "code" \ "coding" \ "display").extract[Seq[String]].head shouldBe "Dyspnea during walking"
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(preopSymptomsMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }

  "anesthesia observations mapping" should "map test data" in {
    fhirMappingJobManager.executeMappingTaskAndReturn(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(anestObsMappingTask), job = testJob), mappingJobSourceSettings = dataSourceSettings) map { mappingResults =>
      val results = mappingResults.map(r => {
        r.mappedResource shouldBe defined
        val resource = r.mappedResource.get.parseJson
        resource shouldBe a[Resource]
        resource
      })
      results.length shouldBe 130
      val p1 = results.filter(r => (r \ "encounter" \ "reference").extractOpt[String].contains(FhirMappingUtility.getHashedReference("Encounter", "e1")))
      p1.length shouldBe 65
      (p1.apply(35) \ "effectiveDateTime").extract[String] shouldBe "2015-05-10T10:25:00+01:00"

      (p1.apply(42) \ "code" \ "coding" \ "code").extract[Seq[String]].head shouldBe "61010-5"
      (p1.apply(42) \ "code" \ "coding" \ "display").extract[Seq[String]].head shouldBe "Burst suppression ratio [Ratio] Cerebral cortex Electroencephalogram (EEG)"
      (p1.apply(42) \ "valueQuantity" \ "value").extract[Double] shouldBe 0.277386451
      (p1.apply(42) \ "valueQuantity" \ "unit").extract[String] shouldBe "%"
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    assume(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(mappingJobExecution = FhirMappingJobExecution(mappingTasks = Seq(anestObsMappingTask), job = testJob), sourceSettings = dataSourceSettings, sinkSettings = fhirSinkSetting)
      .map(unit =>
        unit shouldBe()
      )
  }
}
