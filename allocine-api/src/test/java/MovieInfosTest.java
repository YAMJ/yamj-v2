import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.moviejukebox.allocine.MovieInfos;
import com.moviejukebox.allocine.XMLAllocineAPIHelper;
import com.moviejukebox.allocine.jaxb.Release;

/**
 * @author Yves Blusseau
 * 
 */
public class MovieInfosTest {

    File       avatarFile  = null;
    File       smple1File  = null;
    MovieInfos avatarInfos = null;
    MovieInfos smple1Infos = null;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        avatarFile = new File("src/test/java/avatar.xml");
        smple1File = new File("src/test/java/sample1.xml");
        avatarInfos = XMLAllocineAPIHelper.getMovieInfos(avatarFile);
        smple1Infos = XMLAllocineAPIHelper.getMovieInfos(smple1File);
    }

    /**
     * Test method for {@link com.moviejukebox.allocine.XMLAllocineAPIHelper#getMovieInfos()}.
     */
    @Test
    public void testGetMovieInfos() throws Exception {
        File noresultFile = new File("src/test/java/noresult.xml");
        MovieInfos noresultInfos = XMLAllocineAPIHelper.getMovieInfos(noresultFile);
        Assert.assertNull(noresultInfos);
    }

    /**
     * Test method for {@link com.moviejukebox.allocine.MovieInfos#getSynopsis()}.
     */
    @Test
    public void testGetSynopsis() {
        String validStart = "Malgré sa paralysie, Jake Sully";
        String validEnd   = "sauve la vie de Jake...";

        String avatarSynopsis = avatarInfos.getSynopsis();
        Assert.assertEquals(993, avatarSynopsis.length());
        Assert.assertTrue(avatarSynopsis.startsWith(validStart));
        Assert.assertTrue(avatarSynopsis.endsWith(validEnd));

        String smple1Synopsis = smple1Infos.getSynopsis();
        Assert.assertEquals("Un synopsis html avec des sauts de lignes et de multiples espaces...", smple1Synopsis);

    }

    /**
     * Test method for {@link com.moviejukebox.allocine.MovieInfos#getRating()}.
     */
    @Test
    public void testGetRating() {
        Assert.assertEquals(85, avatarInfos.getRating());
        Assert.assertEquals(-1, smple1Infos.getRating());
    }

    /**
     * Test method for {@link com.moviejukebox.allocine.MovieInfos#getCertification()}.
     */
    @Test
    public void testGetCertification() {
        Assert.assertEquals("All", avatarInfos.getCertification());
        Assert.assertEquals("12", smple1Infos.getCertification());
    }

    /**
     * Test method for {@link com.moviejukebox.allocine.MovieInfos#getActors()}.
     */
    @Test
    public void testGetActors() {
        Assert.assertEquals(42, avatarInfos.getActors().size());
    }

    /**
     * Test method for {@link com.moviejukebox.allocine.MovieInfos#getDirectors()}.
     */
    @Test
    public void testGetDirectors() {
        Assert.assertEquals(1, avatarInfos.getDirectors().size());
        Assert.assertEquals("James Cameron", avatarInfos.getDirectors().toArray()[0]);
    }

    /**
     * Test method for {@link com.moviejukebox.allocine.MovieInfos#getWriters()}.
     */
    @Test
    public void testGetWriters() {
        Assert.assertEquals(1, avatarInfos.getWriters().size());
        Assert.assertEquals("James Cameron", avatarInfos.getWriters().toArray()[0]);
        Assert.assertEquals(1, smple1Infos.getWriters().size());
        Assert.assertEquals("Yves Blusseau", smple1Infos.getWriters().toArray()[0]);
    }

    /**
     * Test method for {@link com.moviejukebox.allocine.jaxb.Movie#getTitle()}.
     */
    @Test
    public void testGetTitle() {
        Assert.assertEquals("Avatar", avatarInfos.getTitle());
    }

    /**
     * Test method for {@link com.moviejukebox.allocine.jaxb.Movie#getOriginalTitle()}.
     */
    @Test
    public void testGetOriginalTitle() {
        Assert.assertEquals("Avatar", avatarInfos.getOriginalTitle());
    }

    /**
     * Test method for {@link com.moviejukebox.allocine.jaxb.Movie#getProductionYear()}.
     */
    @Test
    public void testGetProductionYear() {
        Assert.assertEquals("2009", avatarInfos.getProductionYear());
    }

    /**
     * Test method for {@link com.moviejukebox.allocine.jaxb.Movie#getRuntime()}.
     */
    @Test
    public void testGetRuntime() {
        Assert.assertEquals(9720, avatarInfos.getRuntime());
    }

    /**
     * Test method for {@link com.moviejukebox.allocine.jaxb.Movie#getRelease()}.
     */
    @Test
    public void testGetRelease() {
        Release release = avatarInfos.getRelease();
        Assert.assertEquals("2010-09-01", release.getReleaseDate());
        Assert.assertEquals("Twentieth Century Fox France", release.getDistributor().getName());
    }

    /**
     * Test method for {@link com.moviejukebox.allocine.jaxb.Movie#getCode()}.
     */
    @Test
    public void testGetCode() {
        Assert.assertEquals(61282, avatarInfos.getCode());
    }

    /**
     * Test method for {@link com.moviejukebox.allocine.jaxb.Movie#getNationalityList()}.
     */
    @Test
    public void testGetNationalityList() {
        Assert.assertEquals(1, avatarInfos.getNationalityList().size());
        Assert.assertEquals("U.S.A.", avatarInfos.getNationalityList().get(0));
    }

    /**
     * Test method for {@link com.moviejukebox.allocine.jaxb.Movie#getGenreList()}.
     */
    @Test
    public void testGetGenreList() {
        List<String> validGenres = Arrays.asList(new String[] { "Science fiction", "Aventure" });
        Assert.assertEquals(2, avatarInfos.getGenreList().size());
        Assert.assertEquals(validGenres, avatarInfos.getGenreList());
    }

    /**
     * Test method for {@link com.moviejukebox.allocine.jaxb.Movie#getPosterUrls()}.
     */
    @Test
    public void testGetPosterUrl() {
        HashSet<String> validPosterUrls = new HashSet<String>();
        validPosterUrls.add("http://images.allocine.fr/medias/nmedia/18/78/95/70/19485155.jpg");
        validPosterUrls.add("http://images.allocine.fr/medias/nmedia/18/64/43/65/19211318.jpg");
        validPosterUrls.add("http://images.allocine.fr/medias/nmedia/18/64/43/65/19150275.jpg");
        validPosterUrls.add("http://images.allocine.fr/medias/nmedia/18/64/43/65/19149062.jpg");
        Assert.assertEquals(4, avatarInfos.getPosterUrls().size());
        Assert.assertEquals(validPosterUrls, avatarInfos.getPosterUrls());
    }

}
