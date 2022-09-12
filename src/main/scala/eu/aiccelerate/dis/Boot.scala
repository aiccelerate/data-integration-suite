package eu.aiccelerate.dis

import io.onfhir.tofhir.ToFhirEngine
import io.onfhir.tofhir.cli.CommandLineInterface
import io.onfhir.tofhir.config.ToFhirConfig

/**
 * Entrypoint of toFHIR
 */
object Boot extends App {
  val toFhirEngine = new ToFhirEngine(ToFhirConfig.sparkAppName, ToFhirConfig.sparkMaster,
    ToFhirConfig.mappingRepositoryFolderPath, ToFhirConfig.schemaRepositoryFolderPath)

  CommandLineInterface.start(toFhirEngine, ToFhirConfig.mappingJobFilePath)
}
