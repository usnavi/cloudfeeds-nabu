package com.rackspace.feeds.archives

import java.sql.Timestamp

import org.joda.time.{DateTimeZone, DateTimeComparator, DateTime}
import org.joda.time.format._
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters

/**
 * spark-submit  --class com.rackspace.feeds.archives.Tiamat --master yarn-cluster --num-executors 3
 * --executor-memory 512m
 * --jars apps/spark-1.1.1-bin-hadoop2.4/lib/datanucleus-api-jdo-3.2.1.jar,apps/spark-1.1.1-bin-hadoop2.4/lib/datanucleus-core-3.2.2.jar,apps/spark-1.1.1-bin-hadoop2.4/lib/datanucleus-rdbms-3.2.1.jar,/home/rona6028/apps/spark-1.1.1-bin-hadoop2.4/conf/hive-site.xml
 * /opt/cloudfeeds-nabu/tiamat/lib/cloudfeeds-nabu-tiamat-1.0.0-SNAPSHOT-all.jar  -c /home/rona6028/temp-staging.conf -d 2015-01-27
 *
 *
 * Archives atom events from the Cloud Feeds Hadoop cluster into Cloud Files on a per-tenant basis.
 *
 * Usage: com.rackspace.feeds.archives.Tiamat [options]
 *
 * -c <value> | --config <value>
 *       Config file path, default to /etc/cloudfeeds-tiamat/conf/tiamat.conf
 * -f <value> | --feeds <value>
 *       List feed names (comma-separated).  Default is to archive all archivable feeds.
 * -d <value> | --dates <value>
 *       List of dates in the format of yyyy-MM-dd (comma-separated).  Default is yesterday's date.
 * -t <value> | --tenants <value>
 *       List of tenant IDs (comma-separated).  Default is all archiving-enabled tenants.
 * -r <value> | --regions <value>
 *       List of regions (common-separated).  Default is all regions.
 *
 * --help
 *       Show this.
 *
 * If the contents of /etc/cloudfeeds-nabu/tiamat/logback.xml are modified, they need to be loaded
 * at runtime.
 *   -Dlogback.configurationFile=<path to logback.xml>
 *
 * Process:
 *
 * - Get all events for requested days
 * - Extract only events for requested archived feeds & tenants
 * - Filter out cloudfeeds:private events
 * - Map NastId events to tenant ids
 * - Run private attributes XSLT on each event
 * - Group events by tenantid-dc-feed-format
 * - Convert events into JSON (if required) and format for including in JSON array
 * - Sort events for tenantid-dc-feed-format and write out to cloud files
 *   (Nast Id feeds are archived under the tenant's cloud files as well)
 *
 *  Notes:
 *
 * - pulls preferences data from the "preferences" hive datastore
 * - pulls events from the "entries" hive datastore
 * - To build execute gradle tiamat:shadowJar
 *
 */

object Tiamat {

  import com.rackspace.feeds.archives.RunConfig._

  val logger = LoggerFactory.getLogger(getClass)

  val isoFormat = ISODateTimeFormat.basicDateTime()

  /**
   * The method run by Spark to execute the job.
   *
   * @param args
   */
  def main( args: Array[String] ): Unit = {

    val options = new Options()

    val runConfig = options.parseOptions( args)
    logger.debug( runConfig )

    val archiver = new Archiver( runConfig )
    val errors = archiver.run()

    processErrors( errors )
  }

  def processErrors(errors: Iterable[TiamatError]) {
    if (!errors.isEmpty) {

      errors.foreach(e => {

        import ArchiveKey._

        logger.error(s"ERROR: ${archiveKeyToString(e.archiveKey)}: ${e.throwable.getMessage}", e.throwable)
      }
      )
      throw new Exception("Encountered errors. See log for details.")
    }
  }
}


object Joda {
  implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)
}

case class RestException( code : Int, message : String ) extends Throwable( s"""$code: $message""" )

case class TiamatError( archiveKey : ArchiveKey, throwable : Throwable )

case class Entry ( tenantid : String,
                   region : String,
                   feed : String,
                   entrybody : String,
                   datelastupdated : Timestamp,
                   id : Long,
                   categories : String,
                   date : String )

case class ArchiveKey( tenantid : String,
                       region : String = "",
                       feed : String = "",
                       date : String = "",
                       format : String = "" )

object ArchiveKey {

  def archiveKeyToString( a : ArchiveKey ) : String = {

    s"tenantid: ${a.tenantid}, dc: ${a.region}, feed: ${a.feed}, date: ${a.date}, format: ${a.format}"
  }
}


// TODO: looks like Timestamp was fixed in 1.2
case class AtomEntry( entrybody : String, /*datelastupdated : DateTime, */ id : Long ) extends Ordered[AtomEntry] {

  import scala.math.Ordered.orderingToOrdered

  def compare(that: AtomEntry): Int = {


    id.compare( that.id )
    /*
    import Joda._

    datelastupdated.compare( that.datelastupdated ) match {

      case 0 => id.compare( that.id )
      case c => c
    }

    */
  }
}
