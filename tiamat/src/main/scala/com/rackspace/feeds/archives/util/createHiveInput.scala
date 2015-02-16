package com.rackspace.feeds.archives.util

import java.io._

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

/**
 * Created by rona6028 on 1/27/15.
 */

case class User( tid : String, nastyD : String, user : String )

object CreateHiveInput {

  val CA = '\001'

  val format = ISODateTimeFormat.dateTime()

  def main( args: Array[String] ): Unit = {

    val users = Array( User( "5821027", "StagingUS_cab08997-1c5d-4545-815a-186592907ef9", "cfeedstestadminrole" ),
      User( "5914283", "StagingUS_b62ef4d5-0f1d-4167-bcb4-1ff07105fb52", "ahfeedadmin" ) )

    val feeds = 10
    val nasty_feeds = 1
//    val total_entries = 9130000
    val total_entries = 913
    val percent_single_user = .1
    val date = "2015-01-27"

    val entries_file = "import.txt"
    val entries = new BufferedWriter( new FileWriter( new File ( entries_file )))

    val prefs_file = "prefs.txt"

    val fake_users = 10 - users.size

    val num_feeds_user = (( total_entries / ( feeds + nasty_feeds ) ) * percent_single_user).toInt

    (1 to feeds).zipWithIndex.foreach { case( f, fI )=>

      val feed = s"feed_$f/events"

      users.zipWithIndex.foreach { case( u, uI ) =>

        (1 to num_feeds_user).zipWithIndex.foreach {  case( _, eI ) =>
          writeEntry( fI + uI + eI, entries, u.tid, feed, date)
        }
      }

      (1 to fake_users).zipWithIndex.foreach { case(u, uI ) =>

        (1 to num_feeds_user).zipWithIndex.foreach { case( _, eI ) =>
          writeEntry( fI + uI + eI, entries, s"fake$u", feed, date)
        }
      }
    }

    (1 to nasty_feeds).zipWithIndex.foreach { case( f, fI ) =>

      val feed = s"nasty_$f/events"

      users.zipWithIndex.foreach { case( u, uI)  =>

        (1 to num_feeds_user).zipWithIndex.foreach { case( _, eI ) =>
          writeEntry(fI + uI + eI, entries, u.nastyD, feed, date)
        }
      }

      (1 to fake_users).zipWithIndex.foreach { case( u, uI ) =>

        (1 to num_feeds_user).zipWithIndex.foreach { case( _, eI ) =>
          writeEntry(fI + uI + eI, entries, s"fake_$u", feed, date)
        }
      }
    }

    entries.close()
  }

  /*
  CREATE external TABLE import(
  id bigint,
  entryid string,
  creationdate timestamp,
  datelastupdated timestamp,
  entrybody string,
  categories string,
  eventtype string,
  tenantid string,
  dc string,
  feed string,
  date string)

  location '<parent directory of text file>'

set hive.exec.dynamic.partition=true;
set hive.exec.dynamic.partition.mode=nonstrict;
from import e
insert overwrite table entries  partition(date, feed)
select e.id, e.entryid, e.creationdate, e.datelastupdated, e.entrybody, e.categories,
e.eventtype, e.tenantid, e.dc, e.date, e.feed;

  */

  def writeEntry(index : Int, writer : Writer, tid : String, feed : String, date : String ) = {

    val updated = format.print(  new DateTime() ).replace( 'T', ' ').substring(0, 23)


    writer.write( s"""${index}${CA}urn:uuid:59085a27-f9ac-44f7-a74b-0d41fe3c4585${CA}2014-12-01 17:16:22.448148${CA}${updated}${CA}<atom:entry index="${index}" xmlns:atom="http://www.w3.org/2005/Atom" xmlns="http://wadl.dev.java.net/2009/02" xmlns:db="http://docbook.org/ns/docbook" xmlns:error="http://docs.rackspace.com/core/error" xmlns:wadl="http://wadl.dev.java.net/2009/02" xmlns:json="http://json-schema.org/schema#" xmlns:saxon="http://saxon.sf.net/" xmlns:sum="http://docs.rackspace.com/core/usage/schema/summary" xmlns:d558e1="http://wadl.dev.java.net/2009/02" xmlns:cldfeeds="http://docs.rackspace.com/api/cloudfeeds"><atom:id>urn:uuid:59085a27-f9ac-44f7-a74b-0d41fe3c4585</atom:id><atom:category term="tid:${tid}" /><atom:category term="rgn:DFW" /><atom:category term="dc:DFW1" /><atom:category term="rid:ed3f75f5-bd98-4c62-b670-46c7d15ea601" /><atom:category term="widget.widget.gadget.usage" /><atom:category term="type:widget.widget.gadget.usage" /><atom:content type="application/xml"><event xmlns="http://docs.rackspace.com/core/event" xmlns:widget="http://docs.rackspace.com/usage/widget" dataCenter="DFW1" endTime="2012-03-12T15:51:11Z" environment="PROD" id="59085a27-f9ac-44f7-a74b-0d41fe3c4585" region="DFW" resourceId="ed3f75f5-bd98-4c62-b670-46c7d15ea601" startTime="2012-03-12T11:51:11Z" tenantId="${tid}" type="USAGE" version="1"><widget:product version="3" serviceCode="Widget" resourceType="WIDGET" label="test" widgetOnlyAttribute="bar" privateAttribute1="something you can not see" myAttribute="here it should be private" privateAttribute3="W2" mid="e9a67860-52e6-11e3-a0d1-002500a28a7a"><widget:metaData key="foo" value="bar"/><widget:mixPublicPrivateAttributes privateAttribute3="45" myAttribute="here it should be public"/></widget:product></event></atom:content><atom:link href="https://atom.test.ord1.us.ci.rackspace.net/functest1/events/entries/urn:uuid:59085a27-f9ac-44f7-a74b-0d41fe3c4585" rel="self" /><updated>${updated}</updated><published>2015-01-30T20:59:53.836Z</published></atom:entry>${CA}rgn:dfw|dc:dfw1|rid:ed3f75f5-bd98-4c62-b670-46c7d15ea601|widget.widget.gadget.usage${CA}widget.widget.gadget.usage${CA}${tid}${CA}ORD${CA}${feed}${CA}${date}\n""" )
  }

}
