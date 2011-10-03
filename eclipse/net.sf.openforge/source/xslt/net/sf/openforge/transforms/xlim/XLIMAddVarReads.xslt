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
  XLIMAddVarReads.xslt
  Copyright (c) 2005 Xilinx Inc.
  One step in the transformation from the XLIM format into a SLIM format.
  For any port which is sourced by a state var, create a var-ref to
  represent the read of that var

  2005-12-15 Creation
-->
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xd="http://www.pnp-software.com/XSLTdoc"
  version="1.1">
<xsl:output method="xml"/>

<!-- Tells this transformation to ignore white space in all element of the source xml -->
<xsl:strip-space elements="*"/>

<xsl:template match="port[@dir='in']">

  <xsl:variable name="thePort" select="."/>
  <!-- Preserve the existing element information -->  

  <xsl:variable name="sourceName" select="@source"/>
  <xsl:variable name="originalPort" select="."/>
  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
    <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>

  <xsl:variable name="stateVars" select="ancestor::design/stateVar[@name=$sourceName]"/>
  <!-- If the port is sourced from a state var ref then move its source to the created var ref operation -->
  <xsl:if test="count($stateVars) > 0">
    <!-- This name MUST match the out name of the new var_ref created in the operation template -->
    <xsl:attribute name="source"><xsl:value-of select="concat($originalPort/@source,'_read_',$originalPort/@tag)"/></xsl:attribute>
  </xsl:if>
  
    <xsl:apply-templates/>
  </xsl:element> 

</xsl:template>

<!-- Create a new var_ref operation for any port sourced from a state var -->
<xsl:template match="operation">

  <xsl:for-each select="./port[@dir='in']">
  <xsl:variable name="sourceName" select="@source"/>
  <xsl:variable name="originalPort" select="."/>
  
  <xsl:variable name="stateVars" select="ancestor::design/stateVar[@name=$sourceName]"/>
  <xsl:if test="count($stateVars) > 0">
    <xsl:variable name="outName"><xsl:value-of select="concat($originalPort/@source,'_read_',$originalPort/@tag)"/></xsl:variable>
    <xsl:element name="operation">
    <xsl:attribute name="kind">var_ref</xsl:attribute>
    <xsl:attribute name="name"><xsl:value-of select="$sourceName"/></xsl:attribute>
    <xsl:attribute name="tag">FIXME</xsl:attribute>
    <port name="{$outName}" dir="out" size="{$originalPort/@size}" source="{$outName}" typeName="{$originalPort/@typeName}" />
    </xsl:element>
  </xsl:if>
  </xsl:for-each>

  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
    <xsl:apply-templates/>
  </xsl:element> 
  
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