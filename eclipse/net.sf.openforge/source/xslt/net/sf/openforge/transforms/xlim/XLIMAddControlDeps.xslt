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

<xsl:stylesheet
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xd="http://www.pnp-software.com/XSLTdoc"
	version="1.1">
<xsl:output method="xml"/>

<xd:doc type="stylesheet">
  Modifies the SLIM to include Forge specific scheduling dependencies
  by annotating each module with dependencies from each operation to the 'done'
  of the exit for that module.  This indicates that in order for the module to be
  complete that every operation in the module must complete.
  <xd:author>IDM</xd:author>
  <xd:copyright>Xilinx, 2005</xd:copyright>
</xd:doc>

<!-- Tells this transformation to ignore white space in all element of the source xml -->
<xsl:strip-space elements="*"/>

<xd:doc>
  Top level to sequence the sub-transformations.  First a control bus is
  added to each exit.  Then a dependency is added to the exit of the module
  for each element within that module.
</xd:doc>
<xsl:template match="/">
  <xsl:variable name="withDonePorts">
	<xsl:apply-templates mode="addDone"/>
  </xsl:variable>

  <xsl:variable name="renamed">
	<xsl:apply-templates select="$withDonePorts" mode="rename"/>
  </xsl:variable>

  <xsl:variable name="depsAdded">
	<xsl:apply-templates select="$renamed" mode="addDeps"/>
  </xsl:variable>

  <xsl:variable name="renamedDeps">
	<xsl:apply-templates select="$depsAdded" mode="rename"/>
  </xsl:variable>

  <xsl:variable name="cleaned">
	<xsl:apply-templates select="$renamedDeps" mode="clean"/>
  </xsl:variable>
  
  <xsl:copy-of select="$cleaned"/>
  
</xsl:template>

<xd:doc>Adds control dependencies between the children and the module exit.</xd:doc>
<xsl:template match="module[@kind != 'if'][@kind != 'loopBody']" mode="addDeps">
  <!-- Preserve the existing element information -->
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
    <xsl:apply-templates mode="addDeps"/>

	<!-- Now add a dependency for each done bus to the module exit done. -->
	<xsl:variable name="moduleDone">
	  <xsl:value-of select="./exit[@kind='done']/port[@kind='control']/@name"/>
	</xsl:variable>

	<xsl:for-each select="child::*">
	  <xsl:if test="./exit[@kind='done']">
		<xsl:element name="dependency">
		  <!-- <xsl:attribute name="name">FIXME</xsl:attribute>-->
		  <xsl:attribute name="source"><xsl:value-of select="./exit[@kind='done']/port[@kind='control']/@name"/></xsl:attribute>
		  <xsl:attribute name="dest"><xsl:value-of select="$moduleDone"/></xsl:attribute>
		  <xsl:attribute name="group"><xsl:value-of select="number(0)"/></xsl:attribute>
		</xsl:element>
	  </xsl:if>
	</xsl:for-each>
  </xsl:element>
</xsl:template>

<xsl:template match="dependency[@qualifier]" mode="addDeps">
  <!-- Copy the dependency 'as-is' and then ceate another dependency from the
	   qualifier control bus output to the destination target control port -->
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
	  <xsl:if test="name() != 'qualifier'">
		<xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
	  </xsl:if>
    </xsl:for-each>
    <xsl:apply-templates mode="addDeps"/>
  </xsl:element> 

  <xsl:variable name="qualName"><xsl:value-of select="@qualifier"/></xsl:variable>
  <xsl:variable name="destination"><xsl:value-of select="@dest"/></xsl:variable>
  <xsl:element name="dependency">
	<xsl:attribute name="source"><xsl:value-of select="../module[@kind=$qualName]/exit[@kind='done']/port[@kind='control']/@name"/></xsl:attribute>
	<xsl:attribute name="dest"><xsl:value-of select="../exit[@kind='done']/port[@name=$destination]/../port[@kind='control']/@name"/></xsl:attribute>
	<xsl:attribute name="group"><xsl:value-of select="@group"/></xsl:attribute>
  </xsl:element>
</xsl:template>

<xd:doc>Add a control bus to every exit.</xd:doc>
<xsl:template match="exit" mode="addDone">
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
    <xsl:apply-templates mode="addDone"/>

	<!-- Only create done ports for exits which do not already have a done port. -->
	<xsl:variable name="donePorts" select="./port[@kind='control']"/>
	<xsl:if test="count($donePorts) = 0">
	  <xsl:element name="port">
		<xsl:attribute name="name">FIXME</xsl:attribute>
		<xsl:attribute name="dir">out</xsl:attribute>
		<xsl:attribute name="size">1</xsl:attribute>
		<xsl:attribute name="source">FIXME</xsl:attribute>
		<xsl:attribute name="typeName">bool</xsl:attribute>
		<xsl:attribute name="tag">FIXME</xsl:attribute>
		<xsl:attribute name="kind">control</xsl:attribute>
	  </xsl:element>
	</xsl:if>
  </xsl:element> 
</xsl:template>

<xsl:template match="*" mode="rename">
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
	  <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>

	<xsl:if test="@name='FIXME' and name()='port'">
	  <xsl:attribute name="name"><xsl:value-of select="concat(generate-id(), '_done')"/></xsl:attribute>
	</xsl:if>
	<xsl:if test="@name='FIXME' and name()='dependency'">
	  <xsl:attribute name="name"><xsl:value-of select="concat('dd', generate-id())"/></xsl:attribute>
	</xsl:if>
	<xsl:if test="@source='FIXME'">
	  <xsl:attribute name="source"><xsl:value-of select="concat(generate-id(), '_done')"/></xsl:attribute>
	</xsl:if>
	<xsl:if test="@tag='FIXME'">
	  <xsl:attribute name="tag"><xsl:value-of select="generate-id()"/></xsl:attribute>
	</xsl:if>
	
    <xsl:apply-templates mode="rename"/>
  </xsl:element> 
</xsl:template>


<xsl:template match="note" mode="clean">
  <xsl:apply-templates mode="clean"/>
</xsl:template>

<xsl:template match="*" mode="clean">
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:choose>
<!--        <xsl:when test="name() = 'tag'"></xsl:when> -->
        <xsl:when test="name() = 'source' and not(parent::dependency or parent::connection)"></xsl:when>
        <xsl:otherwise>
          <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:for-each>
    <xsl:apply-templates mode="clean"/>
  </xsl:element>
</xsl:template>


<xd:doc>
  Default Copy.
</xd:doc>
<xsl:template match="*">
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
    <xsl:apply-templates/>
  </xsl:element> 
</xsl:template>

<xsl:template match="*" mode="addDone">
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
    <xsl:apply-templates mode="addDone"/>
  </xsl:element> 
</xsl:template>

<xsl:template match="*" mode="addDeps">
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
    <xsl:apply-templates mode="addDeps"/>
  </xsl:element> 
</xsl:template>

</xsl:stylesheet>