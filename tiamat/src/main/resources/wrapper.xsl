<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:event="http://docs.rackspace.com/core/event"
                xmlns:atom="http://www.w3.org/2005/Atom"
                xmlns:httpx="http://openrepose.org/repose/httpx/v1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://wadl.dev.java.net/2009/02"
                version="2.0">

    <xsl:import href="rm_private_attrs_for_obs.xsl"/>
    <xsl:output method="xml" indent="yes" encoding="UTF-8" omit-xml-declaration="yes"/>

    <xsl:template match="/" >
        <xsl:apply-templates mode="rm_priv"/>
    </xsl:template>

</xsl:stylesheet>