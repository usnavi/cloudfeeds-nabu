package com.rackspace.feeds.archives

import java.io.File

import com.typesafe.config.ConfigException
import org.joda.time.format.DateTimeFormat
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import scala.collection.JavaConversions

/**
 * Created by rona6028 on 3/3/15.
 */
@RunWith(classOf[JUnitRunner])
class OptionsTest extends FunSuite {

  val CONF = "src/test/resources/test.conf"

  def getConf( path : String = CONF ) : String = {

    new File( "." ).getCanonicalPath.split( "/" ).last match {
      case "tiamat" => path
      case _ => "tiamat/" + path
    }
  }

  test("Invalid feeds option returns error") {

    val options = new Options()

    intercept[Exception] {
      val runconfig = options.parseOptions( Array("-f", "feed1,feed3") )
    }
  }

  test("No feeds option produces default feeds") {

    val options = new Options()
    val config = options.parseOptions( Array("-c", getConf(), "--all-tenants") )
    val feeds = getFeeds(config)

    assert( feeds &~ config.feeds.toSet isEmpty )
  }

  test("Conf file not exist") {

    val options = new Options()

    intercept[Exception] {
      val config = options.parseOptions( Array("-c", "no-exist.conf"))
    }
  }

  test("Invalid date option") {

    val options = new Options()

    intercept[Exception] {
      val config = options.parseOptions( Array("-d", "AAAA-AA-AA"))
    }
  }

  test("Invalid regions") {

    val options = new Options()

    intercept[Exception] {
      val config = options.parseOptions( Array("-r", "crazytown", "-c", getConf() ))
    }
  }

  test("Default regions") {

    val options = new Options()
    val config = options.parseOptions( Array("-c", getConf(), "--all-tenants" ) )

    assert( Set( "dfw","iad","hkg", "lon","ord", "syd" ) &~ config.regions.toSet isEmpty )
  }

  test( "Bad config file" ) {

    val options = new Options()
    intercept[ConfigException] {
      val config = options.parseOptions( Array("-c", getConf( "src/test/resources/bad.conf" ), "--all-tenants" ) )
    }
  }

  test("No tenants configured") {

    val options = new Options()

    intercept[IllegalArgumentException] {
      val config = options.parseOptions( Array("-c", getConf() ))
    }
  }

  test("Invalid tenants configured") {

    val options = new Options()

    intercept[IllegalArgumentException] {
      val config = options.parseOptions( Array("-c", getConf(), "--all-tenants", "--tenants", "12345,67890" ))
    }
  }

  test("Process all tenants") {

    val options = new Options()
    val config = options.parseOptions( Array("-c", getConf(), "--all-tenants" ) )

    assert( config.isProcessAllTenants == true )
  }

  test( "Test happy path configuration" ) {

    val tids = Set( "tid1", "tid2" )
    val regs = Set( "dfw", "iad" )

    val dateParse =  DateTimeFormat.forPattern("yyyy-MM-dd")
    val dates = Set( dateParse.parseDateTime( "2015-01-01" ), dateParse.parseDateTime(( "2014-01-01") ) )


    val options = new Options()
    val config = options.parseOptions( Array("-c", getConf(),
    "-f", "feed1/events,feed2/events",
    "-d", "2015-01-01,2014-01-01",
    "-t", tids.mkString( "," ),
    "-r", regs.mkString( "," ) ) )
    val feeds = getFeeds(config)

    assert( feeds &~ config.feeds.toSet isEmpty )
    assert( tids &~ config.tenantIds.toSet isEmpty )
    assert( regs &~ config.regions.toSet isEmpty )
    assert( dates &~ config.dates.toSet isEmpty )
    assert( config.skipSuccessFileCheck == false)
    assert( config.isProcessAllTenants == false)
  }

  test("skip success file check") {
    val options = new Options()
    val config = options.parseOptions( Array("-c", getConf(), "--skipSuccessFileCheck", "--all-tenants") )

    assert(config.skipSuccessFileCheck == true)
  }
  
  def getFeeds( config : RunConfig ) : Set[String] = {

    config.getMossoFeeds() ++ config.getNastFeeds()
  }
}