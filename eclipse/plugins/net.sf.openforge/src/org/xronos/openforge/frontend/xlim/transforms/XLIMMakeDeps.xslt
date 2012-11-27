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
  XLIMMakePortNames.xslt
  Copyright (c) 2005 Xilinx Inc.
  One step in the transformation from the XLIM format into SLIM format.
  Create dependencies from output ports to input ports at each hierarchical
  level.
  
  Author: IDM
  2005-12-15 Creation
-->
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xd="http://www.pnp-software.com/XSLTdoc"
  version="1.1">
<xsl:output method="xml"/>

<!-- Tells this transformation to ignore white space in all element of the source xml -->
<xsl:strip-space elements="*"/>

<xsl:template match="module | design">
  <xsl:variable name="scope" select="."/>

  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>

  <xsl:apply-templates/>

  <xsl:call-template name="standardModuleDeps">
    <xsl:with-param name="scope" select="$scope"/>
  </xsl:call-template>

  <xsl:if test="self::module[@kind='loopBody']">
    <!-- case 4: in a loop body we may have a module input directly driving
       a module output. -->
    <xsl:for-each select="$scope/port[@dir='out']">
    <xsl:variable name="source" select="@source"/>
    <xsl:variable name="toName" select="@name"/>
    <xsl:for-each select="$scope/port[@dir='in'][@source=$source]">
      <xsl:variable name="fromName" select="@name"/>
      <xsl:call-template name="makeDep">
      <xsl:with-param name="from" select="$fromName"/>
      <xsl:with-param name="to" select="$toName"/>
      <xsl:with-param name="scope" select="$scope"/>
      </xsl:call-template>
    </xsl:for-each>
    </xsl:for-each>
  </xsl:if>
  
  </xsl:element>
</xsl:template>

<xsl:template name="standardModuleDeps">
  <xsl:param name="scope"/>
  
  <!-- Create dependencies between all matching ports.  There are 3 possible
     relationships between ports.
     1. They are both an operation's port.  ie their parents are siblings.
     2. The consumer (dir='in') is in an operation, the source is a port of
     the module.
     3. The producer (dir='out') is in an operation, the consumer is a port
     of the module.
  -->
  <!-- case 1 -->
  <xsl:for-each select="$scope/*/port[@dir='out']">
    <xsl:variable name="fromName" select="@name"/>
    <xsl:variable name="source" select="@source"/>
    <xsl:for-each select="$scope/*/port[@dir='in'][@source=$source]">
    <xsl:call-template name="makeDep">
      <xsl:with-param name="from" select="$fromName"/>
      <xsl:with-param name="to" select="@name"/>
      <xsl:with-param name="scope" select="$scope"/>
    </xsl:call-template>
    </xsl:for-each>
  </xsl:for-each>
  
  <!-- case 2 -->
  <xsl:for-each select="$scope/port[@dir='in']">
    <xsl:variable name="source" select="@source"/>
    <xsl:variable name="fromName" select="@name"/>
    <xsl:for-each select="$scope/*/port[@dir='in'][@source=$source]">
    <xsl:variable name="toName" select="@name"/>
    <xsl:call-template name="makeDep">
      <xsl:with-param name="from" select="$fromName"/>
      <xsl:with-param name="to" select="$toName"/>
      <xsl:with-param name="scope" select="$scope"/>
    </xsl:call-template>
    </xsl:for-each>
  </xsl:for-each>

  <!-- case 3 -->
  <xsl:for-each select="$scope/port[@dir='out']">
    <xsl:variable name="source" select="@source"/>
    <xsl:variable name="toName" select="@name"/>
    <xsl:for-each select="$scope/*/port[@dir='out'][@source=$source]">
    <xsl:variable name="fromName" select="@name"/>
    <xsl:call-template name="makeDep">
      <xsl:with-param name="from" select="$fromName"/>
      <xsl:with-param name="to" select="$toName"/>
      <xsl:with-param name="scope" select="$scope"/>
    </xsl:call-template>
    </xsl:for-each>
  </xsl:for-each>

</xsl:template>

<xsl:template name="makeDep">
  <xsl:param name="from"/>
  <xsl:param name="to"/>
  <xsl:param name="group" select="0"/>
  <xsl:param name="qual" select="_I_am_not_needed"/>
  <xsl:param name="scope" select="default"/>

  <xsl:choose>
  <xsl:when test="name($scope) = 'design'">
    <xsl:element name="connection">
    <xsl:attribute name="name">FIXME</xsl:attribute>
    <xsl:attribute name="source"><xsl:value-of select="$from"/></xsl:attribute>
    <xsl:attribute name="dest"><xsl:value-of select="$to"/></xsl:attribute>
    </xsl:element>
  </xsl:when>
  <xsl:otherwise>
    <xsl:element name="dependency">
<!--    <xsl:attribute name="name">FIXME</xsl:attribute>-->
    <xsl:attribute name="source"><xsl:value-of select="$from"/></xsl:attribute>
    <xsl:attribute name="dest"><xsl:value-of select="$to"/></xsl:attribute>
    <xsl:attribute name="group"><xsl:value-of select="$group"/></xsl:attribute>
    <!-- If the PHI port has no qualifier tag (eg in loop contexts) then
       $qual will be the empty string. -->
    <!-- <xsl:if test="$qual != '_I_am_not_needed'">-->
    <xsl:if test="$qual != '_I_am_not_needed' and $qual != ''">
      <xsl:attribute name="qualifier"><xsl:value-of select="$qual"/></xsl:attribute>
    </xsl:if>
    </xsl:element>
  </xsl:otherwise>
  </xsl:choose>
  
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