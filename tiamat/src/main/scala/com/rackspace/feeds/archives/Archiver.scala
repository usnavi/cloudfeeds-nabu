package com.rackspace.feeds.archives

import java.sql.Timestamp
import java.util.UUID

import org.apache.hadoop.fs._
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.sql._
import org.apache.spark._
import org.apache.spark.SparkContext._
import org.joda.time.{DateTimeZone, DateTime}

import org.slf4j.LoggerFactory

import scala.collection.{JavaConversions, JavaConverters}

import Errors._


/**
 * Takes in the application's configurations and actually runs the archiving process.
 *
 * @param runConfig
 */
class Archiver( runConfig : RunConfig ) {

  import com.rackspace.feeds.archives.Preferences._
  import com.rackspace.feeds.archives.CreateFilesFeed._
  import com.rackspace.feeds.archives.Archiver._

  val logger = LoggerFactory.getLogger(getClass)

  val conf = runConfig.config

  //
  // map of UUIDs for feeds
  //
  val feedUuidMap = runConfig.feeds.map(
    _ -> s"urn:uuid:${UUID.randomUUID().toString}"
  ).toMap

  //
  // map of URIs for the live feeds
  //
  val liveUriMap = {

    import JavaConverters._

    conf.getObject("tiamat.feeds.liveUri").keySet().asScala.map(k =>

      k -> conf.getString( s"tiamat.feeds.liveUri.${k}" )
    ).toMap
  }

  //
  // Identity interfaces
  //
  val identity = new Identity(conf.getString("tiamat.identity.endpoint"),
    conf.getString("tiamat.identity.user"),
    conf.getString("tiamat.identity.apiKey"),
    conf.getString("tiamat.identity.password"))

  lazy val token = identity.getToken()

  //
  // Spark interfaces
  //
  lazy val sparkConf = new SparkConf().setAppName( this.getClass.getCanonicalName )
                              .set( "spark.eventLog.enabled", "true" )

  lazy val spark = new SparkContext(sparkConf)
  lazy val hive = new HiveContext(spark)

  //
  // map of tenant preferences
  // map of impersonation tokens
  // list of errors associated with accessing identity & preferences
  //
  lazy val (prefMap, impMap, errorsImp) = makePrefs()

  //
  // Get Tenant Id for the given Nast Id
  //
  def getTenantIdForNastId = makeNastIdToTenantMap( prefMap, runConfig.getNastFeeds() )

  /**
   * Run & archive
   *
   * @return errors
   */
  def run() : Iterable[TiamatError] = {

    if (!runConfig.skipSuccessFileCheck) {
      validateSuccessFilePaths()
    }
    
    val grouped = indexEntries()

    val writer = new ArchiverHelper( prefMap, impMap, getTenantIdForNastId, feedUuidMap, liveUriMap )

    val (writtenSet, errorsWrite) = writer.writeFeedsWithEntries( grouped )

    val errorsWriteEmpty = writer.writeFeedsWithoutEntries( writtenSet, runConfig, spark )

    errorsImp ++ errorsWrite ++ errorsWriteEmpty
  }

  /**
   * Validate that success file paths exist. If they dont throws a generic
   * an exception
   * * 
   */
  def validateSuccessFilePaths(): Unit = {

    val hadoopConf = spark.hadoopConfiguration
    val fs = FileSystem.get(hadoopConf)

    val successFilePathSet = getSuccessFilePaths
    
    val nonExistentSuccessFiles = successFilePathSet.filter(successFile => !fs.exists(new Path(successFile)))
    
    if (nonExistentSuccessFiles.size > 0) {
      
      val errorMessage: String = MISSING_SUCCESS_FILES(nonExistentSuccessFiles.mkString(","))
      logger.error(errorMessage)
      
      throw new RuntimeException(errorMessage)
    }
  }

  /**
   * Based on the regions and the runDates, construct the success file paths
   * that need to be verified
   *  
   * @return a sequence of success file paths.
   */
  def getSuccessFilePaths: Set[String] = {
    import JavaConversions._

    val successFilePathTemplatesSet = conf.getStringList("tiamat.success.file.paths").toSet

    (for {
      region <- runConfig.regions
      date <- runConfig.dates
    } yield successFilePathTemplatesSet.map( filePathTemplate => {
        filePathTemplate.replace("#REGION#", region)
                        .replace("#RUNDATE#", dayFormat.print(date))
      })).flatten.toSet
  }
  
  /**
   * Make
   * <ul>
   *   <li>preferences map from the preferences table.
   *   <li>map of tenant to impersonation token
   *   <li>any errors dealing with identity
   * </ul>
   *
   * @return
   */
  def makePrefs() : (Map[String, TenantPrefs], Map[String, String], Iterable[TiamatError])= {

    // tenant-related maps
    val prefRdd = tenantPrefs( hive, runConfig )

    val (impMap, errorsImp) = impersonationMap( token, prefRdd, identity )

    // FYI: filterKeys returns unserializable Map!
    // https://issues.scala-lang.org/browse/SI-6654
    val prefMap = prefRdd.filter( i => impMap.keySet.contains( i._1 ) ).collect.toMap

    (prefMap, impMap, errorsImp)
  }

  /**
   * Get the events, filter, process into JSON (if necessary) and return indexed by tenant-feed-date-archive-file format.
   *
   * @return
   */
  def indexEntries(): RDD[(ArchiveKey, Iterable[AtomEntry])] = {
    // group entries by tenant,feed,region,date
    val grouped = getEntries(runConfig, hive)
      .map( toEntry )
      .filter(viewableForArchivers(prefMap.values, runConfig.feeds.toSet, runConfig.regions.toSet))
      .flatMap(index(prefMap))
      .map(processJson)
      .groupByKey()
    grouped
  }

  def getEntries(runConfig: RunConfig, hive: HiveContext): SchemaRDD = {
    val dateWhere = runConfig.dates.map(d => s"date = '${dayFormat.print(d)}'").mkString(" OR ")
    hive.sql( s"""select tenantid, region, feed, entrybody, datelastupdated, id, categories, date from entries where ${dateWhere}""")
  }
}

/**
 * Due to spark serialization issues, I've encapsulated the following methods into they're own class.  The serialization issues are:
 * <ul>
 *   <li>Spark serializes the function being run on the cluster to send to the data nodes
 *   <li>When an anonymous function is serialized, all fields of the encompassing class are included
 *   <li>spark & hive contexts are not serializable
 * </ul>
 *
 * I'm sure there is a way to execute this code without a helper class, but I was banging my head on it and figured
 * we can refactor this later.
 *
 * @param prefMap
 * @param impMap
 * @param getTenantIdForNastId
 * @param feedUuidMap
 * @param liveUriMap
 */
class ArchiverHelper( prefMap : Map[String, TenantPrefs],
                      impMap : Map[String, String],
                      getTenantIdForNastId : (String, String) => String,
                      feedUuidMap : Map[String, String], liveUriMap : Map[String, String ]) extends Serializable {

  /**
   * Write out empty feeds for any dc-tenant-feed-date-regions-file format combinations which didn't have any events.
   *
   * @param writtenSet - feeds that have already been written because they have events
   * @return any errors.
   */
  def writeFeedsWithoutEntries(writtenSet: Set[ArchiveKey], runConfig : RunConfig, spark : SparkContext ): Array[TiamatError] = {

    import CreateFilesFeed._

    // write out empty feeds
    val expectedFiles = (for {t <- prefMap.keySet;
                              f <- runConfig.feeds;
                              d <- prefMap(t).containers.keySet;
                              format <- prefMap(t).formats;
                              date <- runConfig.dates}
    yield ArchiveKey(t, d, f, dayFormat.print(date), format)).toSet


    val errorsWriteEmpty = spark.parallelize(expectedFiles.diff(writtenSet).toSeq).map {
      {
        // calling .get blindly on an option is risky, but the region's are already filted by the keys in the map
        // so there will be a value
        key => writeFile(key, Array[AtomEntry](), prefMap, impMap, getTenantIdForNastId, feedUuidMap(key.feed), liveUriMap.get(key.region).get)
      }
    }
      .flatMap {
      _ match {
        case Some(e) => List(e)
        case _ => List()
      }
    }.collect()
    errorsWriteEmpty
  }

  def writeFeedsWithEntries( grouped : RDD[(ArchiveKey, Iterable[AtomEntry])] ) : (Set[ArchiveKey], Array[TiamatError]) = {

    val output = grouped.map { case (key, value) =>

      // calling .get blindly on an option is risky, but the region's are already filtered by the keys in the map
      // so there will be a value
      (key, writeFile(key, value, prefMap, impMap, getTenantIdForNastId, feedUuidMap(key.feed), liveUriMap.get(key.region).get))
    }

    val errorsWrite = output.flatMap(
      _._2 match {

        case Some(e) => List(e)
        case _ => List()
      }).collect

    (output.map(_._1).collect.toSet, errorsWrite)
  }

  /**
   * Given the parameters, do the following:
   * <ul>
   *   <li> ensure container exists and create if necessary
   *   <li> create feed & write to container
   * </ul>
   *
   * @param key
   * @param content
   * @param prefMap
   * @param impMap
   * @param getFeedId
   * @param feedUuid
   * @param liveFeed
   * @return
   */
  def writeFile( key : ArchiveKey,
                 content: Iterable[AtomEntry],
                 prefMap: Map[String, TenantPrefs],
                 impMap: Map[String, String],
                 getFeedId : (String, String) => String,
                 feedUuid : String,
                 liveFeed : String ) : Option[TiamatError] = {

    import com.rackspace.feeds.archives.CreateFilesFeed._

    val tid = key.tenantid

    val container = prefMap( tid ).containers(key.region)

    try {
      containerCheck(container, impMap(tid))
      createFeed(container, key, content, impMap(tid), getFeedId, feedUuid, liveFeed)
      None
    }
    catch {

      case e : RestException => Some( TiamatError( key, e ) )
      case th : Throwable => Some( TiamatError( key, th, NO_CLOUD_FILES ) )
    }
  }
}


/**
 * We keep some methods as plain functions for 2 reasons:
 * <ul>
 *   <li>functions don't have serialization problems with spark like methods on objects potentially do.
 *   <li>we can easily write unit tests for them without working about the complete spark infrastructure.
 * <ul>
 */
object Archiver {

  import com.rackspace.feeds.archives.XmlProcessing._
  import com.rackspace.feeds.archives.CreateFilesFeed._


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
   * @param regionSet
   * @param entry
   * @return
   */
  def viewableForArchivers( ids : Iterable[TenantPrefs], feedSet : Set[String], regionSet : Set[String] )( entry : Entry ) : Boolean = {

    ids.flatMap(a => List(a.tenantId, a.alternateId)).toSet
      .contains( entry.tenantid ) &&
      feedSet.contains( entry.feed ) &&
      regionSet.contains( entry.region ) &&
      !entry.categories.split( "|" ).contains( "cloudfeeds:private" )
  }

  def toEntry( row : Row ) : Entry = {

    Entry(  getStringValue( row, 0 ),
      getStringValue( row, 1 ),
      getStringValue( row, 2 ),
      getStringValue( row, 3 ),
      row( 4 ).asInstanceOf[Timestamp],
      row.getLong( 5 ),
      getStringValue( row, 6 ),
      getStringValue( row, 7 )
    )
  }

  /**
   * TODO:  This was added to prevent tiamat from breaking when any of the string values are null.  I added this when
   * we were getting ready for our demo, and something a bit more intelligent needs to happen.
   *
   * @param row
   * @param i
   * @return
   */
  def getStringValue( row: Row, i : Int ) : String = {

    if( row.isNullAt( i ) )
      ""
    else
      row.getString( i )
  }

  /**
   * Make NastId -> tenant Id map.
   *
   * @param nastFeed
   * @return
   */
  def makeNastIdToTenantMap( prefMap : Map[String, TenantPrefs], nastFeed : Set[String] ) : ( String, String ) => String = {

    val idToNastyMap = prefMap.map(a => a._1 -> a._2.alternateId )

    ( tid : String, feed : String ) => {

      nastFeed.contains( feed ) match {

        case true => idToNastyMap( tid )
        case false => tid
      }
    }
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
  def index(prefMap: Map[String, TenantPrefs]) : (Entry) => List[(ArchiveKey, AtomEntry)] = {

    val nastyMap = prefMap.map(a => a._2.alternateId -> a._1)

    entry => {

      val entrybody = privateAttrs( entry.entrybody )

      val id = entry.tenantid

      // map alternateId to tid
      val tenantid = nastyMap.contains(id) match {
        case true => nastyMap(id)
        case _ => id
      }

      prefMap( tenantid ).formats.flatMap( f =>
        List(( ArchiveKey( tenantid, entry.region, entry.feed, entry.date, f ),
          AtomEntry( entrybody, new DateTime( entry.datelastupdated.getTime, DateTimeZone.UTC ), entry.id))))
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
}
