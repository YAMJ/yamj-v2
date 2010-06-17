<xsl:stylesheet version="1.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:media="http://search.yahoo.com/mrss/" 
    xmlns:dcterms="http://purl.org/dc/terms/">

<xsl:output method="xml" omit-xml-declaration="yes"/>

<xsl:template match="/">
<rss version="2.0">
    <xsl:for-each select="jukebox">

    <channel>
    <title><xsl:value-of select="@name"/></title>
    <link>http://www.foo.com</link>
    <description><xsl:value-of select="@name"/></description>
    
        <xsl:for-each select="movies">
            <xsl:call-template name="movie"/>
        </xsl:for-each>
    </channel>

    </xsl:for-each>
</rss>
</xsl:template>

<xsl:template name="movie">
    <item>
        <title><xsl:value-of select="title"/></title>
        <link>http://www.imdb.com/title/<xsl:value-of select="id[@movieDatabase = 'imdb']"/>/</link>
        <media:content>
            <xsl:attribute name="url"><xsl:value-of select="files/file/fileURL"/></xsl:attribute>
            <xsl:attribute name="fileSize"><xsl:value-of select="files/file/@size"/></xsl:attribute> 
            <xsl:attribute name="type"><xsl:value-of select="container"/></xsl:attribute> 

            <media:credit role="director"><xsl:value-of select="director"/></media:credit>
            <media:credit role="artist">artist's name</media:credit>
            <media:category scheme="http://blah.com/scheme">music/artist name/album/song</media:category>
            <media:text type="plain"><xsl:value-of select="plot"/></media:text>
            <media:rating><xsl:value-of select="certification"/></media:rating>
        </media:content>
    </item>
</xsl:template>


</xsl:stylesheet>
