package io.onfhir.tofhir.model

import org.json4s.JsonAST.{JString, JValue}

import java.net.URI

/**
 * Interface for data source settings/configurations
 */
trait DataSourceSettings {
  /**
   * Human friendly name for the source organization for data source
   */
  val name: String

  /**
   * Computer friendly canonical url indicating the source of the data (May be used for Resource.meta.source)
   */
  val sourceUri: String

  /**
   * Return the context params that will be supplied to mapping tasks
   *
   * @return
   */
  def getContextParams: Map[String, JValue] = Map.empty

  def toConfigurationContext: (String, ConfigurationContext) =
    "sourceSystem" -> ConfigurationContext(Map("name" -> JString(name), "sourceUri" -> JString(sourceUri)) ++ getContextParams)
}

/**
 *
 * @param name          Human friendly name for the source organization for data source
 * @param sourceUri     Computer friendly canonical url indicating the source of the data (May be used for Resource.meta.source)
 * @param dataFolderUri Path to the folder all source data is located
 */
case class FileSystemSourceSettings(name: String, sourceUri: String, dataFolderUri: URI) extends DataSourceSettings

/**
 * Comman interface for sink settings
 */
trait FhirSinkSettings

/**
 * Settings for a FHIR repository to store the mapped resources
 *
 * @param fhirRepoUrl      FHIR endpoint root url
 * @param securitySettings Security settings if target API is secured
 */
case class FhirRepositorySinkSettings(fhirRepoUrl: String, securitySettings: Option[FhirRepositorySecuritySettings] = None) extends FhirSinkSettings

/**
 * Security settings for FHIR API access
 *
 * @param clientId                   OpenID Client identifier assigned to toFhir
 * @param clientSecret               OpenID Client secret given to toFhir
 * @param requiredScopes             List of required scores to write the resources
 * @param authzServerTokenEndpoint   Authorization servers token endpoint
 * @param clientAuthenticationMethod Client authentication method
 */
case class FhirRepositorySecuritySettings(clientId: String,
                                          clientSecret: String,
                                          requiredScopes: Seq[String],
                                          authzServerTokenEndpoint: String,
                                          clientAuthenticationMethod: String = "client_secret_basic")


/**
 * Any mapping task instance
 */

/**
 * FHIR Mapping task instance
 *
 * @param mappingRef Canonical URL of the FhirMapping definition to execute
 */
case class FhirMappingTask(mappingRef: String, sourceContext: Map[String, FhirMappingSourceContext])

/**
 * Interface for source contexts
 */
trait FhirMappingSourceContext extends Serializable {
  val settings: DataSourceSettings
}

/**
 * Context/configuration for one of the source of the mapping that will read the source data from file system
 *
 * @param path       File path to the source file
 * @param sourceType Source format for the file See[SourceFileFormats]
 */
case class FileSystemSource(path: String, sourceType: String, override val settings: FileSystemSourceSettings) extends FhirMappingSourceContext

/**
 * List of source file formats supported by tofhir
 */
object SourceFileFormats {
  final val CSV = "csv"
  final val PARQUET = "parquet"
  final val JSON = "json"
  final val AVRO = "avro"
}

case class FhirMappingJob(id: String, mappingRepositoryUri: URI, schemaRepositoryUri: URI, sinkSettings: FhirSinkSettings, tasks: Seq[FhirMappingTask])
