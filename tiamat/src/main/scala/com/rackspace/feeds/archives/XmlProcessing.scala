package com.rackspace.feeds.archives

import java.io.{ByteArrayOutputStream, StringWriter, StringReader}
import javax.xml.transform
import javax.xml.transform._
import javax.xml.transform.stream.{StreamResult, StreamSource}

import net.sf.saxon.Controller
import org.codehaus.jackson.map.ObjectMapper
import org.codehaus.jackson.node.ObjectNode

import scala.xml.{XML, Elem, NodeSeq, Node}
import scala.xml.transform.{RuleTransformer, RewriteRule}

/**
 * Created by rona6028 on 3/11/15.
 */
object XmlProcessing {

  /**
   * Filter private attributes.
   *
   */
  def privateAttrs( entrybody : String ) : String = {

    val removeLink = new RewriteRule {
      override def transform(n: Node): NodeSeq = n match {
        case e: Elem if e.label == "link"
          && e.prefix == "atom" => NodeSeq.Empty
        case n => n
      }
    }

    val body = new RuleTransformer(removeLink)
      .transform(XML.loadString(entrybody))
      .toString()

    // filter private attributes
    val source = new StreamSource(new StringReader(body))
    val writer = new StringWriter()
    val result = new StreamResult(writer)

    val xsltWrapper = new StreamSource(getClass.getResourceAsStream("/wrapper.xsl"))

    val resolver = new URIResolver() {

      override def resolve(href: String, base: String): Source = {

        // assume xslt within classpath
        new StreamSource(getClass.getResourceAsStream(s"/$href"))
      }
    }

    val factory = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null)
    factory.setURIResolver(resolver)

    val transformer = factory.newTransformer(xsltWrapper)

    transformer.transform(source, result)

    writer.toString
  }

  /**
   *
   * Convert XML entry into JSON ready to be added to JSON array.
   *
   * @param entry
   * @return
   */
  def makeJson( entry : AtomEntry ) : AtomEntry = {

    val source = new StreamSource( new StringReader( entry.entrybody ) )
    val out = new ByteArrayOutputStream()
    val result = new StreamResult( out )

    val factory = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null)
    val  resolver = new URIResolver() {

      override def resolve(href: String, base: String): transform.Source = {

        // assume xslt within classpath
        new StreamSource( getClass.getResourceAsStream( s"/$href" ) )
      }
    }
    factory.setURIResolver( resolver )

    val xsltJson = new StreamSource( getClass.getResourceAsStream( "/xml2json-feeds.xsl" ) )
    val transformer = factory.newTransformer( xsltJson )
    (transformer.asInstanceOf[Controller]).setInitialTemplate( "main" )
    transformer.transform( source, result )

    AtomEntry(  makeArrayEntry( out.toString( "UTF-8" ) ), entry.datelastupdated, entry.id )
  }

  def makeArrayEntry(json: String): String = {

    val mapper = new ObjectMapper()

    val map = mapper.readTree( json )

    val entryMap = map.get("entry").asInstanceOf[ObjectNode]
    entryMap.remove("@type")

    val writer = new StringWriter()

    mapper.writeValue( writer , entryMap )

    writer.toString
  }
}
