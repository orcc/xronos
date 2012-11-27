<!-- 
 * Copyright 2002-2009  Xilinx Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 -->

<!--
  XLIMBuildControl.xslt
  Copyright (c) 2005 Xilinx Inc.
  One step in the transformation from the XLIM format into SLIM format.
  Explicit control dependencies are represented by the go/done attribute
  on any module or operation.  These are now turned into a go port and/or
  done bus on the module/operation with the appropriate source/target.

  2005-12-15 Creation
-->
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xd="http://www.pnp-software.com/XSLTdoc"
  version="1.1">
<xsl:output method="xml"/>

<!-- Tells this transformation to ignore white space in all element of the source xml -->
<xsl:strip-space elements="*"/>

<xd:doc></xd:doc>
<xsl:template match="module | operation">
  
  <!-- If the go is an internal port name, create a pin read as a peer to this module -->
  <!-- Note, now that we use a taskCall I do not believe that this mechanism is ever used -->
  <xsl:variable name="goSource" select="@go"/>
  <xsl:variable name="doneSource" select="@done"/>
  <xsl:variable name="goPort" select="//internal-port[@name=$goSource]"/>
  <xsl:variable name="donePort" select="//internal-port[@name=$doneSource]"/>
  <xsl:variable name="uniqueGoName" select="concat('pz', generate-id())"/>
  <xsl:variable name="uniqueDoneName" select="concat('dz', generate-id())"/>
  
  <xsl:variable name="goImplementation">
    <xsl:choose>
      <xsl:when test="count($goPort) > 0">
        <xsl:element name="sourceName"><xsl:value-of select="$uniqueGoName"/></xsl:element>
        <xsl:element name="operation">
          <xsl:attribute name="kind">pinRead</xsl:attribute>
          <xsl:attribute name="portName"><xsl:value-of select="$goSource"/></xsl:attribute>
          <xsl:attribute name="tag">FIXME</xsl:attribute>
          <xsl:element name="port">
            <xsl:attribute name="name"><xsl:value-of select="$uniqueGoName"/></xsl:attribute>
            <xsl:attribute name="dir">out</xsl:attribute>
            <xsl:attribute name="size"><xsl:value-of select="$goPort[1]/@size"/></xsl:attribute>
            <xsl:attribute name="source"><xsl:value-of select="$uniqueGoName"/></xsl:attribute>
            <xsl:attribute name="typeName"><xsl:value-of select="$goPort[1]/@typeName"/></xsl:attribute>
            <xsl:attribute name="tag">FIXME</xsl:attribute>
          </xsl:element>
        </xsl:element>
      </xsl:when>
      <xsl:otherwise>
        <xsl:element name="sourceName"><xsl:value-of select="$goSource"/></xsl:element>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  
  <xsl:copy-of select="$goImplementation/operation"/>
  
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
  
  <xsl:for-each select="@*">
    <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
  </xsl:for-each>
  
  <xsl:if test="string-length(@go) > 0">
    <xsl:element name="port">
    <xsl:attribute name="name">FIXME</xsl:attribute>
    <xsl:attribute name="dir">in</xsl:attribute>
    <xsl:attribute name="size">1</xsl:attribute>
    <xsl:attribute name="source"><xsl:value-of select="$goImplementation/sourceName"/></xsl:attribute>
    <xsl:attribute name="typeName">bool</xsl:attribute>
    <xsl:attribute name="kind">control</xsl:attribute>
    <xsl:attribute name="tag">FIXME</xsl:attribute>
    </xsl:element>
  </xsl:if>
  
  <xsl:apply-templates/>
  
  <xsl:if test="string-length(@done) > 0">
    <xsl:element name="port">
    <xsl:attribute name="name"><xsl:value-of select="$uniqueDoneName"/></xsl:attribute>
    <xsl:attribute name="dir">out</xsl:attribute>
    <xsl:attribute name="size">1</xsl:attribute>
    <xsl:attribute name="source"><xsl:value-of select="$uniqueDoneName"/></xsl:attribute>
    <xsl:attribute name="typeName">bool</xsl:attribute>
    <xsl:attribute name="kind">control</xsl:attribute>
    <xsl:attribute name="tag">FIXME</xsl:attribute>
    </xsl:element>
  </xsl:if>
  
  </xsl:element> 
  
  <!-- If there was a 'done' specified and it targets a pin, then insert a pinwrite as a peer to the module -->
  <xsl:if test="count($donePort) > 0">
  <xsl:element name="operation">
    <xsl:attribute name="kind">pinWrite</xsl:attribute>
    <xsl:attribute name="portName"><xsl:value-of select="$doneSource"/></xsl:attribute>
    <xsl:attribute name="tag">FIXME</xsl:attribute>
    <xsl:element name="port">
    <xsl:attribute name="name">FIXME</xsl:attribute>
    <xsl:attribute name="dir">in</xsl:attribute>
    <xsl:attribute name="size"><xsl:value-of select="$donePort[1]/@size"/></xsl:attribute>
    <xsl:attribute name="source"><xsl:value-of select="$uniqueDoneName"/></xsl:attribute>
    <xsl:attribute name="typeName"><xsl:value-of select="$goPort[1]/@typeName"/></xsl:attribute>
    <xsl:attribute name="tag">FIXME</xsl:attribute>
    </xsl:element>
  </xsl:element>
  </xsl:if>
  
</xsl:template>

<xsl:template match="*">
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
    <xsl:apply-templates/>
  </xsl:element> 
</xsl:template>

</xsl:stylesheet>