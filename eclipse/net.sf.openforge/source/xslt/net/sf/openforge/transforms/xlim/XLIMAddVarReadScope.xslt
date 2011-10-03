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
  XLIMAddVarReadScope.xslt
  Copyright (c) 2005 Xilinx Inc.
  One step in the transformation from the XLIM format into SLIM format.
  Annotate the var_ref operations with var-scope as an aid to parsing. 

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

<!--  <xsl:message>obsolete???</xsl:message> -->
  <xsl:apply-templates mode="addVarReadScope"/>
  
</xsl:template>

<!-- Annotate each var_ref with a var-scope to identify local vs stateVar accesses.
   This is just an aid to parsing. -->
<xsl:template match="operation[@kind='var_ref']" mode="addVarReadScope">
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
  <!-- If the name is a stateVar name, then var-scope is stateVar, local otherwise -->
  <xsl:variable name="name" select="@name"/>
  <xsl:choose>
    <xsl:when test="ancestor::design/stateVar[@name=$name]">
    <xsl:attribute name="var-scope">stateVar</xsl:attribute>
    </xsl:when>
    <xsl:otherwise>
    <xsl:attribute name="var-scope">local</xsl:attribute>
    </xsl:otherwise>
  </xsl:choose>
    <xsl:apply-templates mode="addVarReadScope"/>
  </xsl:element>
  
</xsl:template>

<xsl:template match="*" mode="addVarReadScope">
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
    <xsl:apply-templates mode="addVarReadScope"/>
  </xsl:element> 
</xsl:template>


</xsl:stylesheet>