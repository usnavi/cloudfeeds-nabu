package com.rackspace.feeds.archives

import java.io.ByteArrayOutputStream

import org.codehaus.jackson.map.ObjectMapper
import org.joda.time.{DateTimeZone, DateTime}
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

/**
 * Created by rona6028 on 3/5/15.
 */
@RunWith(classOf[JUnitRunner])
class CreateFilesFeedTest extends FunSuite {

  import CreateFilesFeed._
  import Archiver._

  val DATE_LAST_UPDATED_1 = new DateTime( 2014, 2, 18, 21, 12, 10, 997, DateTimeZone.UTC )
  val DATE_LAST_UPDATED_0 = new DateTime( 2014, 2, 18, 21, 12, 10, 0, DateTimeZone.UTC )

  val PREFACE = """<?xml version="1.0" encoding="UTF-8" ?>
      <feed xmlns="http://www.w3.org/2005/Atom"
            xmlns:fh="http://purl.org/syndication/history/1.0">
        <fh:archive/>
        <link rel="current" href="http://livefeed1/feed1/1234"/>
        <link rel="self" href="http://livefeed1/archive/1234_nast/container1/filename1"/>
        <id>uuid1</id>
        <title type="text">feed1</title>
        <link rel="prev-archive" href="http://livefeed1/archive/1234_nast/container1/region1_feed1_2014-02-19.xml"/>
        <link rel="next-archive" href="http://livefeed1/archive/1234_nast/container1/region1_feed1_2014-02-17.xml"/>
        <updated>TIME</updated>"""

  test( "feedPreface() - feed preface should be correct for given tenant and feed" ) {

    val tenantPrefs = Map( "1234" -> TenantPrefs( "1234", "1234_nast", Map(), List( CreateFilesFeed.XML_KEY ) ) )

    val preface = feedPreface( "http://cloudfiles/v1/1234_nast/container1",
      "filename1",
      ArchiveKey( "1234", "region1", "feed1", "2014-02-18", XML_KEY  ),
      makeNastIdToTenantMap( tenantPrefs, Set() ),
      "uuid1",
      "http://livefeed1" )

    assert( preface.replaceAll( "\\s+", " ").replaceAll( "(?<=<updated>).+(?=</updated>)", "TIME" ) ==
      PREFACE.replaceAll( "\\s+", " " )  )
  }

  test( "feedPreface() - feed preface should be correct for given tenant and Nast ID feed" ) {

    val protoPreface = """<?xml version="1.0" encoding="UTF-8" ?>
      <feed xmlns="http://www.w3.org/2005/Atom"
            xmlns:fh="http://purl.org/syndication/history/1.0">
        <fh:archive/>
        <link rel="current" href="http://livefeed1/feed1/1234_nast"/>
        <link rel="self" href="http://livefeed1/archive/1234_nast/container1/filename1"/>
        <id>uuid1</id>
        <title type="text">feed1</title>
        <link rel="prev-archive" href="http://livefeed1/archive/1234_nast/container1/region1_feed1_2014-02-19.xml"/>
        <link rel="next-archive" href="http://livefeed1/archive/1234_nast/container1/region1_feed1_2014-02-17.xml"/>
        <updated>TIME</updated>"""

    val tenantPrefs = Map( "1234" -> TenantPrefs( "1234", "1234_nast", Map(), List( CreateFilesFeed.XML_KEY ) ) )

    val preface = feedPreface( "http://cloudfiles/v1/1234_nast/container1",
      "filename1",
      ArchiveKey( "1234", "region1", "feed1", "2014-02-18", XML_KEY  ),
      makeNastIdToTenantMap( tenantPrefs, Set( "feed1" ) ),
      "uuid1",
      "http://livefeed1" )

    assert( preface.replaceAll( "\\s+", " ").replaceAll( "(?<=<updated>).+(?=</updated>)", "TIME" ) ==
      protoPreface.replaceAll( "\\s+", " " )  )
  }

  def getXmlEvents() : Iterable[AtomEntry] = {

    List( AtomEntry( """<atom:entry xmlns:usage-summary="http://docs.rackspace.com/core/usage-summary"
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
                       |""".stripMargin, DATE_LAST_UPDATED_1, 1 ),
      AtomEntry( """<atom:entry xmlns:usage-summary="http://docs.rackspace.com/core/usage-summary"
                   |            xmlns:wadl="http://wadl.dev.java.net/2009/02"
                   |            xmlns:d312e1="http://wadl.dev.java.net/2009/02"
                   |            xmlns:error="http://docs.rackspace.com/core/error"
                   |            xmlns:db="http://docbook.org/ns/docbook"
                   |            xmlns="http://wadl.dev.java.net/2009/02"
                   |            xmlns:atom="http://www.w3.org/2005/Atom">
                   |  <atom:id>urn:uuid:1</atom:id>
                   |  <atom:category term="tid:12334"/>
                   |  <atom:category term="rgn:DFW"/>
                   |  <atom:category term="dc:DFW1"/>
                   |  <atom:category term="rid:1"/>
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
                   |             resourceId="1"
                   |             region="DFW"
                   |             id="1"
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
                   |""".stripMargin, DATE_LAST_UPDATED_0, 0) )
  }

  def getJsonEvents() : Iterable[AtomEntry] = {

    List( AtomEntry( """{
                       |        "category": [
                       |            {
                       |                "term": "tid:1234"
                       |            },
                       |            {
                       |                "term": "rgn:DFW"
                       |            },
                       |            {
                       |                "term": "dc:DFW1"
                       |            },
                       |            {
                       |                "term": "rid:1"
                       |            },
                       |            {
                       |                "term": "widget.explicit.widget.usage"
                       |            },
                       |            {
                       |                "term": "type:widget.explicit.widget.usage"
                       |            }
                       |        ],
                       |        "content": {
                       |            "event": {
                       |                "@type": "http://docs.rackspace.com/core/event",
                       |                "dataCenter": "DFW1",
                       |                "endTime": "2013-03-15T23:59:59Z",
                       |                "id": "e53d007a-fc23-11e1-975c-cfa6b29bb814",
                       |                "product": {
                       |                    "@type": "http://docs.rackspace.com/usage/widget/explicit",
                       |                    "dateTime": "2013-09-26T15:32:00Z",
                       |                    "enumList": "BEST BEST",
                       |                    "label": "sampleString",
                       |                    "mid": "6e8bc430-9c3a-11d9-9669-0800200c9a66",
                       |                    "num_checks": 1,
                       |                    "resourceType": "WIDGET",
                       |                    "serviceCode": "Widget",
                       |                    "stringEnum": "3.0.1",
                       |                    "time": "15:32:00Z",
                       |                    "version": "1"
                       |                },
                       |                "region": "DFW",
                       |                "resourceId": "1",
                       |                "startTime": "2013-03-15T11:51:11Z",
                       |                "tenantId": "1234",
                       |                "type": "USAGE",
                       |                "version": "1"
                       |            }
                       |        },
                       |        "id": "urn:uuid:1",
                       |        "title": "Widget"
                       |    }
                       |""".stripMargin, DATE_LAST_UPDATED_1, 1 ),
      AtomEntry( """{
                   |        "category": [
                   |            {
                   |                "term": "tid:1234"
                   |            },
                   |            {
                   |                "term": "rgn:DFW"
                   |            },
                   |            {
                   |                "term": "dc:DFW1"
                   |            },
                   |            {
                   |                "term": "rid:4a2b42f4-6c63-11e1-815b-7fcbcf67f549"
                   |            },
                   |            {
                   |                "term": "widget.explicit.widget.usage"
                   |            },
                   |            {
                   |                "term": "type:widget.explicit.widget.usage"
                   |            }
                   |        ],
                   |        "content": {
                   |            "event": {
                   |                "@type": "http://docs.rackspace.com/core/event",
                   |                "dataCenter": "DFW1",
                   |                "endTime": "2013-03-15T23:59:59Z",
                   |                "id": "e53d007a-fc23-11e1-975c-cfa6b29bb814",
                   |                "product": {
                   |                    "@type": "http://docs.rackspace.com/usage/widget/explicit",
                   |                    "dateTime": "2013-09-26T15:32:00Z",
                   |                    "enumList": "BEST BEST",
                   |                    "label": "sampleString",
                   |                    "mid": "6e8bc430-9c3a-11d9-9669-0800200c9a66",
                   |                    "num_checks": 1,
                   |                    "resourceType": "WIDGET",
                   |                    "serviceCode": "Widget",
                   |                    "stringEnum": "3.0.1",
                   |                    "time": "15:32:00Z",
                   |                    "version": "1"
                   |                },
                   |                "region": "DFW",
                   |                "resourceId": "4a2b42f4-6c63-11e1-815b-7fcbcf67f549",
                   |                "startTime": "2013-03-15T11:51:11Z",
                   |                "tenantId": "1234",
                   |                "type": "USAGE",
                   |                "version": "1"
                   |            }
                   |        },
                   |        "id": "urn:uuid:e53d007a-fc23-11e1-975c-cfa6b29bb814",
                   |        "title": "Widget"
                   |    }
                   |""".stripMargin, DATE_LAST_UPDATED_0, 0) )
  }


  test( "generateJson() - with events" ) {

    val protoJson= """{"feed" : {"@type": "http://www.w3.org/2005/Atom", "link" : [{"rel" : "current","href" : "http://livefeed1/feed1/1234"}, {"rel" : "self","href" : "http://livefeed1/archive/1234_nast/container1/filename1"}, {"rel" : "prev-archive","href" : "http://livefeed1/archive/1234_nast/container1/region1_feed1_2014-02-19.xml"}, {"rel" : "next-archive","href" : "http://livefeed1/archive/1234_nast/container1/region1_feed1_2014-02-17.xml"}],"archive" :{ "@type" : "http://purl.org/syndication/history/1.0" }, "id" : "uuid1", "title" : {"type" : "text", "@text" : "feed1"}, "updated" : "TIME", "entry": [{
                     |        "category": [
                     |            {
                     |                "term": "tid:1234"
                     |            },
                     |            {
                     |                "term": "rgn:DFW"
                     |            },
                     |            {
                     |                "term": "dc:DFW1"
                     |            },
                     |            {
                     |                "term": "rid:1"
                     |            },
                     |            {
                     |                "term": "widget.explicit.widget.usage"
                     |            },
                     |            {
                     |                "term": "type:widget.explicit.widget.usage"
                     |            }
                     |        ],
                     |        "content": {
                     |            "event": {
                     |                "@type": "http://docs.rackspace.com/core/event",
                     |                "dataCenter": "DFW1",
                     |                "endTime": "2013-03-15T23:59:59Z",
                     |                "id": "e53d007a-fc23-11e1-975c-cfa6b29bb814",
                     |                "product": {
                     |                    "@type": "http://docs.rackspace.com/usage/widget/explicit",
                     |                    "dateTime": "2013-09-26T15:32:00Z",
                     |                    "enumList": "BEST BEST",
                     |                    "label": "sampleString",
                     |                    "mid": "6e8bc430-9c3a-11d9-9669-0800200c9a66",
                     |                    "num_checks": 1,
                     |                    "resourceType": "WIDGET",
                     |                    "serviceCode": "Widget",
                     |                    "stringEnum": "3.0.1",
                     |                    "time": "15:32:00Z",
                     |                    "version": "1"
                     |                },
                     |                "region": "DFW",
                     |                "resourceId": "1",
                     |                "startTime": "2013-03-15T11:51:11Z",
                     |                "tenantId": "1234",
                     |                "type": "USAGE",
                     |                "version": "1"
                     |            }
                     |        },
                     |        "id": "urn:uuid:1",
                     |        "title": "Widget"
                     |    }
                     |,{
                     |        "category": [
                     |            {
                     |                "term": "tid:1234"
                     |            },
                     |            {
                     |                "term": "rgn:DFW"
                     |            },
                     |            {
                     |                "term": "dc:DFW1"
                     |            },
                     |            {
                     |                "term": "rid:4a2b42f4-6c63-11e1-815b-7fcbcf67f549"
                     |            },
                     |            {
                     |                "term": "widget.explicit.widget.usage"
                     |            },
                     |            {
                     |                "term": "type:widget.explicit.widget.usage"
                     |            }
                     |        ],
                     |        "content": {
                     |            "event": {
                     |                "@type": "http://docs.rackspace.com/core/event",
                     |                "dataCenter": "DFW1",
                     |                "endTime": "2013-03-15T23:59:59Z",
                     |                "id": "e53d007a-fc23-11e1-975c-cfa6b29bb814",
                     |                "product": {
                     |                    "@type": "http://docs.rackspace.com/usage/widget/explicit",
                     |                    "dateTime": "2013-09-26T15:32:00Z",
                     |                    "enumList": "BEST BEST",
                     |                    "label": "sampleString",
                     |                    "mid": "6e8bc430-9c3a-11d9-9669-0800200c9a66",
                     |                    "num_checks": 1,
                     |                    "resourceType": "WIDGET",
                     |                    "serviceCode": "Widget",
                     |                    "stringEnum": "3.0.1",
                     |                    "time": "15:32:00Z",
                     |                    "version": "1"
                     |                },
                     |                "region": "DFW",
                     |                "resourceId": "4a2b42f4-6c63-11e1-815b-7fcbcf67f549",
                     |                "startTime": "2013-03-15T11:51:11Z",
                     |                "tenantId": "1234",
                     |                "type": "USAGE",
                     |                "version": "1"
                     |            }
                     |        },
                     |        "id": "urn:uuid:e53d007a-fc23-11e1-975c-cfa6b29bb814",
                     |        "title": "Widget"
                     |    }
                     |]}}""".stripMargin

    val output = new ByteArrayOutputStream()

    generateJson( PREFACE, getJsonEvents() )( output )

    val json = output.toString()

    // verify that JSON parses - just being extra paranoid
    (new ObjectMapper()).readTree( json )

    assert( json.replaceAll( "\\s+", "") == protoJson.replaceAll( "\\s+", "" ) )
  }

  test( "generateJson() - empty feed" ) {

    val protoJson= """{"feed" : {"@type": "http://www.w3.org/2005/Atom", "link" : [{"rel" : "current","href" : "http://livefeed1/feed1/1234"}, {"rel" : "self","href" : "http://livefeed1/archive/1234_nast/container1/filename1"}, {"rel" : "prev-archive","href" : "http://livefeed1/archive/1234_nast/container1/region1_feed1_2014-02-19.xml"}, {"rel" : "next-archive","href" : "http://livefeed1/archive/1234_nast/container1/region1_feed1_2014-02-17.xml"}],"archive" :{ "@type" : "http://purl.org/syndication/history/1.0" }, "id" : "uuid1", "title" : {"type" : "text", "@text" : "feed1"}, "updated" : "TIME"}}""".stripMargin

    val output = new ByteArrayOutputStream()

    generateJson( PREFACE, List() )( output )

    val json = output.toString()

    // verify that JSON parses - just being extra paranoid
    (new ObjectMapper()).readTree( json )

    assert( json.replaceAll( "\\s+", "") == protoJson.replaceAll( "\\s+", "" ) )
  }

  test( "generateXml() - with events" ) {

    val proto = """<?xml version="1.0" encoding="UTF-8" ?>
                  |      <feed xmlns="http://www.w3.org/2005/Atom"
                  |            xmlns:fh="http://purl.org/syndication/history/1.0">
                  |        <fh:archive/>
                  |        <link rel="current" href="http://livefeed1/feed1/1234"/>
                  |        <link rel="self" href="http://livefeed1/archive/1234_nast/container1/filename1"/>
                  |        <id>uuid1</id>
                  |        <title type="text">feed1</title>
                  |        <link rel="prev-archive" href="http://livefeed1/archive/1234_nast/container1/region1_feed1_2014-02-19.xml"/>
                  |        <link rel="next-archive" href="http://livefeed1/archive/1234_nast/container1/region1_feed1_2014-02-17.xml"/>
                  |        <updated>TIME</updated><atom:entry xmlns:usage-summary="http://docs.rackspace.com/core/usage-summary"
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
                  |<atom:entry xmlns:usage-summary="http://docs.rackspace.com/core/usage-summary"
                  |            xmlns:wadl="http://wadl.dev.java.net/2009/02"
                  |            xmlns:d312e1="http://wadl.dev.java.net/2009/02"
                  |            xmlns:error="http://docs.rackspace.com/core/error"
                  |            xmlns:db="http://docbook.org/ns/docbook"
                  |            xmlns="http://wadl.dev.java.net/2009/02"
                  |            xmlns:atom="http://www.w3.org/2005/Atom">
                  |  <atom:id>urn:uuid:1</atom:id>
                  |  <atom:category term="tid:12334"/>
                  |  <atom:category term="rgn:DFW"/>
                  |  <atom:category term="dc:DFW1"/>
                  |  <atom:category term="rid:1"/>
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
                  |             resourceId="1"
                  |             region="DFW"
                  |             id="1"
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
                  |</feed>""".stripMargin

    val output = new ByteArrayOutputStream()

    generateXml( PREFACE, getXmlEvents() )( output )

    assert( output.toString().replaceAll( "\\s+", " ") == proto.replaceAll( "\\s+", " " ) )
  }

  test( "generateXml() - empty feed" ) {


    val proto = """<?xml version="1.0" encoding="UTF-8" ?>
                  |      <feed xmlns="http://www.w3.org/2005/Atom"
                  |            xmlns:fh="http://purl.org/syndication/history/1.0">
                  |        <fh:archive/>
                  |        <link rel="current" href="http://livefeed1/feed1/1234"/>
                  |        <link rel="self" href="http://livefeed1/archive/1234_nast/container1/filename1"/>
                  |        <id>uuid1</id>
                  |        <title type="text">feed1</title>
                  |        <link rel="prev-archive" href="http://livefeed1/archive/1234_nast/container1/region1_feed1_2014-02-19.xml"/>
                  |        <link rel="next-archive" href="http://livefeed1/archive/1234_nast/container1/region1_feed1_2014-02-17.xml"/>
                  |        <updated>TIME</updated></feed>""".stripMargin

    val output = new ByteArrayOutputStream()

    generateXml( PREFACE, List() )( output )

    assert( output.toString().replaceAll( "\\s+", " ") == proto.replaceAll( "\\s+", " " ) )
  }

    val containerUrls = List( "https://hostname/v1/nastId/myContainer/",
                              "https://hostname/v2/nastId/myContainer" )
    containerUrls.foreach ( url =>
        test( "should get the right container name on url: " + url) {
            assert(getNastIdAndContainerName(url) == ("nastId", "myContainer"))
        }
    )

    val invalidUrls = List ( "some_invalid_urls", "sftp:/incomplete" )
    invalidUrls.foreach ( url =>
        test ( "should get MalformedURL on url: " + url) {
            intercept[java.net.MalformedURLException] {
                getNastIdAndContainerName(url)
            }
        }
    )

    val invalidContainerUrls = List ( "http://hostname/not_enough_parts" )
    invalidContainerUrls.foreach ( url =>
        test ( "should get AssertionError on url: " + url) {
            intercept[AssertionError] {
                getNastIdAndContainerName(url)
            }
        }
    )

}