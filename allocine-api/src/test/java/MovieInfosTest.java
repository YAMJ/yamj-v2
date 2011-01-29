import java.io.File;
import java.util.Arrays;
import java.util.List;

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

    MovieInfos movieinfos = null;
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        File file  = new File("src/test/java/avatar.xml");
        movieinfos = XMLAllocineAPIHelper.getMovieInfos(file); 
    }

    /**
     * Test method for {@link com.moviejukebox.allocine.MovieInfos#getSynopsis()}.
     */
    @Test
    public void testGetSynopsis() {
        String validStart = "Malgré sa paralysie, Jake Sully";
        String validEnd   = "sauve la vie de Jake...";

        String synopsis   = movieinfos.getSynopsis();

        Assert.assertEquals(993, synopsis.length());
        Assert.assertTrue(synopsis.startsWith(validStart));
        Assert.assertTrue(synopsis.endsWith(validEnd));
    }

    /**
     * Test method for {@link com.moviejukebox.allocine.MovieInfos#getRating()}.
     */
    @Test
    public void testGetRating() {
        Assert.assertEquals(85, movieinfos.getRating());
    }

    /**
     * Test method for {@link com.moviejukebox.allocine.MovieInfos#getCertification()}.
     */
    @Test
    public void testGetCertification() {
        Assert.assertEquals("All", movieinfos.getCertification());
    }

    /**
     * Test method for {@link com.moviejukebox.allocine.MovieInfos#getActors()}.
     */
    @Test
    public void testGetActors() {
         Assert.assertEquals(42, movieinfos.getActors().size());
    }

    /**
     * Test method for {@link com.moviejukebox.allocine.MovieInfos#getDirectors()}.
     */
    @Test
    public void testGetDirectors() {
        Assert.assertEquals(1, movieinfos.getDirectors().size());
        Assert.assertEquals("James Cameron", movieinfos.getDirectors().toArray()[0]);
    }

    /**
     * Test method for {@link com.moviejukebox.allocine.MovieInfos#getWriters()}.
     */
    @Test
    public void testGetWriters() {
        Assert.assertEquals(1, movieinfos.getWriters().size());
        Assert.assertEquals("James Cameron", movieinfos.getWriters().toArray()[0]);
    }

    /**
     * Test method for {@link com.moviejukebox.allocine.jaxb.Movie#getTitle()}.
     */
    @Test
    public void testGetTitle() {
        Assert.assertEquals("Avatar", movieinfos.getTitle());
    }

    /**
     * Test method for {@link com.moviejukebox.allocine.jaxb.Movie#getOriginalTitle()}.
     */
    @Test
    public void testGetOriginalTitle() {
        Assert.assertEquals("Avatar", movieinfos.getOriginalTitle());
    }

    /**
     * Test method for {@link com.moviejukebox.allocine.jaxb.Movie#getProductionYear()}.
     */
    @Test
    public void testGetProductionYear() {
        Assert.assertEquals("2009", movieinfos.getProductionYear());
    }

    /**
     * Test method for {@link com.moviejukebox.allocine.jaxb.Movie#getRuntime()}.
     */
    @Test
    public void testGetRuntime() {
        Assert.assertEquals(9720, movieinfos.getRuntime());
    }

    /**
     * Test method for {@link com.moviejukebox.allocine.jaxb.Movie#getRelease()}.
     */
    @Test
    public void testGetRelease() {
        Release release = movieinfos.getRelease();
        Assert.assertEquals("2010-09-01", release.getReleaseDate());
        Assert.assertEquals("Twentieth Century Fox France", release.getDistributor().getName());
    }

    /**
     * Test method for {@link com.moviejukebox.allocine.jaxb.Movie#getCode()}.
     */
    @Test
    public void testGetCode() {
        Assert.assertEquals(61282, movieinfos.getCode());
    }

    /**
     * Test method for {@link com.moviejukebox.allocine.jaxb.Movie#getNationalityList()}.
     */
    @Test
    public void testGetNationalityList() {
        Assert.assertEquals(1, movieinfos.getNationalityList().size());
        Assert.assertEquals("U.S.A.", movieinfos.getNationalityList().get(0));
    }

    /**
     * Test method for {@link com.moviejukebox.allocine.jaxb.Movie#getGenreList()}.
     */
    @Test
    public void testGetGenreList() {
        List<String> validGenres = Arrays.asList(new String[] { "Science fiction", "Aventure" });
        Assert.assertEquals(2, movieinfos.getGenreList().size());
        Assert.assertEquals(validGenres, movieinfos.getGenreList());
    }

}
