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
  XLIMSizeAndType.xslt
  Copyright (c) 2005 Xilinx Inc.
  One step in the transformation from the XLIM format into SLIM format.
  Propagates the size and typeName attribute from output ports to the consuming
  input ports.  Also from state vars to their accessors.
  
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

  <xsl:variable name="assignsSized">
    <xsl:apply-templates mode="sizeAssigns"/>
  </xsl:variable>
  
  <!-- Copy typeName attributes from source bus to target port -->
  <xsl:apply-templates select="$assignsSized" mode="copyTypeName"/>
  
</xsl:template>

<xsl:template match="port[@dir='in']" mode="sizeAssigns">
  <xsl:variable name="port" select="."/>
  
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>

    <!-- ensure that the size of the input port matches the size of the
         state var being assigned to, rounding up the same way we do for the
         state vars if there is already a size attribute.  But only do this for
         the data input port. -->
    <xsl:if test="../@kind='assign'">
      <xsl:variable name="op" select=".."/>
      <xsl:choose>
        <xsl:when test="count(../port[@dir='in']) = 1 or (@tag = ../port[@dir='in'][2]/@tag)">
          <!-- Port is data input to assign, take size from target var -->
          <xsl:variable name="source" select="ancestor::design//stateVar[@name=$op/@target]"/>
          <xsl:variable name="sourceSize" select="$source//initValue[@typeName!='List'][1]/@size"/>
          <xsl:attribute name="size"><xsl:value-of select="$sourceSize"/></xsl:attribute>
        </xsl:when>
        <xsl:when test="count(../port[@dir='in']) = 2 and (@tag != ../port[@dir='in'][2]/@tag)">
          <!-- Address input to 2 input assign.  Take size from source bus. -->
          <xsl:apply-templates mode="copyTypeName" select="."/>
        </xsl:when>
      </xsl:choose>
    </xsl:if>
    <xsl:apply-templates mode="sizeAssigns"/>
  </xsl:element>

</xsl:template>

<xsl:template match="*" mode="sizeAssigns">
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
    <xsl:apply-templates mode="sizeAssigns"/>
  </xsl:element>
</xsl:template>

<xsl:template match="port[@dir='in']" mode="copyTypeName">
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
    <!-- Now, grab the typeName from the source bus or var -->
    <!-- Starting at the root module in our ancestor tree, look for any descendent output port whose name matches our source -->
    <xsl:variable name="source" select="@source"/>
    <xsl:variable name="sourceBus" select="./ancestor::module[last()]//port[@dir='out'][@name=$source]"/>
    <xsl:variable name="sourceVar" select="/design/stateVar[@name=$source]"/>

    <xsl:if test="not(@typeName)">
      <xsl:choose>
        <xsl:when test="count($sourceBus) > 0">
          <xsl:attribute name="typeName"><xsl:value-of select="$sourceBus[1]/@typeName"/></xsl:attribute>
        </xsl:when>
        <xsl:when test="count($sourceVar) > 0">
          <xsl:attribute name="typeName"><xsl:value-of select="$sourceVar[1]/initValue/@typeName"/></xsl:attribute>
        </xsl:when>
        <xsl:when test="@kind='control'">
          <xsl:attribute name="typeName">bool</xsl:attribute>
        </xsl:when>
        <xsl:otherwise>
          <xsl:message>Error!  Source (for typename) not a bus in the current module or a state var!  Port source: <xsl:value-of select="@source"/></xsl:message>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:if>
    <xsl:if test="not(@size)">
      <xsl:choose>
        <xsl:when test="count($sourceBus) > 0">
          <xsl:attribute name="size"><xsl:value-of select="$sourceBus[1]/@size"/></xsl:attribute>
        </xsl:when>
        <xsl:when test="count($sourceVar) > 0">
          <xsl:attribute name="size"><xsl:value-of select="$sourceVar[1]/initValue/@size"/></xsl:attribute>
        </xsl:when>
        <xsl:when test="@kind='control'">
          <xsl:attribute name="size">1</xsl:attribute>
        </xsl:when>
        <xsl:otherwise>
          <xsl:message>Error!  Source (for size) not a bus in the current module or a state var!  Port source: <xsl:value-of select="@source"/></xsl:message>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:if>
    
    <xsl:apply-templates mode="copyTypeName"/>
  </xsl:element>
</xsl:template>

<xsl:template match="*" mode="copyTypeName">
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
    <xsl:apply-templates mode="copyTypeName"/>
  </xsl:element>
</xsl:template>

</xsl:stylesheet>