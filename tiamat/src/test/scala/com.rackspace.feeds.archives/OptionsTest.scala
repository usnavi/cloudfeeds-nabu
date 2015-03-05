package com.rackspace.feeds.archives

import java.io.File

import com.typesafe.config.ConfigException
import org.joda.time.format.DateTimeFormat
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

/**
 * Created by rona6028 on 3/3/15.
 */
@RunWith(classOf[JUnitRunner])
class OptionsTest extends FunSuite {

  val FEEDS = Set( "feed1", "feed2" )
  val CONF = "src/test/resources/test.conf"

  test("Invalid feeds option returns error") {

    val options = new Options()

    intercept[Exception] {
      val runconfig = options.parseOptions( FEEDS, Array("-f", "feed1,feed3") )
    }
  }

  test("No feeds option produces default feeds") {

    val options = new Options()
    val config = options.parseOptions( FEEDS, Array("-c", CONF ) )

    assert( FEEDS &~ config.feeds.toSet isEmpty )
  }

  test("Conf file not exist") {

    val options = new Options()

    intercept[Exception] {
      val config = options.parseOptions(FEEDS, Array("-c", "no-exist.conf"))
    }
  }

  test("Invalid date option") {

    val options = new Options()

    intercept[Exception] {
      val config = options.parseOptions(FEEDS, Array("-d", "AAAA-AA-AA"))
    }
  }

  test("Invalid regions") {

    val options = new Options()

    intercept[Exception] {
      val config = options.parseOptions(FEEDS, Array("-r", "crazydown", "-c", CONF))
    }
  }

  test("Default regions") {

    val options = new Options()
    val config = options.parseOptions( FEEDS, Array("-c", CONF ) )

    assert( Set( "dfw","iad","hkg", "lon","ord", "syd" ) &~ config.regions.toSet isEmpty )
  }

  test( "Bad config file" ) {

    println( "greg" )
    println( "pwd: " + (new File( "." ).getCanonicalPath ))

    val options = new Options()
    intercept[ConfigException] {
      val config = options.parseOptions(FEEDS, Array("-c", "src/test/resources/bad.conf"))
    }
  }

  test( "Test happy path configuration" ) {

    val feeds = Set( "feed1", "feed2" )
    val tids = Set( "tid1", "tid2" )
    val regs = Set( "dfw", "iad" )

    val dateParse =  DateTimeFormat.forPattern("yyyy-MM-dd")
    val dates = Set( dateParse.parseDateTime( "2015-01-01" ), dateParse.parseDateTime(( "2014-01-01") ) )


    val options = new Options()
    val config = options.parseOptions( FEEDS, Array("-c", CONF,
    "-f", feeds.mkString(","),
    "-d", "2015-01-01,2014-01-01",
    "-t", tids.mkString( "," ),
    "-r", regs.mkString( "," ) ) )

    assert( feeds &~ config.feeds.toSet isEmpty )
    assert( tids &~ config.tenantIds.toSet isEmpty )
    assert( regs &~ config.regions.toSet isEmpty )
    assert( dates &~ config.dates.toSet isEmpty )
  }

  test( "test defaults!" ) {

    assert( false )
  }
}