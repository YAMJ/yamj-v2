<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="xml" omit-xml-declaration="yes" />

<!-- Changing options in this file will only require the HTML pages to be regenerated, not the XML files -->

<!-- *****************Index Page Options********************** -->

<!-- Index Option #1: Show movie year after title -->
<xsl:variable name="skin-year">false</xsl:variable>

<!-- Index Option #2: Show TV show year after title -->
<xsl:variable name="skin-yearTV">false</xsl:variable>

<!-- Index Option #3: Show movie (certification) rating after title (and year if applicable) -->
<xsl:variable name="skin-certification">true</xsl:variable>



<!-- ************Detail Page Options***************************** -->

<!-- Detail Option #1: List TV episodes in reversed order (last to first) -->
<xsl:variable name="skin-reverseEpisodeOrder">false</xsl:variable>

<!-- Detail Option #2: Where to show Play All link on the details page -->
<!-- Valid values are: top, bottom, false/none, where top is now the default for movies -->
<xsl:variable name="skin-playAllMovie">top</xsl:variable>
<xsl:variable name="skin-playAllTV">bottom</xsl:variable>

<!-- Detail Option #3: Text to use for the Play All link on the details page -->
<xsl:variable name="skin-playAllText">PLAY ALL</xsl:variable>

<!-- Change the <BR> by what you want in plot -->
<xsl:variable name="skin-PlotLineBreak">&lt;br&gt;</xsl:variable>

</xsl:stylesheet>
