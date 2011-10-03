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
  XLIMProcessPHI.xslt
  Copyright (c) 2005 Xilinx Inc.
  One step in the transformation from the XLIM format into SLIM format.
  ensure that the order of the PHI ports is then, else.  otherwise
  the parser will fail b/c it expects dependency 0 to be the true
  path and dependency 1 to be the false path for branches
  
  Remove the PHI elements by creating one dependency for each PHI port
  using group # to reflect the functionality.
  
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

<xsl:template match="/">

  <xsl:variable name="orderedPHI">
    <xsl:apply-templates mode="orderPHI"/>
  </xsl:variable>

  <xsl:apply-templates select="$orderedPHI" mode="flattenPHI"/>
  
</xsl:template>

<xsl:template match="PHI" mode="orderPHI">
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>

    <xsl:choose>
      <xsl:when test="parent::module[@kind='if']">
        <!-- in case we have notes or some such -->
        <xsl:apply-templates select="./*[not(self::port)]" mode="orderPHI"/>
        
        <xsl:variable name="context" select="."/>
        <xsl:variable name="port1source" select="./port[@dir='in'][1]/@source"/>
        <xsl:choose>
          <xsl:when test="ancestor::module/module[@kind='then']//port[@dir='out'][@source=$port1source]">
            <xsl:apply-templates select="./port[@dir='in'][1]" mode="orderPHI"><xsl:with-param name="qual">then</xsl:with-param></xsl:apply-templates>
            <xsl:apply-templates select="./port[@dir='in'][2]" mode="orderPHI"><xsl:with-param name="qual">else</xsl:with-param></xsl:apply-templates>
          </xsl:when>
          <xsl:otherwise>
            <xsl:apply-templates select="./port[@dir='in'][2]" mode="orderPHI"><xsl:with-param name="qual">then</xsl:with-param></xsl:apply-templates>
            <xsl:apply-templates select="./port[@dir='in'][1]" mode="orderPHI"><xsl:with-param name="qual">else</xsl:with-param></xsl:apply-templates>
          </xsl:otherwise>
        </xsl:choose>
        <xsl:apply-templates select="./port[@dir='out']" mode="orderPHI"/>
      </xsl:when>
      <xsl:otherwise>
        <!-- simply preserve it 'as is' -->
        <xsl:apply-templates mode="orderPHI"/>
      </xsl:otherwise>
    </xsl:choose>
    
    
  </xsl:element>
</xsl:template>

<xsl:template match="*" mode="orderPHI">
  <xsl:param name="qual" select="_not_valid_"/>
  
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
    <xsl:if test="$qual != '_not_valid_'">
      <xsl:attribute name="qualifier"><xsl:value-of select="$qual"/></xsl:attribute>
    </xsl:if>
    <xsl:apply-templates mode="orderPHI"/>
  </xsl:element>
</xsl:template>


<!-- ********************************************************************* 
   ********* Templates for 'flattenPHI'.
   ********* Elimination of the PHI element.  Replacing it with multiple
   ********* dependency groups on a single port.
   ********************************************************************* -->
<xsl:template match="PHI" mode="flattenPHI">
  <!-- Do nothing so that the PHI elements are removed.  The flattening
     will happen during the dependency processing. -->
</xsl:template>

<xsl:template match="dependency" mode="flattenPHI">
  <xsl:variable name="scope" select="."/>
  <!-- For any dep with source from a PHI, replace the dep with a dep for each PHI input.-->
  <xsl:variable name="source" select="@source"/>
  <xsl:variable name="target" select="@dest"/>

  <xsl:variable name="sourcePHI" select="../PHI/port[@dir='out' and @name=$source]"/> 
  <xsl:variable name="targetPHI" select="../PHI/port[@name=$target]"/>

  <!-- 3 options.
     Dependency feeds a PHI - drop it.
     Dependency is from a PHI - transform to multiple deps on target
     Dependency is not related to a PHI - leave it alone.
  -->
  <xsl:choose>
  <!-- <xsl:when test="count($targetPHI) > 0 and ../@style='if'"> -->
  <xsl:when test="count($targetPHI) > 0">
    <!-- do nothing so that the dependency is dropped -->
  </xsl:when>
  <!-- <xsl:when test="count($sourcePHI) > 0 and ../@style='if'"> -->
  <xsl:when test="count($sourcePHI) > 0"> <!-- a list of PHI output ports -->
    <xsl:for-each select="$sourcePHI">
    <!-- <xsl:variable name="phiPorts" select="./port[@dir='in']"/>-->
    <xsl:variable name="phiPorts" select="../port[@dir='in']"/>
    <xsl:for-each select="$phiPorts">
      <!-- Find the dependency (which we are deleting) which drives the
         specified PHI port. This will work so long as we dont have PHI's
         driving another PHI and in the reverse document order. -->
      <xsl:variable name="phiPortName" select="@name"/>
      <xsl:variable name="phiPortSource" select="$scope/../dependency[@dest=$phiPortName]/@source"/>
      <xsl:call-template name="makeDep">
      <xsl:with-param name="to"><xsl:value-of select="$target"/></xsl:with-param>
      <xsl:with-param name="from"><xsl:value-of select="$phiPortSource"/></xsl:with-param>
      <xsl:with-param name="group"><xsl:value-of select="position()-1"/></xsl:with-param>
      <xsl:with-param name="qual"><xsl:value-of select="@qualifier"/></xsl:with-param>
      </xsl:call-template>
    </xsl:for-each>
    </xsl:for-each>
  </xsl:when>

  <xsl:otherwise>
    <!-- Copy it as-is -->
    <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
    <xsl:apply-templates mode="flattenPHI"/>
    </xsl:element> 
  </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="*" mode="flattenPHI">
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
    <xsl:apply-templates mode="flattenPHI"/>
  </xsl:element> 
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

</xsl:stylesheet>