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
  XLIMFixNames.xslt
  Copyright (c) 2005 Xilinx Inc.
  One step in the transformation from the XLIM format into SLIM format.
  Give each element an exit tag, wrapping the output ports (if any).

  2005-12-15 Creation
-->
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xd="http://www.pnp-software.com/XSLTdoc"
  version="1.1">
<xsl:output method="xml"/>

<!-- Tells this transformation to ignore white space in all element of the source xml -->
<xsl:strip-space elements="*"/>

<xsl:template match="*">
  <!-- Copy the element in its entirety, but if there are any output port
     elements then wrap that in an element tag -->
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
  
  <xsl:apply-templates select="*[not(self::port[@dir='out'])]"/>
  <xsl:variable name="needingExit" select="*[self::port[@dir='out']]"/>
  <xsl:if test="count($needingExit) > 0 or self::operation | self::module">
    <xsl:element name="exit">
    <xsl:attribute name="kind">done</xsl:attribute>
    <xsl:apply-templates select="$needingExit"/>
    </xsl:element>
  </xsl:if>
  
  </xsl:element>
</xsl:template>

<xsl:template match="module[@kind='loopBody']">
  <!-- Copy the element in its entirety, but if there are any output port
     elements then wrap that in an element tag -->
  <xsl:variable name="loopBody" select="."/>
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
  
  <xsl:apply-templates select="*[not(self::port[@dir='out'])]"/>

  <!-- All output ports of the loop body feed either/both: An input of the same
     loop body (feedback) or an output of the loop (done).  Group the ports based on
     their destination: feedback, done, or both.
     This var has an element with port name and type=feedback|done|both.
  -->
  <xsl:variable name="portTypes">
    <xsl:for-each select="*[self::port[@dir='out']]">
    <xsl:variable name="thePort" select="."/>
    <xsl:variable name="allDeps" select="$loopBody/../dependency[@source=$thePort/@name]"/>
    <xsl:variable name="fbDeps">
      <xsl:for-each select="$allDeps">
      <xsl:variable name="target" select="@dest"/>
      <xsl:if test="$loopBody/port[@dir='in' and @name=$target]">
        <xsl:copy-of select="."/>
      </xsl:if>
      </xsl:for-each>
    </xsl:variable>
    
    <xsl:variable name="doneDeps">
      <xsl:for-each select="$allDeps">
      <xsl:variable name="target" select="@dest"/>
      <xsl:if test="$loopBody/../port[@dir='out' and @name=$target]">
        <xsl:copy-of select="."/>
      </xsl:if>
      </xsl:for-each>
    </xsl:variable>

    <xsl:choose>
      <xsl:when test="count($fbDeps/*) > 0 and count($doneDeps/*) > 0">
      <xsl:element name="portType">
        <xsl:attribute name="name"><xsl:value-of select="$thePort/@name"/></xsl:attribute>
        <xsl:attribute name="type">both</xsl:attribute>
      </xsl:element>
      </xsl:when>
      <xsl:when test="count($fbDeps/*) > 0 and count($doneDeps/*) = 0">
      <xsl:element name="portType">
        <xsl:attribute name="name"><xsl:value-of select="$thePort/@name"/></xsl:attribute>
        <xsl:attribute name="type">feedback</xsl:attribute>
      </xsl:element>
      </xsl:when>
      <xsl:when test="count($fbDeps/*) = 0 and count($doneDeps/*) > 0">
      <xsl:element name="portType">
        <xsl:attribute name="name"><xsl:value-of select="$thePort/@name"/></xsl:attribute>
        <xsl:attribute name="type">done</xsl:attribute>
      </xsl:element>
      </xsl:when>
    </xsl:choose>
    </xsl:for-each>
  </xsl:variable>

  <xsl:element name="exit">
    <xsl:attribute name="kind">feedback</xsl:attribute>
    <xsl:for-each select="$portTypes/*[@type='feedback']">
    <xsl:variable name="currentPortName" select="@name"/>
    <xsl:apply-templates select="$loopBody/port[@name=$currentPortName]"/>
    </xsl:for-each>
  </xsl:element>
  
  <xsl:element name="exit">
    <xsl:attribute name="kind">done</xsl:attribute>
    <xsl:for-each select="$portTypes/*[@type='done']">
    <xsl:variable name="currentPortName" select="@name"/>
    <xsl:apply-templates select="$loopBody/port[@name=$currentPortName]"/>
    </xsl:for-each>
  </xsl:element>

  <xsl:if test="count($portTypes/*[@type='both']) > 0">
    <xsl:message>ERROR!!!! No support (yet) for a loop output and feedback sharing ports</xsl:message>
		<xsl:element name="both">
		<xsl:for-each select="$portTypes/*[@type='both']">
			<xsl:message> port <xsl:value-of select="@name"/></xsl:message>
			<xsl:variable name="currentPortName" select="@name"/>
			<xsl:apply-templates select="$loopBody/port[@name=$currentPortName]"/>
		</xsl:for-each>
		</xsl:element>
	</xsl:if>
  
  </xsl:element>
  
</xsl:template>

</xsl:stylesheet>