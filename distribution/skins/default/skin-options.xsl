<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="xml" omit-xml-declaration="yes" />

<!-- Changing options in this file will only require the HTML pages to be regenerated, not the XML files -->

<!-- *****************Index Page Options********************** -->

<!-- Index Option #1: Show movie year after title -->
<xsl:variable name="skin-year">false</xsl:variable>

<!-- Index Option #2: Show TV show year after title -->
<xsl:variable name="skin-yearTV">false</xsl:variable>

<!-- Index Option #3: Show movie (certification) rating after title (and year if applicable) -->
<xsl:variable name="skin-certification">false</xsl:variable>

<!-- Index Option #4:  This turns parental controls on in the index -->
<xsl:variable name="parental-control-on">false</xsl:variable>

<!--Index Option #5:  This sets the number of children you have, Only applies if you have parental-control-on set to true.  The value can only be 1,2 or 3 currently-->
<xsl:variable name="number-of-children">3</xsl:variable>

<!-- Index Option #6: Age of your youngest child, if applicable (NOTE: use this one if you only have one child)-->
<xsl:variable name="childs-age-youngest">8</xsl:variable>

<!-- Index Option #7: Age of your middle child, if applicable-->
<xsl:variable name="childs-age-middle">12</xsl:variable>

<!-- Index Option #8: Age of your oldest child, if applicable-->
<xsl:variable name="childs-age-oldest">14</xsl:variable>

<!-- Index Option #9: This is the password to give to your middle child (if applicable)-->
<xsl:variable name="parental-password-middle">aaa</xsl:variable>

<!-- Index Option #10: This is the password to give to your oldest child-->
<xsl:variable name="parental-password-oldest">jjj</xsl:variable>

<!-- Index Option #11: This is the master password for parental controls, it works on all movies-->
<xsl:variable name="parental-password-master">ttt</xsl:variable>

<!-- ************Detail Page Options***************************** -->

<!-- Detail Option #1: Chooses whether to use fanart on the details page or not-->
<xsl:variable name="use-fanart">false</xsl:variable>

<!-- Detail Option #2: List TV episodes in reversed order (last to first) -->
<xsl:variable name="skin-reverseEpisodeOrder">false</xsl:variable>

<!-- Detail Option #3: Where to show Play All link on the details page -->
<!-- Valid values are: top, bottom, false/none, where top is now the default for movies -->
<xsl:variable name="skin-playAllMovie">top</xsl:variable>
<xsl:variable name="skin-playAllTV">bottom</xsl:variable>

<!-- Detail Option #4: Text to use for the Play All link on the details page -->
<xsl:variable name="skin-playAllText">PLAY ALL</xsl:variable>

<!-- Detail Option #5: Change the <BR> by what you want in the plot -->
<xsl:variable name="skin-PlotLineBreak">&lt;br&gt;</xsl:variable>

</xsl:stylesheet>
