package io.onfhir.tofhir.engine

import akka.http.scaladsl.model.StatusCodes
import io.onfhir.client.OnFhirNetworkClient
import io.onfhir.path.FhirPathEvaluator
import io.onfhir.tofhir.ToFhirTestSpec
import io.onfhir.tofhir.model.{FhirMappingTask, FhirRepositorySinkSettings, FileSystemSource, FileSystemSourceSettings, SourceFileFormats}
import io.onfhir.tofhir.util.FhirMappingUtility
import io.onfhir.util.JsonFormatter.formats
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.json4s.JArray

import java.net.URI
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

class Pilot1IntegrationTest extends ToFhirTestSpec {

  val sparkConf:SparkConf = new SparkConf()
    .setAppName("tofhir-test")
    .setMaster("local[4]")
    .set("spark.driver.allowMultipleContexts", "false")
    .set("spark.ui.enabled", "false")

  val sparkSession: SparkSession = SparkSession.builder().config(sparkConf).getOrCreate()

  Paths.get(".") .toAbsolutePath

  val mappingRepository: IFhirMappingRepository =
    new FhirMappingFolderRepository(Paths.get("mappings/pilot1").toAbsolutePath.toUri)

  val contextLoader: IMappingContextLoader = new MappingContextLoader(mappingRepository)

  val schemaRepository = new SchemaFolderRepository(Paths.get("schemas/pilot1").toAbsolutePath.toUri)

  val dataSourceSettings: FileSystemSourceSettings = FileSystemSourceSettings("test-source-1", "http://hus.fi", Paths.get("test-data/pilot1").toAbsolutePath.toUri)

  val fhirMappingJobManager = new FhirMappingJobManager(mappingRepository, contextLoader, schemaRepository, sparkSession)

  val fhirSinkSetting: FhirRepositorySinkSettings = FhirRepositorySinkSettings("http://localhost:8081/fhir")
  val onFhirClient = OnFhirNetworkClient.apply(fhirSinkSetting.fhirRepoUrl)

  val fhirServerIsAvailable =
    Try(Await.result(onFhirClient.search("Patient").execute(), FiniteDuration(5, TimeUnit.SECONDS)).httpStatus == StatusCodes.OK)
      .getOrElse(false)

  val patientMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot1/patient-mapping",
    sourceContext = Map("source" ->  FileSystemSource(
      path = "patients.csv",
      sourceType = SourceFileFormats.CSV,
      dataSourceSettings
    ))
  )
  val encounterMappingTask =
    FhirMappingTask(
      mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot1/operation-episode-encounter-mapping",
      sourceContext = Map("source" ->  FileSystemSource(
        path = "operation-episode-encounters.csv",
        sourceType = SourceFileFormats.CSV,
        dataSourceSettings
      ))
    )
  val surgeryPlanMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot1/surgery-plan-mapping",
    sourceContext = Map("source" ->FileSystemSource(
      path = "surgery-plans.csv",
      sourceType = SourceFileFormats.CSV,
      dataSourceSettings
    ))
  )

  val surgeryDetailsMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot1/surgery-details-mapping",
    sourceContext = Map("source" ->FileSystemSource(
      path = "surgery-details.csv",
      sourceType = SourceFileFormats.CSV,
      dataSourceSettings
    ))
  )

  val preopAssMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot1/preoperative-assessment-mapping",
    sourceContext = Map("source" ->FileSystemSource(
      path = "preoperative-assessment.csv",
      sourceType = SourceFileFormats.CSV,
      dataSourceSettings
    ))
  )

  val healthBehaviorMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot1/health-behavior-assessment-mapping",
    sourceContext = Map("source" ->FileSystemSource(
      path = "health-behavior-assessment.csv",
      sourceType = SourceFileFormats.CSV,
      dataSourceSettings
    ))
  )

  val otherObsMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot1/other-observation-mapping",
    sourceContext = Map("source" ->FileSystemSource(
      path = "other-observations.csv",
      sourceType = SourceFileFormats.CSV,
      dataSourceSettings
    ))
  )

  val medUsedMappingTask = FhirMappingTask(
    mappingRef = "https://aiccelerate.eu/fhir/mappings/pilot1/medication-used-mapping",
    sourceContext = Map("source" ->FileSystemSource(
      path = "medication-used.csv",
      sourceType = SourceFileFormats.CSV,
      dataSourceSettings
    ))
  )

  "patient mapping" should "map test data" in {
    //Some semantic tests on generated content
    fhirMappingJobManager.executeMappingTaskAndReturn(task = patientMappingTask) map { results =>
      val genders = (JArray(results.toList) \ "gender").extract[Seq[String]]
      genders shouldBe ((1 to 5).map(_ => "male") ++ (6 to 10).map(_=> "female"))

      (results.apply(2) \ "id").extract[String] shouldBe FhirMappingUtility.getHashedId("Patient", "p3")
      (results.apply(2) \ "identifier" \ "value").extract[Seq[String]] shouldBe Seq("p3")
      (results.apply(2) \ "birthDate").extract[String] shouldBe("1997-02")
      (results.apply(3) \ "address" \ "postalCode").extract[Seq[String]] shouldBe(Seq("H10564"))
      (results.apply(4) \ "deceasedDateTime" ).extract[String] shouldBe("2019-04-21")
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    //Send it to our fhir repo if they are also validated
   assert(fhirServerIsAvailable)
   fhirMappingJobManager
     .executeMappingJob(tasks = Seq(patientMappingTask), sinkSettings = fhirSinkSetting)
     .map( unit =>
       unit shouldBe ()
     )
  }


  "operation episode encounter mapping" should "map test data" in {
    fhirMappingJobManager.executeMappingTaskAndReturn(task = encounterMappingTask) map { results =>
      results.size shouldBe(10)
      (results.apply(1) \ "id").extract[String] shouldBe FhirMappingUtility.getHashedId("Encounter", "e2")
      (results.apply(1) \ "identifier" \ "value").extract[Seq[String]] shouldBe Seq("e2")
      (results.apply(1) \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p2")
      (results.apply(3) \ "episodeOfCare" \ "reference").extract[Seq[String]] shouldBe Seq( FhirMappingUtility.getHashedReference("EpisodeOfCare", "ep4"))
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
    assert(fhirServerIsAvailable)
     fhirMappingJobManager
       .executeMappingJob(tasks = Seq(encounterMappingTask), sinkSettings = fhirSinkSetting)
       .map( unit =>
         unit shouldBe ()
       )
  }

  "surgery plan mapping" should "map test data" in {
    fhirMappingJobManager.executeMappingTaskAndReturn(task = surgeryPlanMappingTask) map { results =>
      results.size shouldBe 4
      //ServiceRequests
      val sr1 = results.find(r => (r \"id").extractOpt[String].contains(FhirMappingUtility.getHashedId("ServiceRequest", "sp1")))
      val sr2 = results.find(r => (r \"id").extractOpt[String].contains(FhirMappingUtility.getHashedId("ServiceRequest", "sp2")))
      sr1.isDefined shouldBe true
      sr2.isDefined shouldBe true
      //Encounters
      val e1 = results.find(r => (r \"episodeOfCare" \ "reference").extractOpt[Seq[String]].contains(Seq(FhirMappingUtility.getHashedReference("EpisodeOfCare","ep11"))))
      val e2 = results.find(r => (r \"episodeOfCare" \ "reference").extractOpt[Seq[String]].contains(Seq(FhirMappingUtility.getHashedReference("EpisodeOfCare","ep12"))))
      e1.isDefined shouldBe true
      e2.isDefined shouldBe true

      (sr1.get \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient", "p1")
      (sr2.get \ "encounter" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Encounter","e22")
      (sr1.get \ "occurrencePeriod" \ "start").extract[String] shouldBe "2007-09-22T10:00:00+01:00"
      (sr2.get \ "occurrencePeriod" \ "end").extract[String] shouldBe "2007-09-22T16:00:00+01:00"
      (sr1.get \ "requester" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Practitioner","pr1")
      (sr1.get \ "performer" \ "reference").extractOpt[Seq[String]] shouldBe Some(Seq(FhirMappingUtility.getHashedReference("PractitionerRole","pr1")))
      (sr2.get \ "code" \ "coding" \ "code").extract[Seq[String]] shouldBe Seq("AAA27")

      (e1.get \ "subject" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Patient","p1")
      (e2.get \ "type" \ "coding" \ "code").extract[Seq[String]] shouldBe Seq("305351004")
    }

  }

  it should "map test data and write it to FHIR repo successfully" in {
   assert(fhirServerIsAvailable)
   fhirMappingJobManager
     .executeMappingJob(tasks = Seq(surgeryPlanMappingTask), sinkSettings = fhirSinkSetting)
     .map( unit =>
       unit shouldBe ()
     )
  }

  "surgery details mapping" should "map test data" in {
    fhirMappingJobManager.executeMappingTaskAndReturn(task = surgeryDetailsMappingTask) map { results =>
      results.size shouldBe 15
      val surgeryEncounters = results.filter(r => (r \ "meta" \ "profile").extract[Seq[String]].head == "https://aiccelerate.eu/fhir/StructureDefinition/AIC-SurgeryEncounter")
      surgeryEncounters.size shouldBe 2
      (surgeryEncounters.head \ "id").extract[String] shouldBe FhirMappingUtility.getHashedId("Encounter", "e11")
      (surgeryEncounters.head \ "serviceType" \ "coding" \ "code").extract[Seq[String]] shouldBe Seq("394609007")
      (surgeryEncounters.head \ "serviceType" \ "coding" \ "display").extract[Seq[String]] shouldBe Seq("General surgery")
      (surgeryEncounters.last \ "episodeOfCare" \ "reference").extract[Seq[String]] shouldBe Seq(FhirMappingUtility.getHashedReference("EpisodeOfCare", "ep2"))
      (surgeryEncounters.last \ "location" \ "location" \ "reference").extract[Seq[String]] shouldBe Seq(FhirMappingUtility.getHashedReference("Location", "or1"))
      (surgeryEncounters.head \ "basedOn" \  "reference").extract[Seq[String]] shouldBe Seq(FhirMappingUtility.getHashedReference("ServiceRequest", "sp1"))
      (surgeryEncounters.last \ "basedOn" \  "reference").extract[Seq[String]] shouldBe empty
      (surgeryEncounters.head \ "participant" \ "individual" \ "reference").extract[Seq[String]].length shouldBe 5
      //p1 has all 3 phases
      val otherPhases = results.filter(r => (r \ "meta" \ "profile").extract[Seq[String]].head == "https://aiccelerate.eu/fhir/StructureDefinition/AIC-OperationPhaseDetails")
      otherPhases.length shouldBe 3
      //Enrollment
      val enrollment = otherPhases.find(p => (p \ "category" \ "coding" \ "code").extract[Seq[String]] == Seq("305408004"))
      enrollment should not be empty
      (enrollment.head \ "performedPeriod" \ "start").extract[String] shouldBe "2015-05-17T10:05:00+01:00"
      //Both patients has anesthesia data
      val anesthesiaPhases = results.filter(r => (r \ "meta" \ "profile").extract[Seq[String]].head == "https://aiccelerate.eu/fhir/StructureDefinition/AIC-AnesthesiaPhaseDetails")
      anesthesiaPhases.length shouldBe 2
      val patient1AnesthesiaProcedure = anesthesiaPhases.find(r => (r \ "subject" \ "reference").extract[String] == FhirMappingUtility.getHashedReference("Patient", "p1"))
      patient1AnesthesiaProcedure should not be empty
      (patient1AnesthesiaProcedure.head \ "code" \ "coding" \ "code").extract[Seq[String]].toSet shouldBe Set("50697003","WX402")
      (patient1AnesthesiaProcedure.head \ "code" \ "coding" \ "display").extract[Seq[String]].toSet shouldBe Set("General Anesthesia","General anesthesis")

      val surgeryPhases = results.filter(r => (r \ "meta" \ "profile").extract[Seq[String]].head == "https://aiccelerate.eu/fhir/StructureDefinition/AIC-SurgeryPhaseDetails")
      surgeryPhases.length shouldBe 2
      val patient1Surgery = surgeryPhases.find(r => (r \ "subject" \ "reference").extract[String] == FhirMappingUtility.getHashedReference("Patient", "p1"))
      patient1Surgery should not be empty
      (patient1Surgery.head \ "code" \ "coding" \ "code").extract[Seq[String]] shouldBe Seq("AAC00")
      (patient1Surgery.head \ "code" \ "coding" \ "display").extract[Seq[String]] shouldBe Seq("Ligature of intracranial aneurysm")
      //3 surgeons for p1
      FhirPathEvaluator().evaluateString("Procedure.performer.where(function.coding.where(code='304292004').exists()).actor.reference", patient1Surgery.head ).length shouldBe 3

      val patient2Surgery = surgeryPhases.find(r => (r \ "subject" \ "reference").extract[String] == FhirMappingUtility.getHashedReference("Patient", "p2"))
      patient2Surgery should not be empty
      //For patient 2, 2 procedure from other procedures and 1 from intubation
      val otherProcedures = results.filter(r => (r \ "meta" \ "profile").extract[Seq[String]].head == "https://aiccelerate.eu/fhir/StructureDefinition/AIC-ProcedureRelatedWithSurgicalWorkflow")
      otherProcedures.length shouldBe 3
      (JArray(otherProcedures.toList) \ "code" \ "coding" \ "code").extract[Seq[String]].toSet shouldBe Set("ADA99","ADB","232678001")
      (JArray(otherProcedures.toList) \ "partOf" \ "reference").extract[Seq[String]].toSet should contain ("Procedure/"+(patient2Surgery.head \ "id").extract[String])
      //both patient has this data
      val surgicalWounds = results.filter(r => (r \ "meta" \ "profile").extract[Seq[String]].head == "https://aiccelerate.eu/fhir/StructureDefinition/AIC-SurgicalWoundClassificationObservation")
      surgicalWounds.length shouldBe 2
      //Both has classification 2
      (JArray(surgicalWounds.toList) \ "valueCodeableConcept" \ "coding" \ "code").extract[Seq[String]].toSet shouldBe Set("418115006")
      (JArray(surgicalWounds.toList) \ "valueCodeableConcept" \ "coding" \ "display").extract[Seq[String]].toSet shouldBe Set("Clean-contaminated (Class II)")
      //Only patient 2 has this data
      val punctures = results.filter(r => (r \ "meta" \ "profile").extract[Seq[String]].head == "https://aiccelerate.eu/fhir/StructureDefinition/AIC-IntraOperativeObservation")
      punctures.length shouldBe 1
      (punctures.head \ "encounter" \ "reference").extract[String] shouldBe FhirMappingUtility.getHashedReference("Encounter", "e12")
      (punctures.head \ "valueQuantity" \ "value").extract[Int] shouldBe 2
    }
  }

  it should "map test data and write it to FHIR repo successfully" in {
    assert(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(tasks = Seq(surgeryDetailsMappingTask), sinkSettings = fhirSinkSetting)
      .map( unit =>
        unit shouldBe ()
      )
  }

  "preoperative assessment mapping" should "map test data" in {
    fhirMappingJobManager.executeMappingTaskAndReturn(task = preopAssMappingTask) map { results =>
      results should not be empty
      val asaObs = results.filter(r => (r \ "meta" \ "profile").extract[Seq[String]].head == "https://aiccelerate.eu/fhir/StructureDefinition/AIC-ASAClassification")
      asaObs.length shouldBe 10
      (JArray(asaObs.toList) \ "subject" \ "reference").extract[Seq[String]].toSet shouldBe (1 to 10).map(i => s"p$i").map(pid => FhirMappingUtility.getHashedReference("Patient", pid)).toSet
      (JArray(asaObs.toList) \ "encounter" \ "reference").extract[Seq[String]].toSet shouldBe (1 to 10).map(i => s"e$i").map(pid => FhirMappingUtility.getHashedReference("Encounter", pid)).toSet

      val patient3Asa  = asaObs.find(r => (r \ "subject" \ "reference").extract[String] == FhirMappingUtility.getHashedReference("Patient", "p3"))
      (patient3Asa.head \ "valueCodeableConcept" \ "coding" \ "code").extract[Seq[String]].head shouldBe "413499007"
      (patient3Asa.head \ "valueCodeableConcept" \ "coding" \ "display").extract[Seq[String]].head shouldBe "ASA5: A moribund patient who is not expected to survive without the operation"
      (patient3Asa.head \ "effectiveDateTime").extract[String] shouldBe "2011-01-07"

      val urgObs = results.filter(r => (r \ "meta" \ "profile").extract[Seq[String]].head == "https://aiccelerate.eu/fhir/StructureDefinition/AIC-OperationUrgency")
      urgObs.length shouldBe 10
      (JArray(urgObs.toList) \ "subject" \ "reference").extract[Seq[String]].toSet shouldBe (1 to 10).map(i => s"p$i").map(pid => FhirMappingUtility.getHashedReference("Patient", pid)).toSet
      (JArray(urgObs.toList) \ "encounter" \ "reference").extract[Seq[String]].toSet shouldBe (1 to 10).map(i => s"e$i").map(pid => FhirMappingUtility.getHashedReference("Encounter", pid)).toSet
      val patient7Urg  = urgObs.find(r => (r \ "subject" \ "reference").extract[String] == FhirMappingUtility.getHashedReference("Patient", "p7"))
      (patient7Urg.head  \ "valueCodeableConcept" \ "coding" \ "code").extract[Seq[String]].head shouldBe "R3"
      (patient7Urg.head  \ "valueCodeableConcept" \ "coding" \ "display").extract[Seq[String]].head shouldBe "Elective patient: (in 6 months)"

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

      val bloodGroupObs = results.filter(r => (r \ "meta" \ "profile").extract[Seq[String]].head =="https://aiccelerate.eu/fhir/StructureDefinition/AIC-BloodGroupObservation")
      bloodGroupObs.length shouldBe 8
      val patient5BloodGroup = bloodGroupObs.find(r => (r \ "subject" \ "reference").extract[String]  == FhirMappingUtility.getHashedReference("Patient", "p5"))
      patient5BloodGroup should not be empty
      (patient5BloodGroup.head \ "valueCodeableConcept" \ "coding" \ "code").extract[Seq[String]].head shouldBe "278151004"
      (patient5BloodGroup.head \ "valueCodeableConcept" \ "coding" \ "display").extract[Seq[String]].head shouldBe "AB Rh(D) positive"
    }
  }
  it should "map test data and write it to FHIR repo successfully" in {
    assert(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(tasks = Seq(preopAssMappingTask), sinkSettings = fhirSinkSetting)
      .map( unit =>
        unit shouldBe ()
      )
  }

  "health behaviour assessment mapping" should "map test data" in {
    fhirMappingJobManager.executeMappingTaskAndReturn(task = healthBehaviorMappingTask) map { results =>
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
    assert(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(tasks = Seq(healthBehaviorMappingTask), sinkSettings = fhirSinkSetting)
      .map( unit =>
        unit shouldBe ()
      )
  }

  "other observation mapping" should "map test data" in {
    fhirMappingJobManager.executeMappingTaskAndReturn(task = otherObsMappingTask) map { results =>
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
    assert(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(tasks = Seq(otherObsMappingTask), sinkSettings = fhirSinkSetting)
      .map( unit =>
        unit shouldBe ()
      )
  }

  "medication used mapping" should "map test data" in {
    fhirMappingJobManager.executeMappingTaskAndReturn(task = medUsedMappingTask) map { results =>
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
    assert(fhirServerIsAvailable)
    fhirMappingJobManager
      .executeMappingJob(tasks = Seq(medUsedMappingTask), sinkSettings = fhirSinkSetting)
      .map( unit =>
        unit shouldBe ()
      )
  }


}
