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
  XLIMFixSelector.xslt
  Copyright (c) 2005 Xilinx Inc.
  One step in the transformation from the XLIM format into SLIM format.
  Converts the $selector element into an if/then/else construct.  The selector
  is the ternary operator.  The ports are: decision, true, false, output
  
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

<xsl:template match="operation[@kind='$selector']">

  <xsl:variable name="select" select="./port[@dir='in'][1]"/>
  <xsl:variable name="truePort" select="./port[@dir='in'][2]"/>
  <xsl:variable name="falsePort" select="./port[@dir='in'][3]"/>
  <xsl:variable name="testName" select="concat(generate-id($select), '_noop_out')"/>
  <xsl:variable name="trueName" select="concat(generate-id($truePort), '_noop_out')"/>
  <xsl:variable name="falseName" select="concat(generate-id($falsePort), '_noop_out')"/>
  
  <!-- Write the selector out as an if statement -->
  <xsl:element name="module">
    <xsl:attribute name="kind">if</xsl:attribute>

    <module kind="test" decision="{$testName}">
      <!-- create a noop for the test node -->
      <xsl:variable name="source" select="ancestor::module[last()]//port[@dir='out'][@source=$select/@source]"/>
      <operation kind="noop">
        <port dir="in" source="{$select/@source}"/>
        <port dir="out" source="{$testName}" typeName="{$source/@typeName}" size="{$source/@size}"/>
      </operation>
    </module>

    <module kind="then">
      <xsl:variable name="source" select="ancestor::module[last()]//port[@dir='out'][@source=$truePort/@source]"/>
      <operation kind="noop">
        <port dir="in" source="{$truePort/@source}"/>
        <port dir="out" source="{$trueName}" typeName="{$source/@typeName}" size="{$source/@size}"/>
      </operation>
    </module>
    
    <module kind="else">
      <xsl:variable name="source" select="ancestor::module[last()]//port[@dir='out'][@source=$falsePort/@source]"/>
      <operation kind="noop">
        <port dir="in" source="{$falsePort/@source}"/>
        <port dir="out" source="{$falseName}" typeName="{$source/@typeName}" size="{$source/@size}"/>
      </operation>
    </module>

    <PHI>
      <xsl:variable name="outPort" select="./port[@dir='out']"/>
      <port dir="in" source="{$trueName}" qualifier="then"/>
      <port dir="in" source="{$falseName}" qualifier="else"/>
      <port dir="out" source="{$outPort/@source}" typeName="{$outPort/@typeName}" size="{$outPort/@size}"/>
    </PHI>
    
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