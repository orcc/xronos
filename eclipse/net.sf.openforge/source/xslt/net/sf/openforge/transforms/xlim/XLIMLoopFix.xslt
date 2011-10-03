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
  
  <xd:doc>
    This transformation annotates loop elements with the necessary additional
    information to make the process of conversion to SLIM more
    regular.  To accomplish this, an output port is added to loops for each
    value calculated in the loop, but consumed outside the loop.  The output
    port takes care of translating the internal name (which is used in the
    feedback path) to an external name (which is used in the loop 'done' path).
    Once this port is created, we can take the extra step of wrapping the loop
    body block and decision into a loopbody module.  Also, we add a no-op in the
    loopbody module that replaces the created output port.  This no-op now
    translates the internal (feedback) name into an external (done) name.
    We also create a no-op as a peer to the loop for each PHI port that consumes
    a state variable.  This allows the conversion of state var references to var
    reads to only consider operations.
  </xd:doc>

<!-- Tells this transformation to ignore white space in all element of the source xml -->
<xsl:strip-space elements="*"/>

<xsl:template match="/">
  
  <!-- Adds tags to each element, used in uniquely identifying things. -->
  <xsl:variable name="tagified">
    <xsl:apply-templates mode="tagify"/>
  </xsl:variable>

  <xsl:variable name="allPHIHaveSinks">
    <xsl:apply-templates select="$tagified" mode="addPHISinks"/>
  </xsl:variable>
  
  <!-- Builds up a data structure idenitfying loop outputs, their salient
       characteristics, and consuming ports.
       It consists of n many 'nameToConsumers' elements having:
       @source
       @typeName
       @size
       @loopTag
       <consumer @value=consuming port tag>
       ...
  -->
  <xsl:variable name="loopOuts">
    <xsl:apply-templates select="$allPHIHaveSinks" mode="findLoopOutputs"/>
  </xsl:variable>
  
  <!--
      Adds output ports to loops for each value generated in the loop and consumed
      outside the loop.  Additionally, any input port (outside the loop) which consumes
      the loop generated value has its source attribute updated to point to the new
      loop output port.
  -->
  <xsl:variable name="withPorts">
    <xsl:apply-templates select="$allPHIHaveSinks" mode="addLoopPorts">
      <xsl:with-param name="loopOutputs" select="$loopOuts/*"/>
    </xsl:apply-templates>
  </xsl:variable>
  
  <!--
      Update the 'source' attribute of each generated port such that there is a
      path from actual generated value in the loop through the intermediate loop
      output ports to the consuming port (following name-> source relationships)
  -->
  <xsl:variable name="nameRouted">
    <xsl:apply-templates select="$withPorts" mode="routeNames"/>
  </xsl:variable>
  
  <!-- Fix the structure of loops to be as expected structurally. -->
  <xsl:variable name="loopsFixed">
    <xsl:apply-templates select="$nameRouted" mode="fixLoops"/>
  </xsl:variable>

  <xsl:apply-templates select="$loopsFixed" mode="fixLoopPHI"/>
  
</xsl:template>


<!--
    *********************************************************************
    *********************************************************************
-->

<xsl:template match="module[@kind='loop']" mode="findLoopOutputs">
  
  <xsl:variable name="loopTag" select="@tag"/>
  <xsl:variable name="consumedNames" select="ancestor::module[last()]//port[@dir='in'] except .//port[@dir='in']"/>
  <xsl:for-each select=".//port[@dir='out']">
    <xsl:variable name="outputName" select="@source"/>
    <xsl:if test="count($consumedNames[@source=$outputName]) > 0">

      <nameToConsumers source="{@source}" typeName="{@typeName}" size="{@size}" loopTag="{$loopTag}">
        <xsl:for-each select="$consumedNames">
          <consumer value="{@tag}"/>
        </xsl:for-each>
      </nameToConsumers>
      
    </xsl:if>
  </xsl:for-each>
  <xsl:apply-templates mode="findLoopOutputs"/>
</xsl:template>

<xsl:template match="*" mode="findLoopOutputs">
  <xsl:apply-templates mode="findLoopOutputs"/>
</xsl:template>



<!--
    *********************************************************************
    *********************************************************************
-->

<xsl:template match="module[@kind='loop']" mode="addLoopPorts">
  <xsl:param name="loopOutputs" select="_empty_list_"/>
  
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
    
    <xsl:apply-templates mode="addLoopPorts">
      <xsl:with-param name="loopOutputs" select="$loopOutputs"/>
    </xsl:apply-templates>			
    
    <xsl:variable name="tag" select="@tag"/>
    <xsl:for-each select="$loopOutputs">
      <xsl:if test="$tag=@loopTag">
        <port name="{concat(generate-id(),'_tp')}" dir="out" typeName="{@typeName}" size="{@size}" tsource="{@source}"/>
      </xsl:if>
    </xsl:for-each>
    
  </xsl:element> 
  
</xsl:template>

<!--
    For any input port which consumes a loop generated value, give it
    a tsource attribute.  However, only do this for things that are
    OUTSIDE the loop.  The mapping in the loopOutputs map will give
    us the data we need.
-->
<xsl:template match="port[@dir='in']" mode="addLoopPorts">
  <xsl:param name="loopOutputs" select="_empty_list_"/>
  
  <xsl:variable name="localSource" select="@source"/>
  <xsl:variable name="localTag" select="@tag"/>
  
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
    
    <xsl:for-each select="$loopOutputs">
      <xsl:if test="@source=$localSource and count(./consumer[@value=$localTag]) > 0">
        <xsl:attribute name="tsource"><xsl:value-of select="@source"/></xsl:attribute>
      </xsl:if>
    </xsl:for-each>
    <xsl:apply-templates mode="addLoopPorts">
      <xsl:with-param name="loopOutputs" select="$loopOutputs"/>
    </xsl:apply-templates>			
    
  </xsl:element> 
</xsl:template>


<xsl:template match="*" mode="addLoopPorts">
  <xsl:param name="loopOutputs" select="_empty_list_"/>
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
    <xsl:apply-templates mode="addLoopPorts">
      <xsl:with-param name="loopOutputs" select="$loopOutputs"/>
    </xsl:apply-templates>			
  </xsl:element> 
</xsl:template>

<!--
    *********************************************************************
    *********************************************************************
-->

<xsl:template match="*" mode="routeNames">
  <!-- Preserve the existing element information -->
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <!-- <xsl:if test="name() != 'tsource' and name() != 'tag'"> -->
      <xsl:if test="name() != 'tsource'">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:if>
    </xsl:for-each>
    
    <!-- If there is a tsource attribute then overide the 'source' attribute
         to achieve routing of the loop output. -->
    <xsl:if test="@tsource">
      <xsl:variable name="trueSource" select="@tsource"/>
      
      <xsl:choose>
        <!-- if parent is a loop module, find the source as a previous sibling's child -->
        <xsl:when test="parent::module[@kind='loop']">
          <xsl:variable name="both" select="preceding-sibling::*//port[@dir='out'][@source=$trueSource or @tsource=$trueSource]"/>
          <xsl:attribute name="source">
            <xsl:choose>
              <xsl:when test="$both[last()]/@name"><xsl:value-of select="$both[last()]/@name"/></xsl:when>
              <xsl:otherwise><xsl:value-of select="$both[last()]/@source"/></xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
        </xsl:when>
        
        <!-- If parent is a PHI, then the reference is to something within the
             scope of the loop body. -->
        <xsl:when test="parent::PHI">
          <xsl:variable name="both" select="../../*//port[@dir='out'][@source=$trueSource or @tsource=$trueSource]"/>
          <xsl:attribute name="source">
            <xsl:choose>
              <xsl:when test="$both[last()]/@name"><xsl:value-of select="$both[last()]/@name"/></xsl:when>
              <xsl:otherwise><xsl:value-of select="$both[last()]/@source"/></xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
        </xsl:when>
        
        <!-- if parent is an operation, the source is the highest tsource in the tree from root module -->
        <xsl:otherwise>
          <xsl:variable name="sources" select="ancestor::module[last()]//port[@dir='out'][@tsource=$trueSource]"/>
          <xsl:for-each select="$sources">
          </xsl:for-each>
          <xsl:attribute name="source">
            <xsl:choose>
              <xsl:when test="$sources[last()]/@name"><xsl:value-of select="$sources[last()]/@name"/></xsl:when>
              <xsl:otherwise><xsl:value-of select="$sources[last()]/@source"/></xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
        </xsl:otherwise>
      </xsl:choose>
      
    </xsl:if>
    
    <xsl:apply-templates mode="routeNames"/>
  </xsl:element>
  
</xsl:template>


<!--
    *********************************************************************
    *********************************************************************
-->

<xsl:template match="module[@kind='loop']" mode="fixLoops">
  
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
    
    <!-- Wrap any modules (eg the test and body) in a loopBody module -->
    <!-- Also, remove any loop (module) output ports.  Replacing them with
         a noop with an input port and output port.  The input port source is
         the source of the module output port.  The output port name and
         source is the name of the output port. -->
    <xsl:variable name="outports" select="./port[@dir='out']"/>
    
    <xsl:apply-templates select="./*[not(self::module)][not(self::port[@dir='out'])]" mode="fixLoops"/>
    
    <xsl:element name="module">
      <xsl:attribute name="kind">loopBody</xsl:attribute>
      <xsl:attribute name="tag"><xsl:value-of select="concat(generate-id(),'_lb')"/></xsl:attribute>
      
      <!-- Put the noops, one per output port, into the loop body. -->
      <xsl:apply-templates select="./module" mode="fixLoops"/>
      <xsl:for-each select="$outports">
        <operation kind="noop">
          <port dir="in" source="{@source}"/>
          <port dir="out" source="{@name}" typeName="{@typeName}" size="{@size}"/>
        </operation>
      </xsl:for-each>
    </xsl:element>
    
  </xsl:element> 
</xsl:template>

<xsl:template match="*" mode="fixLoops">
  <xsl:param name="convertToNoOps" select="_empty_list"/>
  
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
    
    <xsl:apply-templates mode="fixLoops"/>
    
    <!-- Insert the noops after everything else -->
    <xsl:for-each select="$convertToNoOps">
      <operation kind="noop">
        <port dir="in" source="{@source}"/>
        <port dir="out" source="{@name}" typeName="{@typeName}" size="{@size}"/>
      </operation>
    </xsl:for-each>
    
  </xsl:element> 
</xsl:template>


<!-- Find all PHI elements which have NO targets and create a no-op within the
     loop body as a sink for that PHI.  This keeps us from having to special case
     later on in the transformation chain because we can be sure that all PHI
     operators have a sink. -->
<xsl:template match="module[@kind='body']" mode="addPHISinks">
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>

    <xsl:variable name="scope" select="."/>
    <xsl:for-each select="../PHI">
      <xsl:variable name="thePHI" select="."/>
      <xsl:variable name="thePHIName" select="./port[@dir='out']/@source"/>
      <xsl:variable name="targets" select="ancestor::module[last()]//port[@dir='in'][@source=$thePHIName]"/>
      <xsl:if test="count($targets)=0">
        <operation kind="noop" boo="far">
          <port dir="in" source="{$thePHIName}"/>
          <port dir="out" source="{concat($thePHIName,'_sink')}" size="{$thePHI/port[@dir='out']/@size}" typeName="{$thePHI/port[@dir='out']/@typeName}"/>
        </operation>
      </xsl:if>
    </xsl:for-each>
    
    <xsl:apply-templates mode="addPHISinks"/>

  </xsl:element>

  
</xsl:template>

<xsl:template match="*" mode="addPHISinks">
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
    <xsl:apply-templates mode="addPHISinks"/>
  </xsl:element>
</xsl:template>

<!--
    *********************************************************************
    *********************************************************************
-->


<xsl:template match="*" mode="tagify">
  
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
    <!-- Create (overwrite) unique tags for all elements. -->
    <xsl:attribute name="tag"><xsl:value-of select="generate-id()"/></xsl:attribute>
    
    <xsl:apply-templates mode="tagify"/>
  </xsl:element>
</xsl:template>

<!--
    *********************************************************************
    *********************************************************************
-->

<xsl:template match="module[@kind='loop']" mode="fixLoopPHI">

  <xsl:for-each select="./PHI/port[@dir='in']">
    <xsl:variable name="sourceName" select="./@source"/>
    <xsl:if test="count(ancestor::design/stateVar[@name=$sourceName]) > 0">
      <xsl:variable name="newName" select="concat($sourceName, '_to_', ../@tag)"/>
      <operation kind="noop">
        <port dir="in" source="{$sourceName}"/>
        <port dir="out" name="{$newName}" source="{$newName}" size="{../port[@dir='out']/@size}" typeName="{../port[@dir='out']/@typeName}"/>
      </operation>
    </xsl:if>
  </xsl:for-each>
  
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
    <xsl:apply-templates mode="fixLoopPHI"/>
  </xsl:element>
  
</xsl:template>

<xsl:template match="PHI/port[@dir='in']" mode="fixLoopPHI">
  <!-- Preserve the existing element information -->
  <xsl:variable name="sourceName" select="@source"/>
  <xsl:variable name="PHItag" select="../@tag"/>
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:choose>
        <xsl:when test="(name() = 'source') and count(ancestor::design/stateVar[@name=$sourceName]) > 0">
          <xsl:attribute name="{name()}"><xsl:value-of select="concat($sourceName, '_to_', $PHItag)"/></xsl:attribute>
        </xsl:when>
        <xsl:otherwise>
          <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:for-each>
    <xsl:apply-templates mode="fixLoopPHI"/>
  </xsl:element>
</xsl:template>

<xsl:template match="*" mode="fixLoopPHI">
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
    <xsl:for-each select="@*">
      <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
    <xsl:apply-templates mode="fixLoopPHI"/>
  </xsl:element>
</xsl:template>


</xsl:stylesheet>
