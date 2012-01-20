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
  XLIMMInsertCasts.xslt
  Copyright (c) 2005 Xilinx Inc.
  One step in the transformation from the XLIM format into SLIM format.
  Create dependencies from output ports to input ports at each hierarchical
  level.
  Insert cast operations for any size mismatched flows.
  
  Casting works by casting each operations input ports to match the
  maximum size of any input port (so that they balance) and to ensure
  that size mismatches from the source port are handled.  To preserve
  correct structure, any cast operations in the if or loop constructs
  are moved to be peers with the source of the cast.
  
  Author: IDM
  2005-12-15 Creation
-->
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xd="http://www.pnp-software.com/XSLTdoc"
  xmlns:math="http://exslt.org/math"
  extension-element-prefixes="math"
  version="1.1">
<xsl:output method="xml"/>

<!-- Tells this transformation to ignore white space in all element of the source xml -->
<xsl:strip-space elements="*"/>

<xsl:template match="/">

  <xsl:variable name="sizeMarked">
    <xsl:apply-templates mode="markOpSizes"/>
  </xsl:variable>

  <xsl:variable name="casted">
    <xsl:apply-templates select="$sizeMarked" mode="balancePorts"/>
  </xsl:variable>
  
  <xsl:apply-templates select="$casted" mode="moveOpsOutOfLoopsAndIfs"/>
  
</xsl:template>

<!--
    Templates to derive the typeName and size of each port based on rules to 'balance'
    the sizes and types by type of operation.
-->
<xsl:template match="operation[@kind='pinWrite']" mode="markOpSizes">
  <xsl:variable name="pinName" select="@portName"/>
  <xsl:variable name="actorPin" select="/design/actor-port[@name=$pinName]"/>
  <xsl:variable name="internalPin" select="/design/internal-port[@name=$pinName]"/>
  <!-- Preserve the existing element information -->
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
    <xsl:apply-templates mode="markOpSizes"/>

    <xsl:choose>
      <xsl:when test="count($actorPin) > 0">
        <note kind="opSize" maxSize="{$actorPin/@size}" maxType="{$actorPin/@typeName}"/>
      </xsl:when>
      <xsl:otherwise>
        <note kind="opSize" maxSize="{$internalPin/@size}" maxType="{$internalPin/@typeName}"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:element>
</xsl:template>

<xsl:template match="operation|PHI" mode="markOpSizes">
  <!-- Calculate the max port size -->
  <xsl:variable name="maxPortSizes">
    <xsl:for-each select="./port">
      <xsl:variable name="mySize" select="@size"/>
      <xsl:if test="not(preceding-sibling::*[number(@size) > number($mySize)])">
        <xsl:element name="psize"><xsl:value-of select="$mySize"/></xsl:element>
      </xsl:if>
    </xsl:for-each>
  </xsl:variable>

  <xsl:variable name="opType">
    <xsl:apply-templates select="." mode="deriveTypeName"/>
  </xsl:variable>
  
  <!-- Preserve the existing element information -->
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
    <xsl:apply-templates mode="markOpSizes"/>

    <note kind="opSize" maxSize="{$maxPortSizes/*[last()]}" maxType="{$opType}"/>
  </xsl:element>

</xsl:template>

<xsl:template match="*" mode="markOpSizes">
  <!-- <xsl:if test="self::design"><xsl:message>FIXME.  Cast insertion needs to account for differing typeName attributes.  Need to derive rules for the dominant typeName</xsl:message></xsl:if>-->
  <!-- Preserve the existing element information -->
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
    <xsl:apply-templates mode="markOpSizes"/>
  </xsl:element>
</xsl:template>

<!--
    Templates to derive the typename based on the type of the operation
-->
<xsl:template match="operation[@kind=('$eq','$ge','$gte','$gt','$le','$lt')]" mode="deriveTypeName">
  <!-- the assumption is that the two ports will have matching types -->
  <xsl:if test="not(every $port in ./port[@dir='in'] satisfies $port/@typeName = ./port[@dir='in'][1]/@typeName)">
    <xsl:message>WARNING: Unbalanced port types on <xsl:value-of select="@tag"/> defaulting to type of first port</xsl:message>
  </xsl:if>
  <xsl:value-of select="./port[@dir='in'][1]/@typeName"/>
</xsl:template>
<xsl:template match="operation[@kind='assign']" mode="deriveTypeName">
  <xsl:choose>
    <xsl:when test="count(./port[@dir='in']) = 1"><xsl:value-of select="./port[@dir='in']/@typeName"/></xsl:when>
    <xsl:when test="count(./port[@dir='in']) = 2"><xsl:value-of select="./port[@dir='in'][2]/@typeName"/></xsl:when>
  </xsl:choose>
</xsl:template>

<xsl:template match="operation[@kind=('pinStall','taskCall')]" mode="deriveTypeName"><xsl:value-of select="'int'"/></xsl:template>

<xsl:template match="operation|PHI" mode="deriveTypeName">
  <xsl:choose>
    <xsl:when test="count(./port[@dir='out']) = 1"><xsl:value-of select="./port[@dir='out']/@typeName"/></xsl:when>
    <xsl:otherwise><xsl:message>Unknown derivation of typename for <xsl:value-of select="@tag"/></xsl:message></xsl:otherwise>
  </xsl:choose>
</xsl:template>
<xsl:template match="*" mode="deriveTypeName"></xsl:template>


<xsl:template match="operation[@kind='assign']" mode="balancePorts">
  <!--
      Ignore the maxSize and maxType calculated from the
      markOpSizes templates. Instead, simply cast the input
      ports as necessary.
  -->
  <!-- Insert casts if the input port(s) does not match its source -->
  <xsl:variable name="nameMap">
    <xsl:for-each select="./port[@dir='in']">
      <xsl:variable name="theInPort" select="."/>
      <xsl:variable name="theOutPort" select="$theInPort/ancestor::module[last()]//port[@dir='out'][@source=$theInPort/@source]"/>

      <xsl:call-template name="checkCastPort">
        <xsl:with-param name="theInPort" select="$theInPort"/>
        <xsl:with-param name="theSize" select="$theInPort/@size"/>
        <xsl:with-param name="theType" select="$theInPort/@typeName"/>
      </xsl:call-template>
    </xsl:for-each>
  </xsl:variable>

  <!-- write out the input casts -->
  <xsl:copy-of select="$nameMap/operation"/>
  
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
    <xsl:apply-templates mode="balancePorts">
      <xsl:with-param name="nameMap" select="$nameMap"/>
    </xsl:apply-templates>
  </xsl:element>

</xsl:template>

<!--
    create a cast anytime we have ports whose size != max size or the max size is not
    equal to the driving size.  For certain operations, size the output to max size
    also.
-->
<xsl:template match="operation[@kind='lshift' or @kind='rshift']" mode="balancePorts">
  <!--
      Ignore the maxSize and maxType calculated from the
      markOpSizes templates. Instead, simply copy the data 
      port size to the result and then cast accordingly.
  -->
  <xsl:variable name="dataPort" select="./port[@dir='in'][1]"/>
  <xsl:variable name="magPort" select="./port[@dir='in'][2]"/>
  <xsl:variable name="result" select="./port[@dir='out']"/>

  <xsl:variable name="dataPortSizes" select="$dataPort/@size | $result/@size"/>
  <!-- The data port of the rshift must be in a power of 2 size. -->
  <xsl:variable name="maxDataSize" select="math:max($dataPortSizes)"/>
  <xsl:variable name="dataSize">
    <xsl:choose>
      <xsl:when test="@kind='rshift'"><xsl:value-of select="math:power(2, ceiling(math:log($maxDataSize) div math:log(2)))"/></xsl:when>
      <xsl:otherwise><xsl:value-of select="$maxDataSize"/></xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <!-- The magnitude port size should be set to the -->
  <xsl:variable name="magSize">
    <xsl:choose>
      <xsl:when test="@kind='rshift'"><xsl:value-of select="ceiling(math:log($dataSize) div math:log(2))"/></xsl:when>
      <xsl:otherwise><xsl:value-of select="$magPort/@size"/></xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  
  <xsl:variable name="magType" select="'bool'"/> <!-- bool is the ony unsigned type we have -->
  
  <!-- Ensure that the output port and the input port are equivalently sized -->
  <xsl:variable name="outNameMap">
    <xsl:variable name="theOutputPort" select="./port[@dir='out']"/>
    <xsl:if test="$theOutputPort/@size != $dataSize">
      <xsl:call-template name="checkCastOutput">
        <xsl:with-param name="theOutPort" select="$theOutputPort"/>
        <xsl:with-param name="theSize" select="$dataSize"/>
        <xsl:with-param name="theType" select="$dataPort/@typeName"/>
      </xsl:call-template>
    </xsl:if>
  </xsl:variable>
  
  <!-- Insert casts if either input port does not match its source -->
  <xsl:variable name="nameMap">
    <xsl:call-template name="checkCastPort">
      <xsl:with-param name="theInPort" select="$dataPort"/>
      <xsl:with-param name="theSize" select="$dataSize"/>
      <xsl:with-param name="theType" select="$dataPort/@typeName"/>
    </xsl:call-template>
    <xsl:call-template name="checkCastPort">
      <xsl:with-param name="theInPort" select="$magPort"/>
      <xsl:with-param name="theSize" select="$magSize"/>
      <xsl:with-param name="theType" select="$magType"/>
    </xsl:call-template>
    <xsl:copy-of select="$outNameMap/entry"/>
  </xsl:variable>

  <!-- write out the input casts -->
  <xsl:copy-of select="$nameMap/operation"/>
  
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
    <xsl:apply-templates mode="balancePorts">
      <xsl:with-param name="nameMap" select="$nameMap"/>
    </xsl:apply-templates>
  </xsl:element>

  <!-- copy the generated output casts (if any) -->
  <xsl:copy-of select="$outNameMap/operation"/>
  
</xsl:template>

<xsl:template match="*" mode="balancePorts">

  <!-- Calculate the max port size -->
  <xsl:variable name="maxSize" select="./note[@kind='opSize']/@maxSize"/>
  <xsl:variable name="opType" select="./note[@kind='opSize']/@maxType"/>

  <xsl:variable name="castableOutputs" select="./port[@dir='out'][@size != $maxSize]"/>

  <xsl:variable name="outNameMap">
    <xsl:if test="@kind=('$add','$and','bitnot','bitand','$div','$mul','$negate','$not','$or','bitor','$sub','$xor','bitxor','$noop','noop')">
      <xsl:for-each select="$castableOutputs">
        <xsl:variable name="theOutputPort" select="."/>
        <xsl:call-template name="checkCastOutput">
          <xsl:with-param name="theOutPort" select="$theOutputPort"/>
          <xsl:with-param name="theSize" select="$maxSize"/>
          <xsl:with-param name="theType" select="$opType"/>
        </xsl:call-template>
      </xsl:for-each>
    </xsl:if>
  </xsl:variable>
  
  <!-- For each input port, create a cast if needed and store it in a map along with the
       data about naming. -->
  <xsl:variable name="nameMap">
    <xsl:for-each select="./port[@dir='in']">
      <xsl:variable name="theInPort" select="."/>
      <xsl:variable name="theOutPort" select="$theInPort/ancestor::module[last()]//port[@dir='out'][@source=$theInPort/@source]"/>

      <xsl:call-template name="checkCastPort">
        <xsl:with-param name="theInPort" select="$theInPort"/>
        <xsl:with-param name="theSize" select="$maxSize"/>
        <xsl:with-param name="theType" select="$opType"/>
      </xsl:call-template>
    </xsl:for-each>

    <xsl:copy-of select="$outNameMap/entry"/>
  </xsl:variable>
  
  <!-- write out the input casts -->
  <xsl:copy-of select="$nameMap/operation"/>
  
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
    <xsl:apply-templates mode="balancePorts">
      <xsl:with-param name="nameMap" select="$nameMap"/>
    </xsl:apply-templates>
  </xsl:element>

  <!-- copy the generated output casts (if any) -->
  <xsl:copy-of select="$outNameMap/operation"/>
  
</xsl:template>

<xsl:template match="port" mode="balancePorts">
  <xsl:param name="nameMap" select="_empty_map"/>
  <xsl:variable name="currentPort" select="."/>
  <xsl:variable name="mapEntry" select="$nameMap/entry[@sourceName=$currentPort/@source]"/>
  
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
    <xsl:if test="$mapEntry">
      <xsl:attribute name="source"><xsl:value-of select="$mapEntry/@newName"/></xsl:attribute>
      <xsl:if test="$currentPort[@dir='out']">
        <xsl:attribute name="name"><xsl:value-of select="$mapEntry/@newName"/></xsl:attribute>
      </xsl:if>
      <xsl:attribute name="size"><xsl:value-of select="$mapEntry/@newSize"/></xsl:attribute>
      <xsl:attribute name="typeName"><xsl:value-of select="$mapEntry/@newType"/></xsl:attribute>
    </xsl:if>
    <xsl:apply-templates mode="balancePorts"/>
  </xsl:element>

</xsl:template>

<xsl:template name="checkCastPort">
  <xsl:param name="theInPort"/>
  <xsl:param name="theSize"/>
  <xsl:param name="theType"/>

  <xsl:variable name="theOutPort" select="$theInPort/ancestor::module[last()]//port[@dir='out'][@source=$theInPort/@source]"/>

  <xsl:if test="$theInPort/@size != $theSize or $theOutPort/@size != $theSize or $theInPort/@typeName != $theType or $theOutPort/@typeName != $theType">
    
    <xsl:variable name="portName" select="concat($theOutPort/@source,'_castedTo_',$theInPort/@name)"/>
    
    <xsl:call-template name="writeCast">
      <xsl:with-param name="fromName" select="$theOutPort/@source"/>
      <xsl:with-param name="fromSize" select="$theOutPort/@size"/>
      <xsl:with-param name="fromType" select="$theOutPort/@typeName"/>
      
      <xsl:with-param name="toName" select="$portName"/>
      <xsl:with-param name="toSize" select="$theSize"/>
      <xsl:with-param name="toType" select="$theType"/>
    </xsl:call-template>
    
    <entry sourceName="{$theOutPort/@source}" newName="{$portName}" newSize="{$theSize}" newType="{$theType}"/>
  </xsl:if>
  
</xsl:template>

<xsl:template name="checkCastOutput">
  <xsl:param name="theOutPort"/>
  <xsl:param name="theSize"/>
  <xsl:param name="theType"/>

  <xsl:if test="$theOutPort/@size != $theSize">
    
    <xsl:variable name="portName" select="concat($theOutPort/@source,'_toCast_')"/>
    
    <xsl:call-template name="writeCast">
      <xsl:with-param name="fromName" select="$portName"/>
      <xsl:with-param name="fromSize" select="$theSize"/>
      <xsl:with-param name="fromType" select="$theType"/>
      
      <xsl:with-param name="toName" select="$theOutPort/@source"/>
      <xsl:with-param name="toSize" select="$theOutPort/@size"/>
      <xsl:with-param name="toType" select="$theOutPort/@typeName"/>
    </xsl:call-template>
    
    <entry sourceName="{$theOutPort/@source}" newName="{$portName}" newSize="{$theSize}" newType="{$theType}"/>
  </xsl:if>
  
</xsl:template>


<xsl:template name="writeCast">
  <xsl:param name="fromName"/>
  <xsl:param name="fromSize"/>
  <xsl:param name="fromType"/>
  
  <xsl:param name="toName"/>
  <xsl:param name="toSize"/>
  <xsl:param name="toType"/>

  <operation kind="cast" tag="{concat(generate-id(),'_castId')}">
<!--    <port dir="in" source="{$fromName}" name="{concat(generate-id(),'_cast')}" typeName="{$fromType}" size="{$fromSize}" tag="{concat(generate-id(),'_1')}"/>-->
    <port dir="in" source="{$fromName}" name="{concat(generate-id(),'_cast_from_', $fromName)}" typeName="{$fromType}" size="{$fromSize}" tag="{concat(generate-id(),'_1')}"/>
    <port dir="out" source="{$toName}" name="{$toName}" typeName="{$toType}" size="{$toSize}" tag="{concat(generate-id(),'_2')}"/>
  </operation>
  
</xsl:template>

<!-- If and Loop modules expect to contain only very specific structures.  The insertion
     of casts and the like upset this expectation.  This template finds operations in the
     if and loop modules and moves the to be peers of the thing which generates them.  -->
<xsl:template match="*" mode="moveOpsOutOfLoopsAndIfs">
  <xsl:if test="not(self::operation and parent::module[@kind=('if','loop')])">
    <!-- Preserve the existing element information -->
    <xsl:element name="{name()}">
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      <xsl:apply-templates mode="moveOpsOutOfLoopsAndIfs"/>
    </xsl:element>
  </xsl:if>

  <xsl:variable name="movableOps">
    <xsl:for-each select="./port[@dir='out']">
      <xsl:variable name="outPort" select="."/>
      <xsl:for-each select="ancestor::module[last()]//port[@dir='in'][@source=$outPort/@source]">
        <xsl:variable name="inPort" select="."/>
        <xsl:if test="$inPort/ancestor::module[1]/@kind=('if','loop') and $inPort/parent::operation">
          <!-- The in port is a match to the cast operation that we want to move to this context -->
          <xsl:copy-of select="$inPort/.."/>
        </xsl:if>
      </xsl:for-each>
    </xsl:for-each>
  </xsl:variable>

  <xsl:variable name="uniqueMovableOps">
    <xsl:for-each select="$movableOps/*">
      <xsl:variable name="localTag" select="@tag"/>
      <xsl:if test="not(preceding-sibling::*[@tag=$localTag])">
        <xsl:copy-of select="."/>
      </xsl:if>
    </xsl:for-each>
  </xsl:variable>

  <xsl:for-each select="$uniqueMovableOps/*">
    <xsl:element name="{name()}">
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      <xsl:apply-templates mode="moveOpsOutOfLoopsAndIfs"/>
    </xsl:element>
  </xsl:for-each>
  
</xsl:template>

</xsl:stylesheet>