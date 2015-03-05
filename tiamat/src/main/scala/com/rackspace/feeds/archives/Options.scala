package com.rackspace.feeds.archives

import java.io.File

import com.typesafe.config.{ConfigFactory, Config}
import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.DateTimeFormat
import scopt.{OptionParser, Read}

case class RunConfig( configPath : String = Options.CONF,
                      feeds : Seq[String] = Seq[String](),
                      dates : Seq[DateTime] = Seq[DateTime](),
                      tenantIds : Seq[String] = Seq[String](),
                      regions : Seq[String] = Seq[String]() )

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

  def parseOptions( feedSet : Set[String], args: Array[String]): RunConfig = {
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
      } validate { x =>

        x.foldLeft( success )( ( output, f) =>

          feedSet.contains(f) match {
            case true => output
            case false => failure(s"'$f' is not an archived feed")
          })
      } text ("List feed names (comma-separated).  Default is to archive all archivable feeds.")

      opt[Seq[DateTime]]('d', "dates") action { (x, c) =>

        c.copy(dates = x)
      } text ("List of dates in the format of YYYY-MM-DD (comma-separated).  Default is yesterday's date.")

      opt[Seq[String]]('t', "tenants") action { (x, c) =>

        c.copy(tenantIds = x)
      } text ("List of tenant IDs (comma-separated).  Default is all archiving-enabled tenants.")

      opt[Seq[String]]('r', "regions") action { (x, c) =>

        c.copy( regions = x)
      }

      help("help") text ("Show this.")
    }

    val rc = parser.parse(args, RunConfig()) match {

      case Some(runConfig: RunConfig) => runConfig
      case _ => throw new Exception( "Invalid ")
    }

    val rc1 = rc match {

      // add yesterday if no date given
      case r: RunConfig if r.dates.isEmpty => {

        val yesterday = List((new DateTime()).minusDays(1).withZone(DateTimeZone.UTC))
        r.copy(dates = yesterday)
      }
      case r: RunConfig => r
    }

    val rc2 = rc1 match {

      // add all feeds if no feeds given
      case r : RunConfig if r.feeds.isEmpty => {

        r.copy( feeds = feedSet.toList )
      }
      case r : RunConfig => r
    }

    val conf = getConf( rc2 )
    val regionConf = conf.getConfig( "tiamat.feeds.liveUri" ).root.unwrapped().keySet().toArray( Array[String]() )

    val result = rc2.regions.foldLeft( true)( (result, x) =>

      regionConf.contains( x ) match {

        // yeah, println is lame, but easy
        case false => {System.out.println( s"${x} is not a region" )
          false }
        case true => result
      }
    )

    if( !result )
      throw new Exception( "invalid region" )

    val rc3 = rc2 match {

      // add regions if no regions given
      case r : RunConfig if r.regions.isEmpty => {

        r.copy( regions = regionConf )
      }
      case r : RunConfig => r
    }

    rc3
  }

  def getConf(runConfig: RunConfig): Config = {
    val conf = ConfigFactory.parseFile(new File(runConfig.configPath))
    conf.checkValid(ConfigFactory.load("reference.conf"), "tiamat")
    conf
  }
}
