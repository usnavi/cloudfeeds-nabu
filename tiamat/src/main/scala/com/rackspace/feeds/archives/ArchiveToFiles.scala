package com.rackspace.feeds.archives

import java.io._

import javax.xml.transform
import javax.xml.transform.{Source, URIResolver, TransformerFactory}
import javax.xml.transform.stream.{StreamResult, StreamSource}

import net.sf.saxon.Controller
import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.sql._
import org.apache.spark._
import org.codehaus.jackson.map.ObjectMapper
import org.apache.spark.SparkContext._
import org.codehaus.jackson.node.ObjectNode
import org.joda.time.{DateTimeComparator, DateTime}
import org.joda.time.format._

import scala.xml.{Node, Elem, NodeSeq, XML}
import scala.xml.transform.{RuleTransformer, RewriteRule}

object Joda {
  implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)
}

case class RestException( code : Int, message : String ) extends Throwable( s"""$code: $message""" )
case class ArchiveKey( tenantid : String, dc : String, feed : String, format : String )

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
 * Some notes on Tiamat:
 *
 * - pulls preferences data from the "preferences" hive datastore
 * - currently hardcoded to pull events from 2015-01-27
 * - pulls events from the "import" hive datastore
 * - archives feeds with tenant ids & nast ids under the tenant id's cloud files
 * - archives xml, json or both based on a customer's preferences
 * - To build execute gradle tiamat:shadowJar
 *
 * Process
 * - Get all events for particular day
 * - Extract only events for archived feeds & tenants
 * - Filter out cloudfeeds:private events
 * - Map NastId events to tenant ids
 * - Run private attributes XSLT on each event
 * - Group events by tenantid-dc-feed-format
 * - Convert events into JSON (if required) and format for including in JSON array
 * - Sort events for tenantid-dc-feed-format and write out to cloud files
 *
 *
 *  TODO:  (will add these to Jira)
 *
 * - configuration
 * - add logging
 * - error handling so we don't stop processing
 * - interface for executing yesterday & custom
 * - test spark 1.2 to order by datelastupdated
 * - get xslt & wadl from standard-usage-schema
 * - load archived feeds from standard-usage-schema wadl
 * - can we define nast id feeds from wadl?
 * - make sure assumptions about what are in preferences are correct
 * - if DC is not specified (and no default) make sure that dc is not tracked
 * - unit tests
 * - packaging
 * - pool transformers
 *
 * - update json xslt to support xmlns:fh namespace
 */

object ArchiveToFiles {

  import com.rackspace.feeds.archives.Identity._
  import com.rackspace.feeds.archives.Preferences._
  import com.rackspace.feeds.archives.CreateFilesFeed._


  val admin = "XXX"
  val apiKey = "XXX"
  val pw = "XXX"

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

  val isoFormat = ISODateTimeFormat.basicDateTime()

  def main( args: Array[String] ): Unit = {

    // TODO: - if no date, get yesterday
    val date = "2015-01-27"

    // initialize spark & hive interface
    val conf = new SparkConf().setAppName("ArchiveToFiles: " + date)
    conf.set( "spark.eventLog.enabled", "true" )
    val spark = new SparkContext(conf)
    val hive = new HiveContext(spark)

    val feedSet = FEED ++ FEED_NAST

    val dateTime = dayFormat.parseDateTime( date )

    val token = getToken( admin, apiKey )

    val entries = hive.sql( s"""select tenantid, dc, feed, entrybody, datelastupdated, id, categories from import where date = '$date'""")

    // tenant-related maps
    val prefMap = tenantPrefs(hive)
    val impMap = impersonationMap( token, prefMap, admin, pw )

    // group entries by tenant,feed, dc
    val grouped = entries
      .filter( viewableForArchivers( prefMap.values, feedSet) )
      .flatMap( index( prefMap ) )
      .map( processJson )
      .groupByKey()

    def getFeedId = makeGetFeedId( prefMap, FEED_NAST )

    // write out tenant-feed-dc combos, return list of written files
    val writtenSet = grouped.map { case (key, value) =>

      writeFile(key, dateTime, value, prefMap, impMap, getFeedId)
      key
    }.collect().toSet

    // write out empty files
    // need to filter this
    val expectedFiles = (for {t <- prefMap.keySet;
                              f <- feedSet;
                              d <- prefMap( t ).containers.keySet;
                              format <- prefMap( t ).formats }
    yield ArchiveKey(t, d, f, format)).toSet

    spark.parallelize(expectedFiles.diff(writtenSet).toSeq).foreach {
      {
        key => writeFile( key, dateTime, Array[AtomEntry](), prefMap, impMap, getFeedId)
      }
    }
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
   *   <li> only for archived feeds
   *   <li> only for archived tenants
   *   <li> no private entries
   * </ul>
   *
   * @param ids
   * @param feedSet
   * @param row
   * @return
   */
  def viewableForArchivers( ids : Iterable[TenantPrefs], feedSet : Set[String] )( row : Row ) : Boolean = {

    ids.flatMap(a => List(a.tenantId, a.alternateId)).toSet
      .contains(row.getString(0)) &&
      feedSet.contains(row.getString(2)) &&
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

     val entrybody = privateAttrs( row )

      val id = row.getString(0)

      // map alternateId to tid
      val tenantid = nastyMap.contains(id) match {
        case true => nastyMap(id)
        case _ => id
      }

      prefMap( tenantid ).formats.flatMap( f =>
        List(( ArchiveKey( tenantid, row.getString(1), row.getString(2), f ), AtomEntry( row.getString(3 ),
        /*isoFormat.parseDateTime( row.getString(4)), */ row.getLong(5)))))
    }
  }

  /**
   * Filter private attributes.
   *
   * @param row
   * @return
   */
  def privateAttrs(row: Row) : String = {
    val entrybody = row.getString(3)

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


/*
object Test {

  def main( args: Array[String] ): Unit = {

    val entry = """<atom:entry index="0" xmlns:atom="http://www.w3.org/2005/Atom" xmlns="http://wadl.dev.java.net/2009/02" xmlns:db="http://docbook.org/ns/docbook" xmlns:error="http://docs.rackspace.com/core/error" xmlns:wadl="http://wadl.dev.java.net/2009/02" xmlns:json="http://json-schema.org/schema#" xmlns:saxon="http://saxon.sf.net/" xmlns:sum="http://docs.rackspace.com/core/usage/schema/summary" xmlns:d558e1="http://wadl.dev.java.net/2009/02" xmlns:cldfeeds="http://docs.rackspace.com/api/cloudfeeds"><atom:id>urn:uuid:59085a27-f9ac-44f7-a74b-0d41fe3c4585</atom:id><atom:category term="tid:5821027" /><atom:category term="rgn:DFW" /><atom:category term="dc:DFW1" /><atom:category term="rid:ed3f75f5-bd98-4c62-b670-46c7d15ea601" /><atom:category term="widget.widget.gadget.usage" /><atom:category term="type:widget.widget.gadget.usage" /><atom:content type="application/xml"><event xmlns="http://docs.rackspace.com/core/event" xmlns:widget="http://docs.rackspace.com/usage/widget" dataCenter="DFW1" endTime="2012-03-12T15:51:11Z" environment="PROD" id="59085a27-f9ac-44f7-a74b-0d41fe3c4585" region="DFW" resourceId="ed3f75f5-bd98-4c62-b670-46c7d15ea601" startTime="2012-03-12T11:51:11Z" tenantId="5821027" type="USAGE" version="1"><widget:product version="3" serviceCode="Widget" resourceType="WIDGET" label="test" widgetOnlyAttribute="bar" privateAttribute1="something you can not see" myAttribute="here it should be private" privateAttribute3="W2" mid="e9a67860-52e6-11e3-a0d1-002500a28a7a"><widget:metaData key="foo" value="bar"/><widget:mixPublicPrivateAttributes privateAttribute3="45" myAttribute="here it should be public"/></widget:product></event></atom:content><atom:link href="https://atom.test.ord1.us.ci.rackspace.net/functest1/events/entries/urn:uuid:59085a27-f9ac-44f7-a74b-0d41fe3c4585" rel="self" /><updated>2015-02-05 11:37:52.737</updated><published>2015-01-30T20:59:53.836Z</published></atom:entry>"""

    val source = new StreamSource(new StringReader( entry ))

    val writer = new StringWriter()
    val result = new StreamResult( writer )
    val xsltWrapper = new StreamSource( getClass.getResourceAsStream( "/xml2json-feeds.xsl" ) )

    val  resolver = new URIResolver() {

      override def resolve(href: String, base: String): transform.Source = {

        // assume xslt within classpath
        new StreamSource( getClass.getResourceAsStream( s"/$href" ) )
      }
    }

    val factory = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null)
    factory.setURIResolver( resolver )

    val transformer = factory.newTransformer( xsltWrapper )
    (transformer.asInstanceOf[Controller]).setInitialTemplate( "main" );

    transformer.transform( source, result )

    val json = writer.toString
    println( json )

    val mapper = new ObjectMapper()

    val map = mapper.readTree( json )

    val entryMap = map.get("entry").asInstanceOf[ObjectNode]
    entryMap.remove("@type")

    val writer2 = new StringWriter()

    mapper.writeValue( writer2 , entryMap )

    println( writer2.toString )

  }
}

*/

