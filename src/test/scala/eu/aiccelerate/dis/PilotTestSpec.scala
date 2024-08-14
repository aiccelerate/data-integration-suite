package eu.aiccelerate.dis


import io.tofhir.engine.config.ToFhirConfig
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should
import org.scalatest.{Inside, Inspectors, OptionValues}

class PilotTestSpec extends AsyncFlatSpec with should.Matchers with
  OptionValues with Inside with Inspectors {

  val sparkSession: SparkSession = ToFhirConfig.sparkSession
}
