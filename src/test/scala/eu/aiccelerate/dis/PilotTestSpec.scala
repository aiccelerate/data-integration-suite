package eu.aiccelerate.dis

import io.tofhir.engine.config.{ErrorHandlingType, ToFhirConfig}
import io.tofhir.engine.config.ErrorHandlingType.ErrorHandlingType
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should
import org.scalatest.{Inside, Inspectors, OptionValues}

class PilotTestSpec extends AsyncFlatSpec with should.Matchers with
  OptionValues with Inside with Inspectors {

  val mappingErrorHandling: ErrorHandlingType = ErrorHandlingType.HALT
  val fhirWriteErrorHandling: ErrorHandlingType = ErrorHandlingType.HALT

  val sparkConf: SparkConf = new SparkConf()
    .setAppName(ToFhirConfig.sparkAppName)
    .setMaster(ToFhirConfig.sparkMaster)
    .set("spark.driver.allowMultipleContexts", "false")
    .set("spark.ui.enabled", "false")
  val sparkSession: SparkSession = SparkSession.builder().config(sparkConf).getOrCreate()
}
