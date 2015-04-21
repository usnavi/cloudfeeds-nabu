package com.rackspace.feeds.archives

import java.io._
import javax.xml.transform.stream.{StreamResult, StreamSource}
import javax.xml.transform.{URIResolver, TransformerFactory}

import net.sf.saxon.Controller
import org.apache.http.client.methods.{HttpHead, HttpPut}
import org.apache.http.entity.AbstractHttpEntity
import org.apache.http.impl.client.HttpClientBuilder
import org.joda.time.{DateTimeZone, DateTime}
import org.joda.time.format.{ISODateTimeFormat, DateTimeFormat}

import scala.io.Source

import Errors._

/**
 * Groups methods related to:
 *
 * <ul>
 *   <li> Generating feed structure in XML & JSON formats
 *   <li> Writing to cloud files
 * </ul>
 */
object CreateFilesFeed {

  import com.rackspace.feeds.archives.Identity._

  val JSON_KEY = "JSON"
  val XML_KEY = "XML"

  val dayFormat = DateTimeFormat.forPattern( "yyyy-MM-dd")

  val feedUpdateTime = ISODateTimeFormat.dateTime().print( (new DateTime).withZone( DateTimeZone.UTC ))

  def getFileName( key : ArchiveKey, date : String ): String = {

    val feed_name = key.feed.replace("/", "-")
    s"${key.region}_${feed_name}_${date}" + getExt( key.format )
  }

  def getExt( format : String ) : String = {

    format match {

      case JSON_KEY => ".json"
      case XML_KEY => ".xml"
    }
  }

  /**
   * Create feed and write to given file location.
   *
   * @param container
   * @param key
   * @param content
   * @param token
   * @param getFeedId
   * @param feedUuid
   * @param liveFeed
   */
  def createFeed(container : String,
                 key: ArchiveKey,
                 content: Iterable[AtomEntry],
                 token: String,
                 getFeedId: (String, String) => String,
                 feedUuid : String,
                 liveFeed : String ) {

    val client = HttpClientBuilder.create.build

    val containerClean = container.replaceFirst( "/$", "" )

    val fileName = getFileName( key, key.date )

    val path = s"$containerClean/$fileName"

    val put = new HttpPut( path )

    put.addHeader(TOKEN, token)
    put.addHeader(CONTENT_TYPE, key.format match {
      case XML_KEY => APP_XML
      case JSON_KEY => APP_JSON
    })

    val preface = feedPreface( containerClean,
      fileName,
      key,
      getFeedId,
      feedUuid,
      liveFeed )

    val entity = new StreamAtomFeedHttpEntity(
      key.format match {
        case JSON_KEY => generateJson( preface, content )_
        case XML_KEY => generateXml( preface, content )_
      }
    )

    put.setEntity(entity)

    val resp = client.execute(put)

    resp.getStatusLine.getStatusCode match {

      case 201 => ()
      case _ => throw new RestException(resp.getStatusLine.getStatusCode,
        WRITE( path, Source.fromInputStream(resp.getEntity.getContent).mkString ) )
    }
  }

  //
  // Generate Feed Formats
  //

  def generateJson( preface : String, entries : Iterable[AtomEntry] )( outputStream : OutputStream) = {

    val writer = new OutputStreamWriter( outputStream )

    // add end to preface
    val template = preface + "</feed>"


    // TODO:  chandra wants to pull this into its own class
    val factory = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null)
    val  resolver = new URIResolver() {

      override def resolve(href: String, base: String): javax.xml.transform.Source = {

        // assume xslt within classpath
        new StreamSource( getClass.getResourceAsStream( s"/$href" ) )
      }
    }
    factory.setURIResolver( resolver )

    val source = new StreamSource( new StringReader( template ))

    val out = new ByteArrayOutputStream()

    val result = new StreamResult( out )

    val xsltJson = new StreamSource( getClass.getResourceAsStream( "/xml2json-feeds.xsl" ) )
    val transformer = factory.newTransformer( xsltJson )
    (transformer.asInstanceOf[Controller]).setInitialTemplate( "main" )
    transformer.transform( source, result )

    writer.write( out.toString( "UTF-8" ).dropRight( 2 ) )

    val iterator = entries.toList
      .sorted( Ordering[AtomEntry].reverse ).iterator

    val entryArray = iterator.hasNext

    if( entryArray )
      writer.write( """, "entry": [""" )

    iterator.foreach{ e => writer.write( e.entrybody )
      if ( iterator.hasNext ) writer.write( "," )  }

    if( entryArray )
      writer.write( "]" )

    writer.write( "}}" )
    writer.close()
  }


  def generateXml( preface : String,
                   entries : Iterable[AtomEntry] )( out: OutputStream ) : Unit = {

    val writer = new OutputStreamWriter(out, "UTF-8");
    writer.write( preface )

    entries.toList
      .sorted( Ordering[AtomEntry].reverse )
      .map ( e => writer.write( e.entrybody ) )

    writer.write( "</feed>" )
    writer.close()
  }


  def feedPreface(container: String,
                  filename: String,
                  key: ArchiveKey,
                  getFeedId: (String, String) => String,
                  feedUuid : String,
                  liveFeed : String ): String = {

    val dateTime = dayFormat.parseDateTime( key.date )

    val next = getFileName( key, dayFormat.print( dateTime.minusDays( 1 ) ) )
    val prev = getFileName( key, dayFormat.print( dateTime.plusDays( 1 ) ) )

    s"""<?xml version="1.0" encoding="UTF-8" ?>
                  <feed xmlns="http://www.w3.org/2005/Atom"
                        xmlns:fh="http://purl.org/syndication/history/1.0">
        <fh:archive/>
        <link rel="current" href="${liveFeed}/${key.feed}/${getFeedId(key.tenantid, key.feed)}"/>
        <link rel="self" href="${container}/${filename}"/>
        <id>${feedUuid}</id>
        <title type="text">${key.feed}</title>
        <link rel="prev-archive" href="${container}/${prev}"/>
        <link rel="next-archive" href="${container}/${next}"/>
        <updated>${feedUpdateTime}</updated>"""
  }

  //
  // Container checks & creation
  //

  def containerCheck(container: String, imp: String) {
    containerExist(container, imp) match {
      case true => ()
      case false => createContainer(container, imp)
    }
  }


  def createContainer(uri: String, token: String) = {

    val client = HttpClientBuilder.create.build
    val put = new HttpPut(uri)
    put.addHeader(TOKEN, token)
    put.addHeader(CONTENT_TYPE, APP_JSON)

    val resp = client.execute(put)

    resp.getStatusLine.getStatusCode match {

      case 201 => ()
      case _ => throw new RestException(resp.getStatusLine.getStatusCode,
        CREATE_CONTAINER( uri, Source.fromInputStream(resp.getEntity.getContent).mkString ) )
    }
  }


  def containerExist(container: String, imp: String): Boolean = {
    val client = HttpClientBuilder.create.build
    val head = new HttpHead(container)
    head.addHeader(TOKEN, imp)
    val resp = client.execute(head)

    resp.getStatusLine.getStatusCode match {

      case 204 => true
      case 404 => false
      case _ => throw new RestException(resp.getStatusLine.getStatusCode,
        CONTAINER_EXISTS( container, Source.fromInputStream(resp.getEntity.getContent).mkString ) )
    }
  }
}


class StreamAtomFeedHttpEntity( proc : OutputStream => Unit ) extends AbstractHttpEntity {

  def isRepeatable() = false

  def getContentLength() = -1

  def isStreaming() = false

  def getContent() : InputStream = {
    // Should be implemented as well but is irrelevant for this case
    throw new UnsupportedOperationException();
  }

  def writeTo( outstream : OutputStream) : Unit = {

    proc( outstream )
  }
}
