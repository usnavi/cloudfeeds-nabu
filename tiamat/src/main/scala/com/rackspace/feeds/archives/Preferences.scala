package com.rackspace.feeds.archives

import org.apache.spark.sql.Row
import org.apache.spark.sql.hive.HiveContext
import org.codehaus.jackson.map.ObjectMapper

case class TenantPrefs( tenantId : String, alternateId : String, containers : Map[ String, String ], formats : List[String] )

/**
 * Groups methods related with creating the TenantPrefs map based on the data from the Preferences db.
 *
 */
object Preferences {

  import com.rackspace.feeds.archives.Identity._

  //  val DC = Array( "DFW", "IAD", "HKG", "LON", "ORD", "SYD" )

  val DC = Array( "ORD", "SYD" )

  def tenantPrefs(hive: HiveContext): Map[String, TenantPrefs] = {
    hive.sql("select * from preferences")
      .collect.map(tenantContainers).toMap
  }


  def tenantContainers(row: Row): (String, TenantPrefs) = {

    import scala.collection.JavaConversions._

    val tenantId = row.getString(0)
    val prefs = (new ObjectMapper).readTree(row.getString(3))
    val default_con = prefs.get("default_container_name").getTextValue
    val formatsNode = prefs.get("data_format").getElements
    val formats = List() ++ formatsNode.map( _.getTextValue )
    val urls = prefs.get("archive_container_urls")

    val containers = DC.map(dc =>

      urls.has(dc) match {
        case true => (dc -> urls.get(dc).getTextValue)
        case _ => (dc -> default_con)
      }
    ).toMap[String, String]

    (tenantId -> TenantPrefs(tenantId, row.getString(1), containers, formats))
  }

  def impersonationMap(token: String, prefMap: Map[String, TenantPrefs], admin : String, pw : String ): Map[String, String] = {

    prefMap.map(a => {

      val user = getTenantAdmin(a._1, admin, pw)
      val imp = impersonate(user, token)

      (a._1 -> imp)
    }
    ).toMap
  }
}
