<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="xml" indent="no" omit-xml-declaration="yes"/>
<xsl:template name="string-replace-all">
    <xsl:param name="text" />
    <xsl:param name="replace" />
    <xsl:param name="by" />
    <xsl:choose>
        <xsl:when test="contains($text, $replace)">
            <xsl:value-of select="substring-before($text,$replace)" />
            <xsl:value-of select="$by" />
            <xsl:call-template name="string-replace-all">
                <xsl:with-param name="text"
                    select="substring-after($text,$replace)" />
                <xsl:with-param name="replace" select="$replace" />
                <xsl:with-param name="by" select="$by" />
            </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
            <xsl:value-of select="$text" />
        </xsl:otherwise>
    </xsl:choose>
</xsl:template>

<xsl:template name="string-replace-all-BR">
    <xsl:param name="text" />
    <xsl:param name="by" />
    <!-- Replace <BR> -->
    <xsl:variable name="myVar">
        <xsl:call-template name="string-replace-all">
            <xsl:with-param name="text" select="$text" />
            <xsl:with-param name="replace" select="'&lt;BR&gt;'" />
            <xsl:with-param name="by" select="$by" />
        </xsl:call-template>
    </xsl:variable>

    <!-- Replace <Br> -->
    <xsl:variable name="myVar">
        <xsl:call-template name="string-replace-all">
            <xsl:with-param name="text" select="$myVar" />
            <xsl:with-param name="replace" select="'&lt;Br&gt;'" />
            <xsl:with-param name="by" select="$by" />
        </xsl:call-template>
    </xsl:variable>

    <!-- Replace <bR> -->
    <xsl:variable name="myVar">
        <xsl:call-template name="string-replace-all">
            <xsl:with-param name="text" select="$myVar" />
            <xsl:with-param name="replace" select="'&lt;bR&gt;'" />
            <xsl:with-param name="by" select="$by" />
        </xsl:call-template>
    </xsl:variable>

    <!-- Replace <br> -->
    <xsl:variable name="myVar">
        <xsl:call-template name="string-replace-all">
            <xsl:with-param name="text" select="$myVar" />
            <xsl:with-param name="replace" select="'&lt;br&gt;'" />
            <xsl:with-param name="by" select="$by" />
        </xsl:call-template>
    </xsl:variable>

    <xsl:value-of select="$myVar" />
</xsl:template>

<xsl:template name="string-replace-plot-BR">
    <xsl:param name="text" />
    <xsl:variable name="myVar">
        <xsl:call-template name="string-replace-all-BR">
            <xsl:with-param name="text" select="$text" />
            <xsl:with-param name="by" select="$skin-PlotLineBreak" />
        </xsl:call-template>
    </xsl:variable>

    <xsl:value-of select="$myVar" />
</xsl:template>
</xsl:stylesheet>