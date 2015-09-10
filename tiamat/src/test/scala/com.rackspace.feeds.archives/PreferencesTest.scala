package com.rackspace.feeds.archives

import org.apache.spark.sql.Row

import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar

/**
 * Created by rona6028 on 3/3/15.
 */
@RunWith(classOf[JUnitRunner])
class PreferencesTest extends FunSuite with MockitoSugar {

  import Preferences._

  def createRow( tid : String, payload : String, aid : String, enabled : Boolean ) : PreferenceRow = {

    val mockRow = mock[PreferenceRow]

    when( mockRow.tenantId ).thenReturn( tid )
    when( mockRow.payload ).thenReturn( payload )
    when( mockRow.alternate_id ).thenReturn( aid )
    when( mockRow.enabled ).thenReturn( enabled )

    mockRow
  }

  test( "Missing and empty containers use default" ) {

    val payload =
      """
        { "enabled": true,
          "data_format" : [ "JSON", "XML" ],
          "default_archive_container_url" : "http://defaultContainer",
          "archive_container_urls": {
          "reg1" : "http://reg1",
          "reg3" : ""
          }
        }
      """

    val mockRow = createRow( "tid1", payload , "aid1", true)
    val list = tenantContainers( Seq( "reg1", "reg2", "reg3" ), mockRow )

    assert( list.size == 1 )

    val prefs = list( 0 )._2

    assert( prefs.formats == List( "JSON", "XML") )
    assert( prefs.alternateId == "aid1" )
    assert( prefs.containers.size == 3 )
    assert( prefs.containers( "reg1") == "http://reg1" )
    assert( prefs.containers( "reg2") == "http://defaultContainer" )
    assert( prefs.containers( "reg3") == "http://defaultContainer" )
  }

  test( "Default not used if all containers are specified" ) {

    val payload =
      """
        { "enabled": true,
          "data_format" : [ "JSON", "XML" ],
          "default_archive_container_url" : "http://defaultContainer",
          "archive_container_urls": {
             "reg1" : "http://reg1",
             "reg2" : "http://reg2",
             "reg3" : "http://reg3"
          }
        }
      """

    val mockRow = createRow( "tid1", payload , "aid1", true)
    val list = tenantContainers( Seq( "reg1", "reg2", "reg3" ), mockRow )

    assert( list.size == 1 )

    val prefs = list( 0 )._2

    assert( prefs.formats == List( "JSON", "XML") )
    assert( prefs.alternateId == "aid1" )
    assert( prefs.containers.size == 3 )
    assert( prefs.containers( "reg1") == "http://reg1" )
    assert( prefs.containers( "reg2") == "http://reg2" )
    assert( prefs.containers( "reg3") == "http://reg3" )
  }

  test( "No default, containers specified" ) {

    val payload =
      """
        { "enabled": true,
          "data_format" : [ "JSON", "XML" ],
          "default_archive_container_url" : "http://defaultContainer",
          "archive_container_urls": {
          }
        }
      """

    val mockRow = createRow( "tid1", payload , "aid1", true)
    val list = tenantContainers( Seq( "reg1", "reg2", "reg3" ), mockRow )

    assert( list.size == 1 )

    val prefs = list( 0 )._2

    assert( prefs.formats == List( "JSON", "XML") )
    assert( prefs.alternateId == "aid1" )
    assert( prefs.containers.size == 3 )
    assert( prefs.containers( "reg1") == "http://defaultContainer" )
    assert( prefs.containers( "reg2") == "http://defaultContainer" )
    assert( prefs.containers( "reg3") == "http://defaultContainer" )
  }

  test( "No default, no containers specified, no regions tracked" ) {

    val payload =
      """
        { "enabled": true,
          "data_format" : [ "JSON", "XML" ]
        }
      """

    val mockRow = createRow( "tid1", payload , "aid1", true)
    val list = tenantContainers( Seq( "reg1", "reg2", "reg3" ), mockRow )

    assert( list.size == 1 )

    val prefs = list( 0 )._2

    assert( prefs.formats == List( "JSON", "XML") )
    assert( prefs.alternateId == "aid1" )
    assert( prefs.containers.size == 0 )
  }
}
