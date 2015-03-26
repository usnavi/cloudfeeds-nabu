package com.rackspace.feeds.archives

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import java.sql.Timestamp

/**
 * Created by rona6028 on 3/4/15.
 */
@RunWith(classOf[JUnitRunner])
class ArchiverTest extends FunSuite with MockitoSugar {

  import Archiver._

  val CHECK_XML = """<atom:entry xmlns="http://www.w3.org/2001/XMLSchema"
                    |            xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                    |            xmlns:atom="http://www.w3.org/2005/Atom">
                    |   <atom:id>urn:uuid:e53d007a-fc23-11e1-975c-cfa6b29bb814</atom:id>
                    |   <atom:category term="tid:1234"/>
                    |   <atom:category term="rgn:DFW"/>
                    |   <atom:category term="dc:DFW1"/>
                    |   <atom:category term="rid:4a2b42f4-6c63-11e1-815b-7fcbcf67f549"/>
                    |   <atom:category term="widget.widget.widget.usage"/>
                    |   <atom:category term="type:widget.widget.widget.usage"/>
                    |   <atom:title>Widget</atom:title>
                    |   <atom:content type="application/xml">
                    |      <event xmlns:sample="http://docs.rackspace.com/usage/widget"
                    |             xmlns="http://docs.rackspace.com/core/event"
                    |             region="DFW"
                    |             dataCenter="DFW1"
                    |             type="USAGE"
                    |             endTime="2013-03-16T11:51:11Z"
                    |             startTime="2013-03-15T11:51:11Z"
                    |             tenantId="1234"
                    |             resourceId="4a2b42f4-6c63-11e1-815b-7fcbcf67f549"
                    |             version="1"
                    |             id="e53d007a-fc23-11e1-975c-cfa6b29bb814">
                    |         <sample:product enumList="BEST BEST"
                    |                         dateTime="2013-09-26T15:32:00Z"
                    |                         time="15:32:00Z"
                    |                         num_checks="1"
                    |                         mid="6e8bc430-9c3a-11d9-9669-0800200c9a66"
                    |                         label="sampleString"
                    |                         resourceType="WIDGET"
                    |                         version="1"
                    |                         serviceCode="Widget"/>
                    |      </event>
                    |   </atom:content>
                    |</atom:entry>
                    |""".stripMargin

  val INPUT_XML = """<atom:entry xmlns:atom="http://www.w3.org/2005/Atom"
                    |            xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                    |            xmlns="http://www.w3.org/2001/XMLSchema">
                    |   <atom:id>urn:uuid:e53d007a-fc23-11e1-975c-cfa6b29bb814</atom:id>
                    |   <atom:category term="tid:1234"/>
                    |   <atom:category term="rgn:DFW"/>
                    |   <atom:category term="dc:DFW1"/>
                    |   <atom:category term="rid:4a2b42f4-6c63-11e1-815b-7fcbcf67f549"/>
                    |   <atom:category term="widget.widget.widget.usage"/>
                    |   <atom:category term="type:widget.widget.widget.usage"/>
                    |   <atom:title>Widget</atom:title>
                    |   <atom:content type="application/xml">
                    |      <event xmlns="http://docs.rackspace.com/core/event"
                    |             xmlns:sample="http://docs.rackspace.com/usage/widget"
                    |             id="e53d007a-fc23-11e1-975c-cfa6b29bb814"
                    |             version="1"
                    |             resourceId="4a2b42f4-6c63-11e1-815b-7fcbcf67f549"
                    |             tenantId="1234"
                    |             startTime="2013-03-15T11:51:11Z"
                    |             endTime="2013-03-16T11:51:11Z"
                    |             type="USAGE"
                    |             dataCenter="DFW1"
                    |             region="DFW">
                    |         <sample:product serviceCode="Widget"
                    |                         version="1"
                    |                         resourceType="WIDGET"
                    |                         label="sampleString"
                    |                         mid="6e8bc430-9c3a-11d9-9669-0800200c9a66"
                    |                         num_checks="1"
                    |                         time="15:32:00Z"
                    |                         dateTime="2013-09-26T15:32:00Z"
                    |                         enumList="BEST BEST"/>
                    |      </event>
                    |   </atom:content>
                    |</atom:entry>""".stripMargin

  val CHECK_JSON = """{"category":
                     |  [{"term":"tid:1234"},
                     |    {"term":"rgn:DFW"},
                     |    {"term":"dc:DFW1"},
                     |    {"term":"rid:4a2b42f4-6c63-11e1-815b-7fcbcf67f549"},
                     |    {"term":"widget.widget.widget.usage"},
                     |    {"term":"type:widget.widget.widget.usage"}],
                     |  "id":"urn:uuid:e53d007a-fc23-11e1-975c-cfa6b29bb814",
                     |  "title":"Widget",
                     |  "content":{
                     |    "event":{
                     |      "@type":"http://docs.rackspace.com/core/event",
                     |      "region":"DFW",
                     |      "dataCenter":"DFW1",
                     |      "type":"USAGE",
                     |      "endTime":"2013-03-16T11:51:11Z",
                     |      "startTime":"2013-03-15T11:51:11Z",
                     |      "tenantId":"1234",
                     |      "resourceId":"4a2b42f4-6c63-11e1-815b-7fcbcf67f549",
                     |      "version":"1",
                     |      "id":"e53d007a-fc23-11e1-975c-cfa6b29bb814","
                     |      product":{
                     |        "@type":"http://docs.rackspace.com/usage/widget",
                     |        "enumList":"BEST BEST",
                     |        "dateTime":"2013-09-26T15:32:00Z",
                     |        "time":"15:32:00Z",
                     |        "num_checks":1,
                     |        "mid":"6e8bc430-9c3a-11d9-9669-0800200c9a66",
                     |        "label":"sampleString",
                     |        "resourceType":"WIDGET",
                     |        "version":"1",
                     |        "serviceCode":"Widget"}}}}
                     |""".stripMargin


  test( "index() - process event with private attributes" ) {

    val protoKey = ArchiveKey( "1234", "dfw", "feed1", "2014-02-18", CreateFilesFeed.XML_KEY )

    val protoEntry = AtomEntry( """<atom:entry xmlns:usage-summary="http://docs.rackspace.com/core/usage-summary"
                                  |            xmlns:wadl="http://wadl.dev.java.net/2009/02"
                                  |            xmlns:d312e1="http://wadl.dev.java.net/2009/02"
                                  |            xmlns:error="http://docs.rackspace.com/core/error"
                                  |            xmlns:db="http://docbook.org/ns/docbook"
                                  |            xmlns="http://wadl.dev.java.net/2009/02"
                                  |            xmlns:atom="http://www.w3.org/2005/Atom">
                                  |  <atom:id>urn:uuid:560490c6-6c63-11e1-adfe-27851d5aed43</atom:id>
                                  |  <atom:category term="tid:12334"/>
                                  |  <atom:category term="rgn:DFW"/>
                                  |  <atom:category term="dc:DFW1"/>
                                  |  <atom:category term="rid:4a2b42f4-6c63-11e1-815b-7fcbcf67f549"/>
                                  |  <atom:category term="widget.widget.widget.update"/>
                                  |  <atom:category term="type:widget.widget.widget.update"/>
                                  |  <atom:category term="label:test"/>
                                  |  <atom:category term="metaData.key:foo"/>
                                  |  <atom:content type="application/xml">
                                  |      <event xmlns:widget="http://docs.rackspace.com/usage/widget"
                                  |             xmlns="http://docs.rackspace.com/core/event"
                                  |             version="1"
                                  |             type="UPDATE"
                                  |             tenantId="12334"
                                  |             startTime="2012-03-12T11:51:11Z"
                                  |             resourceId="4a2b42f4-6c63-11e1-815b-7fcbcf67f549"
                                  |             region="DFW"
                                  |             id="560490c6-6c63-11e1-adfe-27851d5aed43"
                                  |             environment="PROD"
                                  |             endTime="2012-03-12T15:51:11Z"
                                  |             dataCenter="DFW1">
                                  |         <widget:product widgetOnlyAttribute="bar"
                                  |                         version="3"
                                  |                         serviceCode="Widget"
                                  |                         resourceType="WIDGET"
                                  |                         mid="e9a67860-52e6-11e3-a0d1-002500a28a7a"
                                  |                         label="test">
                                  |            <widget:metaData key="foo"/>
                                  |            <widget:mixPublicPrivateAttributes myAttribute="here it should be public"/>
                                  |         </widget:product>
                                  |      </event>
                                  |  </atom:content>
                                  |
                                  |  <atom:updated>2014-02-18T21:12:10.997Z</atom:updated>
                                  |  <atom:published>2014-02-18T21:12:10.997Z</atom:published>
                                  |</atom:entry>
                                  |""".stripMargin, 1 )

    val entry = Entry( protoKey.tenantid, protoKey.region, protoKey.feed,
      """<atom:entry xmlns:atom="http://www.w3.org/2005/Atom" xmlns="http://wadl.dev.java.net/2009/02" xmlns:db="http://docbook.org/ns/docbook" xmlns:error="http://docs.rackspace.com/core/error" xmlns:d312e1="http://wadl.dev.java.net/2009/02" xmlns:wadl="http://wadl.dev.java.net/2009/02" xmlns:usage-summary="http://docs.rackspace.com/core/usage-summary">
        |  <atom:id>urn:uuid:560490c6-6c63-11e1-adfe-27851d5aed43</atom:id>
        |  <atom:category term="tid:12334"/>
        |  <atom:category term="rgn:DFW"/>
        |  <atom:category term="dc:DFW1"/>
        |  <atom:category term="rid:4a2b42f4-6c63-11e1-815b-7fcbcf67f549"/>
        |  <atom:category term="widget.widget.widget.update"/>
        |  <atom:category term="type:widget.widget.widget.update"/>
        |  <atom:category term="label:test"/>
        |  <atom:category term="metaData.key:foo"/>
        |  <atom:content type="application/xml">
        |    <event xmlns="http://docs.rackspace.com/core/event" xmlns:widget="http://docs.rackspace.com/usage/widget" dataCenter="DFW1" endTime="2012-03-12T15:51:11Z" environment="PROD" id="560490c6-6c63-11e1-adfe-27851d5aed43" region="DFW" resourceId="4a2b42f4-6c63-11e1-815b-7fcbcf67f549" startTime="2012-03-12T11:51:11Z" tenantId="12334" type="UPDATE" version="1">
        |      <widget:product label="test" mid="e9a67860-52e6-11e3-a0d1-002500a28a7a" resourceType="WIDGET" serviceCode="Widget" version="3" widgetOnlyAttribute="bar" privateAttribute1="something you can not see" myAttribute="here it should be private" privateAttribute3="W2">
        |        <widget:metaData key="foo" value="bar"/>
        |        <widget:mixPublicPrivateAttributes privateAttribute3="45" myAttribute="here it should be public"/>
        |      </widget:product>
        |    </event>
        |  </atom:content>
        |  <atom:link href="https://localhost/functest1/events/entries/urn:uuid:560490c6-6c63-11e1-adfe-27851d5aed43" rel="self"/>
        |  <atom:updated>2014-02-18T21:12:10.997Z</atom:updated>
        |  <atom:published>2014-02-18T21:12:10.997Z</atom:published>
        |</atom:entry>""".stripMargin,
      new Timestamp( 1392779530 ), 1,
      "tid:12334|rgn:DFW|dc:DFW1|rid:4a2b42f4-6c63-11e1-815b-7fcbcf67f549|widget.widget.widget.update|type:widget.widget.widget.update|label:test|metaData.key:foo",
      protoKey.date )

    val tenantPrefs = Map( "1234" -> TenantPrefs( "1234", "1234_nast", Map(), List( CreateFilesFeed.XML_KEY ) ) )

    val ( archiveKey, atomEntry ) = index( tenantPrefs )( entry )( 0 )

    assert( archiveKey == protoKey  )
    assert( atomEntry.id == protoEntry.id )
    assert( atomEntry.entrybody.replaceAll("\\s+", " ") == protoEntry.entrybody.replaceAll( "\\s+", " " ) )
  }

  test( "index() - process event with no private attributes" ) {

    val protoKey = ArchiveKey( "1234", "dfw", "feed1", "2014-02-18", "XML" )

    val protoEntry = AtomEntry( CHECK_XML, 1 )

    val row = Entry( protoKey.tenantid,
      protoKey.region,
      protoKey.feed,
      INPUT_XML,
      new Timestamp( 1392779530 ),
      1,
      "tid:12334|rgn:DFW|dc:DFW1|rid:4a2b42f4-6c63-11e1-815b-7fcbcf67f549|widget.widget.widget.update|type:widget.widget.widget.update|label:test|metaData.key:foo",
      protoKey.date )

    val tenantPrefs = Map( "1234" -> TenantPrefs( "1234", "1234_nast", Map(), List( CreateFilesFeed.XML_KEY ) ) )

    val ( archiveKey, atomEntry ) = index( tenantPrefs )( row )( 0 )

    assert( archiveKey == protoKey  )
    assert( atomEntry.id == protoEntry.id )
    assert( atomEntry.entrybody.replaceAll("\\s+", " ") == protoEntry.entrybody.replaceAll( "\\s+", " " ) )
  }

  test( "index() event for a NastId feed" ) {

    val protoKey = ArchiveKey( "1234", "dfw", "feed1", "2014-02-18", CreateFilesFeed.XML_KEY )

    val protoEntry = AtomEntry( CHECK_XML, 1 )

    val nastId = "1234_nast"

    val row = Entry( nastId,
      protoKey.region,
      protoKey.feed,
      INPUT_XML,
      new Timestamp( 1392779530 ), 1,
      "tid:12334|rgn:DFW|dc:DFW1|rid:4a2b42f4-6c63-11e1-815b-7fcbcf67f549|widget.widget.widget.update|type:widget.widget.widget.update|label:test|metaData.key:foo",
      protoKey.date )

    val tenantPrefs = Map( "1234" -> TenantPrefs( "1234", nastId, Map(), List( CreateFilesFeed.XML_KEY ) ) )

    val ( archiveKey, atomEntry ) = index( tenantPrefs )( row )( 0 )

    assert( archiveKey == protoKey  )
    assert( atomEntry.id == protoEntry.id )
    assert( atomEntry.entrybody.replaceAll("\\s+", " ") == protoEntry.entrybody.replaceAll( "\\s+", " " ) )
  }

  test( "processJson() into JSON" ) {

    val inKey = ArchiveKey( "1234", "dfw", "feed1", "2014-02-18", CreateFilesFeed.JSON_KEY )
    val inEntry = AtomEntry( CHECK_XML, 1 )

    val( outKey, outEntry ) = processJson( ( inKey, inEntry))

    assert( inKey == outKey )
    assert( inEntry.id == outEntry.id )
    assert( CHECK_JSON.replaceAll( "\\s+", "" ) == outEntry.entrybody.replaceAll( "\\s+", "" ))
  }

  test( "processJson() into XML" ) {

    val inKey = ArchiveKey( "1234", "dfw", "feed1", "2014-02-18", CreateFilesFeed.XML_KEY )
    val inEntry = AtomEntry( CHECK_XML, 1 )

    val( outKey, outEntry ) = processJson( ( inKey, inEntry))

    assert( inKey == outKey )
    assert( inEntry.id == outEntry.id )
    assert( CHECK_XML.replaceAll( "\\s+", " " ) == outEntry.entrybody.replaceAll( "\\s+", " " ))
  }
}
