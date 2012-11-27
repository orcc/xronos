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
  XLIMRoutePorts.xslt
  Copyright (c) 2005 Xilinx Inc.
  One step in the transformation from the XLIM format into SLIM format.
  For every input AND output port of operations, modules, etc create a
  sequence of ports at each hierarchical level such that a path exits
  from each source (output) port to each target (input) port by traversing
  ports at each hierarchical level.  Transitive closure across the ports.
  
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

<xsl:template match="module">

  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>

  <xsl:variable name="scope" select="."/>
  <xsl:variable name="scopeTag" select="@tag"/>
  <!--
    <xsl:variable name="outputs" select=".//port[@dir='out']"/>
    Be sure to skip over output ports of the current module.  We only want to
    propagate those ports which are outputs of something within this module.
    If the module already has an output port (eg output of a loop), it does not
    need to be propagated out.
  -->
  <xsl:variable name="outputs" select="./*/descendant::port[@dir='out']"/>

  <!-- Across all the descendant input ports, find the set of unique source
       ports. In order to do this we need a copy of each input node so that
       the preceding-sibling axis has an appropriate context on which to work.
  -->
  <xsl:variable name="inputs">
    <xsl:for-each select=".//port[@dir='in']">
      <xsl:copy-of select="."/>
    </xsl:for-each>
  </xsl:variable>

  <xsl:variable name="uniqueSources">
    <xsl:for-each select="$inputs/*">
    <xsl:variable name="myName" select="@source"/>
    <xsl:if test="not(preceding-sibling::*[@source=$myName])">
      <xsl:copy-of select="."/>
    </xsl:if>
    </xsl:for-each>
  </xsl:variable>
  
  <!-- For each unique source port (data producer) see if we need to go up
     one more hierarchical level to find the source.  If so, create the
     input port for that signal. -->
  <xsl:for-each select="$uniqueSources/*">
    <xsl:variable name="sourceName" select="@source"/>
    <!-- Check to be sure that the output port exists somewhere within the
       current action(top level module).  If not it is a state var
       reference and will be handled seperately. -->
    <xsl:if test="$scope/ancestor::module[@kind='action' or @kind='action-scheduler']//port[@dir='out'][@name=$sourceName]">
    <xsl:if test="not($scope//port[@dir='out'][@name=$sourceName])">
      <xsl:element name="port">
      <xsl:attribute name="name">FIXME</xsl:attribute>
      <xsl:attribute name="dir">in</xsl:attribute>
      <xsl:attribute name="source"><xsl:value-of select="@source"/></xsl:attribute>
      <xsl:attribute name="typeName"><xsl:value-of select="@typeName"/></xsl:attribute>
      <xsl:attribute name="size"><xsl:value-of select="@size"/></xsl:attribute>
      </xsl:element>
    </xsl:if>
    </xsl:if>
  </xsl:for-each>

  <!-- recurse -->
    <xsl:apply-templates/>

  
  <!-- Looking at the set of all input ports, find the set that consumes the
     current output port.  If any input port in that set does NOT contain the
     current node as an ancestor, then we need to push the given output to
     the next hierarchical level. -->

  <xsl:for-each select="$outputs">
    <!-- Find all the consumer inputs -->
    <xsl:variable name="currentOutput" select="."/>
    
    <xsl:variable name="modules" select="$currentOutput/ancestor::module"/>
    <!-- The list of consuming inputs is found by starting at the top most 
         module in the ancestor list and finding all input ports whose source
         is the current output. -->
    <!-- Filter out those inputs which share the current module in common with the current output -->
    <!-- <xsl:variable name="consumingInputs" select="$modules[1]//port[@dir='in'][@source=$currentOutput/@name]"/>
    <xsl:for-each select="$consumingInputs">
      <xsl:variable name="currentInput" select="."/>
      <xsl:if test="not($currentInput/ancestor::*[@tag=$scopeTag])">
        <xsl:element name="port">
          <xsl:attribute name="name">FIXME</xsl:attribute>
          <xsl:attribute name="dir">out</xsl:attribute>
          <xsl:attribute name="source"><xsl:value-of select="$currentOutput/@name"/></xsl:attribute>
          <xsl:attribute name="size"><xsl:value-of select="$currentOutput/@size"/></xsl:attribute>
          <xsl:attribute name="typeName"><xsl:value-of select="$currentOutput/@typeName"/></xsl:attribute>
          <xsl:attribute name="locate">7</xsl:attribute>
        </xsl:element>
      </xsl:if>
    </xsl:for-each>
    -->
    <xsl:if test="some $port in $modules[1]//port[@dir='in'][@source=$currentOutput/@name] satisfies not($port/ancestor::*[@tag=$scopeTag])">
      <port name="FIXME" dir="out" source="{$currentOutput/@name}" size="{$currentOutput/@size}" typeName="{$currentOutput/@typeName}"/>
    </xsl:if>
    
  </xsl:for-each>
  
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
