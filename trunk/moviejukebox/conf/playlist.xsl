<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="text" indent="no" omit-xml-declaration="yes"/>

<xsl:template match="/">

<!-- If you want to add clips at the beginning, they go here -->
<!-- eg:
Intro 0|0|0|file:///opt/sybhttpd/localhost.drives/HARD_DISK/foo/bar.avi|
-->

<!-- Here's an example that plays a different clip based on the audio codec of the movie
<xsl:choose>
    <xsl:when test="details/movie[1]/audioCodec = AAC">
        DDIntro 0|0|0|file:///opt/sybhttpd/localhost.drives/HARD_DISK/foo/DDIntro.avi|
    </xsl:when>
    
    <xsl:when test="details/movie[1]/audioCodec = MP3">
        MP3Intro 0|0|0|file:///opt/sybhttpd/localhost.drives/HARD_DISK/foo/MP3Intro.avi|
    </xsl:when>
</xsl:choose>
-->

<!-- Here's another example that plays an audio clip based on the audio codec, but it does it a different way.
     It expects the name of the audio codec to be part of the clip's filename. Easier or harder? You decide!
<xsl:if test="UNKNOWN != details/movie[1]/audioCodec">
    Audio Intro 0|0|0|file://opt/sybhttpd/localhost.drives/HARD_DISK/audioclips/<xsl:value-of select="details/movie/audioCodec"/>.avi|
</xsl:if>
-->

<xsl:for-each select="details/movie/files/file">
    <xsl:value-of select="//details/movie/title" />
    <xsl:text> </xsl:text>
    <xsl:value-of select="position()" />
    <xsl:text>|0|0|</xsl:text>
    <xsl:value-of select="//details/preferences/mjb.myiHome.IP" />
    <xsl:value-of select="fileURL" />|
</xsl:for-each>

<!-- If you want to add clips at the end, they go here -->

</xsl:template>
</xsl:stylesheet>
