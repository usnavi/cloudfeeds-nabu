package com.rackspace.feeds.archives

import java.io.File

import com.typesafe.config.{ConfigFactory, Config}
import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.DateTimeFormat
import scopt.{OptionParser, Read}

import scala.collection.JavaConversions

case class RunConfig( configPath : String = Options.CONF,
                      feeds : Seq[String] = Seq[String](),
                      dates : Seq[DateTime] = Seq[DateTime](),
                      tenantIds : Seq[String] = Seq[String](),
                      regions : Seq[String] = Seq[String](),
                      config : Config = ConfigFactory.empty() ) {

  def getMossoFeeds() : Set[String] = {

    import JavaConversions._

    config.getStringList("tiamat.feeds.MossoId").toSet
  }

  def getNastFeeds() : Set[String] = {

    import JavaConversions._

    config.getStringList("tiamat.feeds.NastId").toSet
  }
}

object RunConfig {

  implicit def runConfigToString( c : RunConfig ) : String = {

    s"""RunConfig:
       |configPath: '${c.configPath}'
       |feeds: '${c.feeds}'
       |dates: '${c.dates}'
       |tenantIds: '${c.tenantIds}'
       |regions: '${c.regions}'
     """.stripMargin
  }
}

object Options {

  val CONF = "/etc/cloudfeeds-nabu/tiamat/tiamat.conf"

}

/**
 * Parses the command line options and generates a RunConfig object which encapsulates what should be
 * archived.
 *
 */
class Options {

  import Options._

  def parseOptions( args: Array[String]): RunConfig = {
    implicit val dateTimeRead: Read[DateTime] = Read.reads {

      DateTimeFormat.forPattern("yyyy-MM-dd").parseDateTime
    }

    val parser = new OptionParser[RunConfig]("com.rackspace.feeds.archives.Tiamat") {

      head("Tiamat", "0.9")

      opt[String]('c', "config") action { (x, c) =>

        c.copy(configPath = x)
      } validate { x =>

        (new File( x )).exists() match {

          case true => success
          case false => failure( s"file '${x}' does not exist")
        }
      } text ( s"Config file path, default to ${CONF}" )

      opt[Seq[String]]('f', "feeds") action { (x, c) =>

        c.copy(feeds = x)
      } text ("List feed names (comma-separated).  Default is to archive all archivable feeds.")

      opt[Seq[DateTime]]('d', "dates") action { (x, c) =>

        c.copy(dates = x)
      } text ("List of dates in the format of yyyy-MM-dd (comma-separated).  Default is yesterday's date.")

      opt[Seq[String]]('t', "tenants") action { (x, c) =>

        c.copy(tenantIds = x)
      } text ("List of tenant IDs (comma-separated).  Default is all archiving-enabled tenants.")

      opt[Seq[String]]('r', "regions") action { (x, c) =>

        c.copy( regions = x)
      } text( "List of regions (common-separated).  Default is all regions." )

      help("help") text ( "Show this." )

      note( """|
               |If the contents of /etc/cloudfeeds-nabu/tiamat/logback.xml are modified, they need to be loaded
               |at runtime.
               |
               |  -Dlogback.configurationFile=<path to logback.xml>""".stripMargin )
    }

    val rc = parser.parse(args, RunConfig()) match {

      case Some(runConfig: RunConfig) => runConfig
      case _ => throw new Exception( "Invalid ")
    }

    val rc1 = processDates(rc)

    val conf = getConf(rc1)

    val rc2 = processRegions( conf, rc1 )

    processFeeds( conf, rc2 )
  }

  def processDates(rc: RunConfig): RunConfig = {
    val rc1 = rc match {

      // add yesterday if no date given
      case r: RunConfig if r.dates.isEmpty => {

        val yesterday = List((new DateTime()).minusDays(1).withZone(DateTimeZone.UTC))
        r.copy(dates = yesterday)
      }
      case r: RunConfig => r
    }
    rc1
  }

  def processFeeds(conf: Config, rc3: RunConfig): RunConfig = {

    val feedsConf = rc3.getMossoFeeds() ++ rc3.getNastFeeds()

    val feed_result = rc3.feeds.foldLeft(true)((result, x) =>

      feedsConf.contains(x) match {

        // yeah, println is lame, but easy
        case false => {
          System.out.println(s"${x} is not a recognized feed")
          false
        }
        case true => result
      }
    )

    if (!feed_result)
      throw new IllegalArgumentException("invalid feed")

    val rc4 = rc3 match {

      // add feeds if no feeds given
      case r: RunConfig if r.feeds.isEmpty => {

        r.copy(feeds = feedsConf.toList )
      }
      case r: RunConfig => r
    }

    rc4
  }

  def processRegions(conf : Config, rc1: RunConfig): RunConfig = {
    val rc2 = rc1.copy(config = conf)

    val regionConf = conf.getConfig("tiamat.feeds.liveUri").root.unwrapped().keySet().toArray(Array[String]())

    val region_result = rc2.regions.foldLeft(true)((result, x) =>

      regionConf.contains(x) match {

        // yeah, println is lame, but easy
        case false => {
          System.out.println(s"${x} is not a recognized region")
          false
        }
        case true => result
      }
    )

    if (!region_result)
      throw new IllegalArgumentException("invalid region")

    val rc3 = rc2 match {

      // add regions if no regions given
      case r: RunConfig if r.regions.isEmpty => {

        r.copy(regions = regionConf)
      }
      case r: RunConfig => r
    }
    rc3
  }

  def getConf(runConfig: RunConfig): Config = {
    val conf = ConfigFactory.parseFile(new File(runConfig.configPath))
    conf.checkValid(ConfigFactory.load("reference.conf"), "tiamat")
    conf
  }
}
