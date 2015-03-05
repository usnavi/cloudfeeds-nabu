package com.rackspace.feeds.archives

import java.io._
import java.util.UUID

import javax.xml.transform
import javax.xml.transform._
import javax.xml.transform.stream._

import net.sf.saxon.Controller
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.sql._
import org.apache.spark._
import org.codehaus.jackson.map.ObjectMapper
import org.apache.spark.SparkContext._
import org.codehaus.jackson.node.ObjectNode
import org.joda.time.{format, DateTimeZone, DateTimeComparator, DateTime}
import org.joda.time.format._
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters
import scala.xml._
import scala.xml.transform._

object Joda {
  implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)
}

case class RestException( code : Int, message : String ) extends Throwable( s"""$code: $message""" )

case class TiamatError( archiveKey : ArchiveKey, throwable : Throwable )


// TODO:  enum for formats??
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

/**
 * Archives atom events from the Cloud Feeds Hadoop cluster into Cloud Files on a per-tenant basis.
 *
 * Usage: com.rackspace.feeds.archives.Tiamat [options]
 *
 * -c <value> | --config <value>
 *       Config file path, default to /etc/cloudfeeds-tiamat/conf/tiamat.conf
 * -f <value> | --feeds <value>
 *       List feed names (comma-separated).  Default is to archive all archivable feeds.
 * -d <value> | --dates <value>
 *       List of dates in the format of YYYY-MM-DD (comma-separated).  Default is yesterday's date.
 * -t <value> | --tenants <value>
 *       List of tenant IDs (comma-separated).  Default is all archiving-enabled tenants.
 * -r <value> | --regions <value>
 *
 * --help
 *       Show this.
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
 * - pulls events from the "import" hive datastore
 * - To build execute gradle tiamat:shadowJar
 *
 *
 *  TODO:  (will add these to Jira)
 *
 * - check for preferences success file
 * - can we define nast id feeds from wadl?
 * - packaging
 * - pool transformers/singleton transformer?  broadcast variable for this?
 * - update json xslt to support xmlns:fh namespace
 */

object Tiamat {

  import com.rackspace.feeds.archives.Preferences._
  import com.rackspace.feeds.archives.CreateFilesFeed._
  import com.rackspace.feeds.archives.RunConfig._


  val FEED = Set( "feed_1/events" )
  val FEED_NAST = Set( "nasty_1/events" )

  /*
  val FEED = Array( "backup/events", "bigdata/events", "cbs/events", "dbaas/events", "dns/events",
    "glance/events", "identity/events", "lbaas/events", "monitoring/events", "nova/events", "queues/events",
    "servers/events", "ssl/events", "usagesummary/backup/events", "usagesummary/bigdata/events",
    "usagesummary/cbs/events", "usagesummary/dbaas/events", "dns/events",
    "usagesummary/glance/events", "usagesummary/identity/events", "usagesummary/lbaas/events",
    "usagesummary/monitoring/events", "usagesummary/nova/events", "usagesummary/queues/events",
    "usagesummary/servers/events", "usagesummary/ssl/events" )
val FEED_NAST = Set( "files/events", "usagesummary/files/events" )
*/

  val logger = LoggerFactory.getLogger(getClass)

  val isoFormat = ISODateTimeFormat.basicDateTime()

  def main( args: Array[String] ): Unit = {

    val feedSet = FEED ++ FEED_NAST

    val options = new Options()

    val runConfig = options.parseOptions(feedSet, args)

    val conf = options.getConf(runConfig)

    val feedUuidMap = runConfig.feeds.map(
      _ -> s"urn:uuid:${UUID.randomUUID().toString}"
    ).toMap

    val identity = new Identity(conf.getString("tiamat.identity.endpoint"),
      conf.getString("tiamat.identity.user"),
      conf.getString("tiamat.identity.apiKey"),
      conf.getString("tiamat.identity.password"))

    val liveUri = conf.getObject("tiamat.feeds.liveUri")

    val liveUriMap = {

      import JavaConverters._

      liveUri.keySet().asScala.map(k =>

        k -> conf.getString( s"tiamat.feeds.liveUri.${k}" )
      ).toMap
    }

    val token = identity.getToken()

    logger.debug( runConfig )

    // initialize spark & hive interface
    val sparkConf = new SparkConf().setAppName( this.getClass.getCanonicalName )

    // TODO:  set this in default conf in spark
    sparkConf.set( "spark.eventLog.enabled", "true" )
    val spark = new SparkContext(sparkConf)
    val hive = new HiveContext(spark)

    // tenant-related maps
    val prefMap1 = tenantPrefs( hive, runConfig )

    val (impMap, errorsImp) = impersonationMap( token, prefMap1, identity )

    // FYI: filterKeys returns unserializable Map!
    // https://issues.scala-lang.org/browse/SI-6654
    val prefMap = prefMap1.filter( i => impMap.keySet.contains( i._1 ) )

    val grouped = indexEntries(runConfig, hive, prefMap)

    def getFeedId = makeGetFeedId( prefMap, FEED_NAST )

    // write out tenant-feed-dc combos, return list of written files
    val output = grouped.map { case (key, value) =>

      // calling .get blindly on an option is risky, but the region's are already filtered by the keys in the map
      // so there will be a value
      println( "region: " + liveUriMap.get( key.region ).get )

      (key, writeFile(key, value, prefMap, impMap, getFeedId, feedUuidMap( key.feed ), liveUriMap.get( key.region ).get))
    }

    val errorsWrite = output.flatMap(
      _._2 match {

        case Some( e ) => List(e)
        case _ => List()
      }).collect

    val writtenSet = output.map( _._1 ).collect.toSet

    // write out empty files
    val expectedFiles = (for {t <- prefMap.keySet;
                              f <- runConfig.feeds;
                              d <- prefMap( t ).containers.keySet;
                              format <- prefMap( t ).formats;
                              date <- runConfig.dates}
    yield ArchiveKey(t, d, f, dayFormat.print( date), format)).toSet

    val errorsWriteEmpty = spark.parallelize(expectedFiles.diff(writtenSet).toSeq).map {
      {
        // calling .get blindly on an option is risky, but the region's are already filted by the keys in the map
        // so there will be a value
        key => writeFile( key, Array[AtomEntry](), prefMap, impMap, getFeedId, feedUuidMap( key.feed ), liveUriMap.get( key.region ).get )
      }
    }
      .flatMap {
      _ match {
        case Some(e) => List(e)
        case _ => List()
      }
    }.collect()

    val errors = errorsImp ++ errorsWrite ++ errorsWriteEmpty

    if( !errors.isEmpty ) {

      errors.foreach( e => {

        import ArchiveKey._

        logger.error(s"ERROR: ${archiveKeyToString( e.archiveKey ) }: ${e.throwable.getMessage}", e.throwable )
      }
      )
      throw new Exception( "ERRORS!" )
    }
  }

  def indexEntries(runConfig: RunConfig, hive: HiveContext, prefMap: Map[String, TenantPrefs]): RDD[(ArchiveKey, Iterable[AtomEntry])] = {
    // group entries by tenant,feed,region,date
    val grouped = getEntries(runConfig, hive)
      .filter(viewableForArchivers(prefMap.values, runConfig.feeds.toSet, runConfig.regions.toSet))
      .flatMap(index(prefMap))
      .map(processJson)
      .groupByKey()
    grouped
  }

  def getEntries(runConfig: RunConfig, hive: HiveContext): SchemaRDD = {
    val dateWhere = runConfig.dates.map(d => s"date = '${dayFormat.print(d)}'").mkString(" OR ")
    hive.sql( s"""select tenantid, dc, feed, entrybody, datelastupdated, id, categories, date from import where ${dateWhere}""")
  }

  /**
   *
   * If being stored as JSON, convert XML entry into JSON ready to be added to JSON array.
   *
   * @param pair
   * @return
   */
  def processJson( pair : (ArchiveKey, AtomEntry) ) : (ArchiveKey, AtomEntry) = {

    val (key, entry) = pair

    key.format match {

      case JSON_KEY => (key, makeJson( entry ))
      case XML_KEY => pair
    }
  }

  /**
   *
   * Convert XML entry into JSON ready to be added to JSON array.
   *
   * @param entry
   * @return
   */
  def makeJson( entry : AtomEntry ) : AtomEntry = {

    val source = new StreamSource( new StringReader( entry.entrybody ) )
    val out = new ByteArrayOutputStream()
    val result = new StreamResult( out )

    val factory = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null)
    val  resolver = new URIResolver() {

      override def resolve(href: String, base: String): transform.Source = {

        // assume xslt within classpath
        new StreamSource( getClass.getResourceAsStream( s"/$href" ) )
      }
    }
    factory.setURIResolver( resolver )

    val xsltJson = new StreamSource( getClass.getResourceAsStream( "/xml2json-feeds.xsl" ) )
    val transformer = factory.newTransformer( xsltJson )
    (transformer.asInstanceOf[Controller]).setInitialTemplate( "main" )
    transformer.transform( source, result )

    AtomEntry(  makeArrayEntry( out.toString( "UTF-8" ) ), entry.id )
  }

  def makeArrayEntry(json: String): String = {

    val mapper = new ObjectMapper()

    val map = mapper.readTree( json )

    val entryMap = map.get("entry").asInstanceOf[ObjectNode]
    entryMap.remove("@type")

    val writer = new StringWriter()

    mapper.writeValue( writer , entryMap )

    writer.toString
  }

  /**
   * Filter entries:
   * <ul>
   *   <li> only for archived tenants
   *   <li> only for requested feeds
   *   <li> only for requested regions
   *   <li> no private entries
   * </ul>
   *
   * @param ids
   * @param feedSet
   * @param row
   * @return
   */
  def viewableForArchivers( ids : Iterable[TenantPrefs], feedSet : Set[String], regionSet : Set[String] )( row : Row ) : Boolean = {

    ids.flatMap(a => List(a.tenantId, a.alternateId)).toSet
      .contains(row.getString(0)) &&
      feedSet.contains(row.getString(2)) &&
      regionSet.contains( row.getString(1) ) &&
      !row.getString( 6 ).split( "|" ).contains( "cloudfeeds:private" )
  }

  /**
   * Perform the following:
   *
   * <ul>
   *   <li> filter private attributes from entry
   *   <li> map NastIDs to tenant Ids
   *   <li> create ArchiveKey
   * </ul>
   *
   * @param prefMap
   * @return
   */
  def index(prefMap: Map[String, TenantPrefs]) : (Row) => List[(ArchiveKey, AtomEntry)] = {

    val nastyMap = prefMap.map(a => a._2.alternateId -> a._1)

    row => {

     val entrybody = privateAttrs( row.getString(3) )

      val id = row.getString(0)

      // map alternateId to tid
      val tenantid = nastyMap.contains(id) match {
        case true => nastyMap(id)
        case _ => id
      }

      prefMap( tenantid ).formats.flatMap( f =>
        List(( ArchiveKey( tenantid, row.getString(1), row.getString(2), row.getString( 7 ), f ),
          AtomEntry( entrybody, /*isoFormat.parseDateTime( row.getString(4)), */ row.getLong(5)))))
    }
  }

  /**
   * Filter private attributes.
   *
   */
  def privateAttrs( entrybody : String ) : String = {

    val removeLink = new RewriteRule {
      override def transform(n: Node): NodeSeq = n match {
        case e: Elem if e.label == "link"
          && e.prefix == "atom" => NodeSeq.Empty
        case n => n
      }
    }

    val body = new RuleTransformer(removeLink)
      .transform(XML.loadString(entrybody))
      .toString()

    // filter private attributes
    val source = new StreamSource(new StringReader(body))
    val writer = new StringWriter()
    val result = new StreamResult(writer)

    val xsltWrapper = new StreamSource(getClass.getResourceAsStream("/wrapper.xsl"))

    val resolver = new URIResolver() {

      override def resolve(href: String, base: String): Source = {

        // assume xslt within classpath
        new StreamSource(getClass.getResourceAsStream(s"/$href"))
      }
    }

    val factory = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null)
    factory.setURIResolver(resolver)

    val transformer = factory.newTransformer(xsltWrapper)

    transformer.transform(source, result)

    writer.toString
  }

  /**
   * Make NastId -> tenant Id map.
   *
   * @param prefMap
   * @param nastFeed
   * @return
   */
  def makeGetFeedId( prefMap : Map[ String, TenantPrefs], nastFeed : Set[String] ) : ( String, String ) => String = {

    val idToNastyMap = prefMap.map(a => a._1 -> a._2.alternateId )

    ( tid : String, feed : String ) => {

      nastFeed.contains( feed ) match {

        case true => idToNastyMap( tid )
        case false => tid
      }
    }
  }
}


