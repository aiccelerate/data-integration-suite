package eu.aiccelerate.dis

import io.onfhir.tofhir.config.MappingErrorHandling.MappingErrorHandling
import io.onfhir.tofhir.config.{MappingErrorHandling, ToFhirConfig}
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should
import org.scalatest.{Inside, Inspectors, OptionValues}

class PilotTestSpec extends AsyncFlatSpec with should.Matchers with
  OptionValues with Inside with Inspectors {

  val mappingErrorHandling: MappingErrorHandling = MappingErrorHandling.HALT
  val fhirWriteErrorHandling: MappingErrorHandling = MappingErrorHandling.HALT

  val sparkConf: SparkConf = new SparkConf()
    .setAppName(ToFhirConfig.appName)
    .setMaster(ToFhirConfig.sparkMaster)
    .set("spark.driver.allowMultipleContexts", "false")
    .set("spark.ui.enabled", "false")
  val sparkSession: SparkSession = SparkSession.builder().config(sparkConf).getOrCreate()
}
