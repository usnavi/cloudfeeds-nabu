<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="2.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:cldfeeds="http://docs.rackspace.com/api/cloudfeeds"
    xmlns:cf-nsattrs="http://docs.rackspace.com/api/cloudfeeds/non-string-attrs"
    xmlns:atom="http://www.w3.org/2005/Atom"
    xmlns:svcdoc="http://www.w3.org/2007/app"
    xmlns:saxon="http://saxon.sf.net/"
    exclude-result-prefixes="cldfeeds">
    
    <xsl:output method="text" encoding="utf-8"/>
 
    <!-- atom namespace -->
    <xsl:variable name="atomNs" select="'http://www.w3.org/2005/Atom'"/>
    
    <!-- this nonStringAttrs.xsl file is generated -->
    <xsl:include href="nonStringAttrs.xsl"/>

    <!-- The "main" template". This is the entry point, or in Saxon's term,
         the "initial template".
    -->
    <xsl:template name="main">
        <xsl:text>{</xsl:text>
        <xsl:apply-templates select="/atom:entry" mode="root"/>
        <xsl:apply-templates select="/atom:feed"  mode="root"/>
        <xsl:apply-templates select="/*[node()]"  mode="others"/>
        <xsl:text>}</xsl:text>     
    </xsl:template>

    <!-- this template wrap the "sample" root node around the main template,
         for xproc processing. -->
    <xsl:template name="genSample">
        <sample>
            <xsl:call-template name="main"/>
        </sample>
    </xsl:template>

    <!-- A template that prints out the root 'feed' JSON object -->
    <xsl:template match="atom:feed"  mode="root">
        <xsl:text>"</xsl:text><xsl:value-of select="local-name()"/><xsl:text>" : {</xsl:text>
        <xsl:text>"@type": "</xsl:text><xsl:value-of select="namespace-uri()"/><xsl:text>", </xsl:text>
        <xsl:call-template name="atomList"> 
            <xsl:with-param name="nodes" select="atom:link" as="node()*"/>
            <!-- CF-545 - only print comma if there are entries -->
            <xsl:with-param name="printComma" select="boolean(atom:entry) or cldfeeds:getSiblingCount(atom:link) &gt; 0"/>
        </xsl:call-template>
        
        <!-- print all the atom:entry elements -->
        <xsl:if test="count(atom:entry) &gt; 0">           
            <xsl:text>"entry" : [</xsl:text>
            <xsl:for-each select="atom:entry">
                <xsl:text>{</xsl:text>
                <xsl:apply-templates select="current()" mode="entryObject" />
                <xsl:text>}</xsl:text>
                <xsl:if test="position() ne last()"><xsl:text>, </xsl:text></xsl:if>
            </xsl:for-each>
            <xsl:text>], </xsl:text>
        </xsl:if>
        
        <!-- print the rest -->
        <xsl:apply-templates select="./*" mode="others"/>
        <xsl:text>}</xsl:text>
    </xsl:template>
    
    <!-- A template that handles the root 'entry' object -->
    <xsl:template match="atom:entry" mode="root">
        <xsl:text>"</xsl:text><xsl:value-of select="local-name()"/><xsl:text>" : {</xsl:text>
        <xsl:text>"@type": "</xsl:text><xsl:value-of select="namespace-uri()"/><xsl:text>", </xsl:text>
        <xsl:apply-templates select="." mode="entryObject"/>      
        <xsl:text>}</xsl:text>
    </xsl:template>
    
    <!-- A template that prints out the 'entry' JSON object. This must
         be called from another template that already prints out the 
         curly braces {}.
    -->
    <xsl:template match="atom:entry" mode="entryObject">
        <!-- prints the all the category elements -->
        <xsl:call-template name="atomList"> 
            <xsl:with-param name="nodes" select="atom:category" as="node()*"/>
            <xsl:with-param name="printComma" select="true()"/>
        </xsl:call-template>
        
        <!-- prints the all the link elements -->
        <xsl:call-template name="atomList"> 
            <xsl:with-param name="nodes" select="atom:link" as="node()*"/>
            <xsl:with-param name="printComma" select="true()"/>
        </xsl:call-template>
        
        <!-- print the rest -->
        <xsl:apply-templates select="./*" mode="others"/>
    </xsl:template>
    
    <!-- A no-op template for the atom nodes we have to handle in special ways
        by using the above templates that specifically matches for those.
    -->
    <xsl:template match="atom:category | atom:link | atom:feed | atom:entry" mode="others"/>
 
    <!-- A generic template that basically operates on any XML nodes and detects whether
         the nodes are to be printed as list, or normal JSON objects. 
    -->
    <xsl:template match="*" mode="others">                
        <xsl:choose>
            <!--
              If this is a CADF attachments node, treat its contents as an array.
             -->
            <xsl:when test="cldfeeds:isCadfAttachments( local-name(), namespace-uri() )">
                <xsl:text>"</xsl:text><xsl:value-of select="local-name()"/><xsl:text>" : [</xsl:text>
                <xsl:apply-templates select="*" mode="others" />
                <xsl:text>]</xsl:text>
                <xsl:variable name="sibs" select="following-sibling::*"/>
                <xsl:if test="count($sibs) &gt; 0">, </xsl:if>
            </xsl:when>

            <!--
              If this is a CADF attachment node, treat each node as a member of an array.
            -->
            <xsl:when test="cldfeeds:isCadfAttachment( local-name(), namespace-uri() )">
                <xsl:apply-templates select="." mode="obj-content" />
                <xsl:if test="name(following-sibling::*[1]) = name(.)">, </xsl:if>
            </xsl:when>

            <!-- handle the last item of a list object -->
            <xsl:when test="name(preceding-sibling::*[1]) = name(current()) and name(following-sibling::*[1]) != name(current())">
                <xsl:apply-templates select="." mode="obj-content" />
                <xsl:text>]</xsl:text>
                <xsl:if test="count(following-sibling::*[name() != name(current())]) &gt; 0">, </xsl:if>
            </xsl:when>
            
            <!-- handle a single item (not the last one) of a list object -->
            <xsl:when test="name(preceding-sibling::*[1]) = name(current())">
                <xsl:apply-templates select="." mode="obj-content"/>
                <xsl:if test="name(following-sibling::*[1]) = name(current())">, </xsl:if>
            </xsl:when>
            
            <!-- handle the first item of a list object -->
            <xsl:when test="following-sibling::*[1][name() = name(current())]">
                <xsl:text>"</xsl:text><xsl:value-of select="local-name()"/><xsl:text>" : [</xsl:text>
                <xsl:apply-templates select="." mode="obj-content" /><xsl:text>, </xsl:text>
            </xsl:when>

            <!-- handle nodes that have child nodes or attributes -->
            <xsl:when test="count(./child::*) > 0 or count(@*) > 0">
                <xsl:text>"</xsl:text><xsl:value-of select="local-name()"/>" : <xsl:apply-templates select="." mode="obj-content"/>

                <xsl:if test="cldfeeds:getSiblingCount(.) &gt; 0">, </xsl:if>
            </xsl:when>
            
            <!-- handles the archive element -->
            <xsl:when test="cldfeeds:isArchiveNode( local-name(), namespace-uri() )">
                <xsl:text>"</xsl:text><xsl:value-of select="local-name()"/><xsl:text>" : {</xsl:text>
                <xsl:text>"</xsl:text>@type" : "<xsl:value-of select="namespace-uri()"/><xsl:text>"</xsl:text>
                <xsl:text>}</xsl:text>

                <xsl:if test="cldfeeds:getSiblingCount(.) &gt; 0">, </xsl:if>
            </xsl:when>
            
            <!-- handle nodes that have no child nodes -->
            <xsl:when test="count(./child::*) = 0">
                <xsl:text>"</xsl:text><xsl:value-of select="local-name()"/>" : "<xsl:apply-templates select="."/><xsl:text>"</xsl:text>

                <xsl:if test="cldfeeds:getSiblingCount(.) &gt; 0">, </xsl:if>
            </xsl:when>
        </xsl:choose>
    </xsl:template>
    
    <!-- A template to match atom:content with type="application/xml". For such
         atom:content node, we want to skip printing the attribute (see CF-154).
         This does assume that 'type' is the only attribute allowed under atom:content.
    -->
    <xsl:template match="atom:content[@type='application/xml']" mode="obj-content">
        <xsl:text>{</xsl:text>
        <xsl:apply-templates select="./*" mode="others" />
        <xsl:text>}</xsl:text>
    </xsl:template>
   
    <!-- A template to match svcdoc:workspace tha contains an atom:link element.
         This is a special handling of the Preferences Service endpoint that is 
         listed in our Feeds Catalog (see CF-477). 
    -->
    <xsl:template match="svcdoc:workspace[atom:link]" mode="obj-content">
        <xsl:text>{</xsl:text>

        <xsl:call-template name="atomList">
            <xsl:with-param name="nodes" select="atom:link" as="node()*"/>
            <xsl:with-param name="printComma" select="true()"/>    
        </xsl:call-template>    

        <xsl:apply-templates select="./*[not(atom:link)]" mode="others"/>
        <xsl:text>}</xsl:text>
    </xsl:template>
 
    <!-- A template that handles printing the JSON output of an XML that gets 
         transformed into one single JSON object. This handles the printing
         of non-string values for certain elements.
    -->
    <xsl:template match="*" mode="obj-content">
        <xsl:text>{</xsl:text>
        <xsl:call-template name="printType">
            <xsl:with-param name="localName"><xsl:value-of select="local-name(.)"/></xsl:with-param>
            <xsl:with-param name="ns"><xsl:value-of select="namespace-uri(.)"/></xsl:with-param>
        </xsl:call-template>    
        <xsl:choose>
            <xsl:when test="cldfeeds:isRaxSchemaNode(local-name(), namespace-uri())">
                <xsl:variable name="namespace" select="string(namespace-uri())" as="xs:string"/>
                <xsl:variable name="version"   select="@version"                as="xs:string"/>

                <xsl:variable name="nonStringAttrs"
                              select="tokenize(
                                  $nonStringAttrsList/cf-nsattrs:schema[string(@key) = $namespace and string(@version) = $version]/cf-nsattrs:attributes/text(),
                                  ',')"/>
                <xsl:apply-templates select="@*" mode="raxAttr">
                    <xsl:with-param name="namespace" select="$namespace"/>
                    <xsl:with-param name="version" select="$version"/>
                    <xsl:with-param name="nonStringAttrs" select="$nonStringAttrs"/>
                </xsl:apply-templates>
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates select="@*" mode="normalAttr" />
            </xsl:otherwise>
        </xsl:choose>

        <xsl:if test="count(@*) &gt; 0 and (count(child::*) &gt; 0 or text())">, </xsl:if>
        
        <!-- here we recurse to child objects other than links and categories -->
        <xsl:apply-templates select="./*" mode="others"/>
        
        <xsl:if test="count(child::*) = 0 and text() and not(@*)">
            <xsl:text>"</xsl:text><xsl:value-of select="local-name()"/>" : "<xsl:value-of select="text()"/><xsl:text>"</xsl:text>
        </xsl:if>
        
        <!-- handles element that has both text child node and attributes -->
        <xsl:if test="count(child::*) = 0 and text() and @*">
            <xsl:text>"@text" : </xsl:text>
            <xsl:choose>
                <xsl:when test="local-name() = 'content' and
                                    namespace-uri() = 'http://www.w3.org/2005/Atom' and
                                    @type = 'application/json'">
                    <!-- CF-1554: this handles the mixed-content, JSON-in-XML.
                             Just go ahead and print the text() value here.
                        -->
                    <xsl:value-of select="text()"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:text>"</xsl:text>
                    <xsl:call-template name="removeBreaks">
                        <xsl:with-param name="pText" select="text()"/>
                    </xsl:call-template>
                    <xsl:text>"</xsl:text>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
        <xsl:text>}</xsl:text>
        <xsl:if test="position() &lt; last()">, </xsl:if>
    </xsl:template>
    
    <!-- A template that handles the printing of 'link' and 'category'
         elements. Link and category elements are handled in a special
         way. The elements can appear anywhere inside <entry> or 
         <feed> and they don't necessarily appear together next
         to each other. So
    -->
    <xsl:template name="atomList">
        <xsl:param name="nodes"/>
        <xsl:param name="printComma"/>
        
        <xsl:if test="count($nodes) &gt; 0">           
            <xsl:text>"</xsl:text><xsl:value-of select="$nodes[1]/local-name()"/><xsl:text>" : [</xsl:text>
            <xsl:for-each select="$nodes">
                <xsl:text>{</xsl:text>
                <xsl:apply-templates select="@*" mode="normalAttr" />
                <xsl:text>}</xsl:text>
                <xsl:if test="position() ne last()"><xsl:text>, </xsl:text></xsl:if>
            </xsl:for-each>
            <xsl:text>]</xsl:text>
            <xsl:if test="boolean($printComma)">
                <xsl:text>,</xsl:text>
            </xsl:if>
        </xsl:if>
    </xsl:template>

    <!-- A template to handle the printing of XML attributes
         to normal JSON string-value pairs
    -->
    <xsl:template match="@*" mode="normalAttr">

        <!-- JSON key -->
        <xsl:text>"</xsl:text><xsl:value-of select="name()"/><xsl:text>" : </xsl:text>

        <!-- JSON value, either string or non-string -->
        <xsl:choose>
            <xsl:when test="cldfeeds:isNonString( parent::node()/namespace-uri(), parent::node()/local-name(), local-name(.) )">
                <xsl:value-of select="."/>
            </xsl:when>
            <xsl:otherwise>
               <xsl:text>"</xsl:text><xsl:value-of select="."/><xsl:text>"</xsl:text>
            </xsl:otherwise>
        </xsl:choose>

        <xsl:if test="position() &lt; last()">,</xsl:if>
    </xsl:template>

    <!-- A template to handle Rackspace specific attributes. Specifically,
        attributes of the 'event' and 'product' elements. They need
        special handling because we are printing the non-string attributes
        without surrounding them with quotes. Which attributes are non-string
        are listed in the nonStringAttrs.xsl xsl:include-ed in this file.
    -->
    <xsl:template match="@*" mode="raxAttr">
        <xsl:param name="namespace"/>
        <xsl:param name="version"/>
        <xsl:param name="nonStringAttrs"/>
        
        <xsl:text>"</xsl:text><xsl:value-of select="name()"/><xsl:text>" : </xsl:text>

        <xsl:variable name="mypath">
            <xsl:call-template name="buildPathToProduct"/>
        </xsl:variable>

        <xsl:variable name="isNonString" as="xs:boolean">
            <xsl:value-of select="cldfeeds:isNonStringProduct($mypath, $namespace, $version, $nonStringAttrs)"></xsl:value-of>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$isNonString">
                <xsl:value-of select="."/>    
            </xsl:when>
            <xsl:otherwise>
                 <xsl:text>"</xsl:text><xsl:value-of select="."/><xsl:text>"</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:if test="position() &lt; last()">,</xsl:if>
    </xsl:template>
    
    <!-- A template that removes line breaks from the value of TEXT node. 
    -->
    <xsl:template match="node/@TEXT | text()" name="removeBreaks">
        <xsl:param name="pText" select="normalize-space(.)"/>
        <xsl:choose>
            <xsl:when test="not(contains($pText, '&#xA;'))"><xsl:copy-of select="$pText"/></xsl:when>
            <xsl:otherwise>
                <xsl:copy-of select="normalize-space(translate(translate($pText, '&#xD;',''),'&#xA;', ''))"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <!-- This function determines if the attribute of a particular element with
         a particular 'namespace' is defined in the product schemas as numeric
         or boolean type (i.e: non-string). This is important in JSON because
         numeric and boolean are not quoted.
    -->
    <xsl:function name="cldfeeds:isNonStringProduct" as="xs:boolean">
        <xsl:param name="attributePath"/>
        <xsl:param name="namespace"/>
        <xsl:param name="version"/>
        <xsl:param name="nonStringAttrs"/>
        
        <xsl:choose>
            <xsl:when test="$attributePath ne '' and exists($nonStringAttrs)">
                <xsl:value-of select="exists(index-of($nonStringAttrs, $attributePath))"/>                
            </xsl:when> 
            <xsl:otherwise>
                <xsl:value-of select="false()"/>    
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!--
       Determines if the CADF attachment node value is a non-string or not.
    -->
    <xsl:function name="cldfeeds:isNonString" as="xs:boolean">
        <xsl:param name="ns"/>
        <xsl:param name="node"/>
        <xsl:param name="attr"/>

        <xsl:variable name="nonStringAttrs">
            <cldfeeds:nonStrings>
                <cldfeeds:nonString namespace="http://schemas.dmtf.org/cloud/audit/1.0/event" node="reason" attribute="reasonCode"/>
            </cldfeeds:nonStrings>
        </xsl:variable>

        <xsl:value-of select="exists( $nonStringAttrs//cldfeeds:nonString[@namespace = $ns and
                               @node = $node and
                               @attribute= $attr])"/>
    </xsl:function>


    <xsl:template match="@*" name="buildPathToProduct">
        <!-- This is not perfect because it assumes that we only have 2 levels elements:
               *) product
               *) product/attributeGroup
             To make this work for any levels, I would have to recursively build the path.
             I'm not sure it's worth doing at this point.
             
             This assumption needs to be held the same in the XSLT that generates the
             list of non-string attributes.
        -->
            <xsl:if test="parent::node()/local-name() eq 'product'">
                <xsl:value-of select="concat('product/@', local-name())"/>
            </xsl:if>
            <xsl:if test="parent::node()/parent::node()/local-name() eq 'product'">
                <xsl:value-of select="concat(parent::node()/local-name(), '@', local-name())"/>
            </xsl:if>
    </xsl:template>

    <!--
      Print the @type attribute if the node matches the included nodes & is not within the
      excluded namespaces.
    -->
    <xsl:template name="printType">
        <xsl:param name="localName"/>
        <xsl:param name="ns"/>
        
        <!-- nodes we must print namespace in '@type' string -->
        <xsl:variable name="includeNodes" select="tokenize('feed entry event product error eventError', ' ')"/>
        <!-- except for these namespaces, e.g., event -->
        <xsl:variable name="excludeNS" 
		      select="tokenize('http://schemas.dmtf.org/cloud/audit/1.0/event', ' ')"/>

        <xsl:if test="exists(index-of($includeNodes, $localName)) 
            and not(exists(index-of($excludeNS, $ns)))">
            <xsl:text>"@type": "</xsl:text><xsl:value-of select="namespace-uri()"/><xsl:text>", </xsl:text>
        </xsl:if>        
    </xsl:template>

    <!--
      Return true if the node is defined as a part of hte product schema.  This consists when the node matches the
      included nodes & is not within the execluded namespaces.
    -->
    <xsl:function name="cldfeeds:isRaxSchemaNode" as="xs:boolean">
        <xsl:param name="localName"/>
        <xsl:param name="ns"/>

        <!-- rax schema nodes must have @version attribute -->
        <xsl:variable name="includeNodes" select="tokenize('event product', ' ')"/>
        <xsl:variable name="excludeNS"
                      select="tokenize('http://schemas.dmtf.org/cloud/audit/1.0/event', ' ')"/>

        <xsl:choose>
            <xsl:when test="exists(index-of($includeNodes, $localName))
			  and not(exists(index-of($excludeNS, $ns)))">
                <xsl:value-of select="true()"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="false()"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!--
      Return true if the node is of the form:
      localName = attachments
      namespace = http://schemas.dmtf.org/cloud/audit/1.0/event
    -->
    <xsl:function name="cldfeeds:isCadfAttachments" as="xs:boolean">
        <xsl:param name="localName"/>
        <xsl:param name="ns"/>

        <xsl:choose>
            <xsl:when test="$localName = 'attachments' and $ns = 'http://schemas.dmtf.org/cloud/audit/1.0/event'">
                <xsl:value-of select="true()"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="false()"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!--
      Return true if the node is of the form:
      localName = attachment
      namespace = http://schemas.dmtf.org/cloud/audit/1.0/event
    -->
    <xsl:function name="cldfeeds:isCadfAttachment" as="xs:boolean">
        <xsl:param name="localName"/>
        <xsl:param name="ns"/>

        <xsl:choose>
            <xsl:when test="$localName = 'attachment' and $ns = 'http://schemas.dmtf.org/cloud/audit/1.0/event'">
                <xsl:value-of select="true()"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="false()"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!--
      Return true if the node is of the form:
      localName = archive
      namespace = http://purl.org/syndication/history/1.0
    -->
    <xsl:function name="cldfeeds:isArchiveNode" as="xs:boolean">
        <xsl:param name="localName"/>
        <xsl:param name="ns"/>

        <xsl:choose>
            <xsl:when test="$localName = 'archive' and $ns = 'http://purl.org/syndication/history/1.0'">
                <xsl:value-of select="true()"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="false()"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>
    
    <!--
      Return sibling count of the currentNode 
      excluding atom:category, atom:entry, atom:link nodes
    -->
    <xsl:function name="cldfeeds:getSiblingCount" as="xs:integer">
        <xsl:param name="currentNode"/>
        <xsl:variable name="sibs"
            select="$currentNode/following-sibling::*[not(self::atom:category) and
            not(self::atom:entry) and
            not(self::atom:link)]"/>
        <xsl:value-of select="count($sibs)"></xsl:value-of>
    </xsl:function>
    
</xsl:stylesheet>
