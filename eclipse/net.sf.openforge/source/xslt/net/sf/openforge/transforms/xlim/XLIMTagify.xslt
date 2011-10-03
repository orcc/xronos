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
  XLIMTagify.xslt
  Copyright (c) 2005 Xilinx Inc.
  One step in the transformation from the XLIM format into SLIM format.
  Gives every element in the document a unique tag attribute, overwriting any
  existing tag.
  
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

<xsl:template match="*">
  
  <!-- Preserve the existing element information -->  
  <xsl:element name="{name()}">
  <xsl:for-each select="@*">
    <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
    </xsl:for-each>
  <!-- Create (overwrite) unique tags for all elements. -->
  <xsl:attribute name="tag"><xsl:value-of select="generate-id()"/></xsl:attribute>
  
    <xsl:apply-templates/>
  </xsl:element>
</xsl:template>

</xsl:stylesheet>