package com.rackspace.feeds.archives

import org.apache.commons.lang3.StringUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.apache.spark.sql.hive.HiveContext
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.map.ObjectMapper
import scala.util.{Success, Failure, Try}

case class TenantPrefs( tenantId : String, alternateId : String, containers : Map[ String, String ], formats : List[String] )

/**
 * Groups methods related with creating the TenantPrefs map based on the data from the Preferences db.
 */
object Preferences {

  /**
   * Create map of tenant id -> TenantPreferences from the preferences database.
   *
   * @param hive
   * @param runConfig
   * @return
   */
  def tenantPrefs(hive: HiveContext, runConfig : RunConfig ): RDD[(String, TenantPrefs)] = {

    val tids = runConfig.tenantIds.toSet
    val regions = runConfig.regions

    hive.sql("select id, payload, alternate_id, created, updated, enabled from preferences")
      .filter( (row) => tids.isEmpty match {
      case true => true
      case false => tids.contains(row.getString(0))
    }).flatMap(tenantContainers( regions, _ ))
  }

  /**
   * Create map entry of tenant id -> TenantPreferences
   *
   * @param regions
   * @param row
   * @return
   */
  def tenantContainers(regions : Seq[String], row: Row): List[(String, TenantPrefs)] = {

    import scala.collection.JavaConversions._

    val tenantId = row.getString(0)
    val prefs = (new ObjectMapper).readTree(row.getString(1))
    val formatsNode = prefs.get("data_format").getElements
    val formats = List() ++ formatsNode.map(_.getTextValue)
    val urls = prefs.get("archive_container_urls")
    val enabled = row.getBoolean(5)

    enabled match {

      case false => List()
      case true => {

        val containers = if ( isNotBlank( prefs.get( "default_archive_container_url" ) ) ) {

          val default_con = prefs.get("default_archive_container_url").getTextValue

          regions.map(dc =>

            if (urls == null)
              (dc -> default_con)
            else {

              isNotBlank( urls.get(dc) ) match {
                case true => (dc -> urls.get(dc).getTextValue)
                case _ => (dc -> default_con)
              }
            }
          ).toMap[String, String]
        }
        else if (urls == null)
          Map[String, String]()
        else {

          urls.getFields
            .filter( entry => regions.contains( entry.getKey ) )
            .flatMap( entry =>

            isNotBlank( entry.getValue ) match {

              case true  => List[(String, String)]( (entry.getKey, entry.getValue.getTextValue) )
              case false => List[(String, String)]()
            }
          )
        }.toMap[String, String]

        List((tenantId, TenantPrefs(tenantId, row.getString(2), containers, formats)))
      }
    }
  }

  def isNotBlank( node : JsonNode ) : Boolean = {

    node != null && StringUtils.isNoneBlank( node.getTextValue )
  }

  /**
   * Create map of tenant id -> impersonation token for that tenant's admin account.
   *
   * Returns tuple of successful impersonation tokens & list of errors of the rest of impersonation token retrievall attempts.
   *
   * @param token
   * @param tenants
   * @param identity
   * @return
   */
  def impersonationMap(token: String,
                       tenants: RDD[(String, TenantPrefs)],
                       identity : Identity ): (Map[String, String], Iterable[TiamatError]) = {

    val output = tenants.map( a => {

      val imp = Try( {
        val user = identity.getTenantAdmin( a._1 )
        identity.impersonate(user, token)
      })

      (a._1 -> imp)
    })

    val impMap = output.flatMap( a => {

      a._2 match {
        case Success( token ) => List( a._1 -> token)
        case _ => List()
      }
    }).collect.toMap

    val errors = output.flatMap( a => {

      a._2 match {
        case Failure( e ) => List( TiamatError( ArchiveKey( a._1 ), e ) )
        case _ => List()
      }
    }).collect

    (impMap, errors)
  }
}
