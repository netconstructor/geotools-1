package org.geotools.data.shapefile.indexed.attribute;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;

/**
 * <P>
 * Class to manage a summary for attribute indexes.
 * </P>
 * <P>
 * It's just map that associates attribute name with index file. Using the name
 * instead of its position on dbf permits to abstract from position, so
 * attribute order can change with no influence on indexes.
 * </P>
 * 
 * @author Manuele Ventoruzzo
 */
public class AttributeIndexSummary {

    public static final String SUMMARY_EXT = ".ids";

    public static final String INDEX_EXT = ".i";

    public static final DecimalFormat SUFFIX = new DecimalFormat("00");

    public static final int DEFAULT_CACHE_SIZE = 134217728; // 128MB

    /** Url of summary file */
    protected URL summaryURL   = null;

    protected String filename  = null;

    protected int cacheSize;

    /**
     * Creates an IndexSummary
     * 
     * @param shapefileUrl
     *                url of shapefile for wich indexes are related to
     */
    public AttributeIndexSummary(URL shpURL) throws MalformedURLException, IOException {
        this(shpURL,DEFAULT_CACHE_SIZE);
    }

    /**
     * Creates an IndexSummary
     * 
     * @param shapefileUrl
     *                url of shapefile for wich indexes are related to
     * @param cacheSize
     *                maximum amount of memory to be used for index creation
     */
    public AttributeIndexSummary(URL shpURL, int cacheSize) throws MalformedURLException, IOException {
        try {
            filename = java.net.URLDecoder.decode(shpURL.toString(),
                    "US-ASCII");
            filename = filename.substring(0, filename.lastIndexOf(".shp"));
        } catch (java.io.UnsupportedEncodingException use) {
            throw new java.net.MalformedURLException("Unable to decode " + shpURL + " cause " + use.getMessage());
        }
        int indexslash = filename.lastIndexOf(File.pathSeparator);

        if (indexslash == -1) {
            indexslash = 0;
        }
        summaryURL = new URL(filename + SUMMARY_EXT);
        // create summary file (if it doesn't exist) empty
        new File(summaryURL.getFile()).createNewFile();
        this.cacheSize = cacheSize;
    }

    /**
     * Index creation. Adds attribute name to summary and invokes attribute
     * index creation.
     * 
     * @param attribute
     */
    public void createIndex(String attribute) throws FileNotFoundException, IOException {
        URL url = getIndexURL(attribute);
        if (url==null) {
            addIndex(attribute);
            url = getIndexURL(attribute);
        }
        synchronized (this) {
            // now invokes AttributeIndexWriter to create the index
            File f = new File(url.getFile());
            if (f.exists()) {
                if (!f.delete())
                    throw new IOException("File index cannot be deleted. Probably it's locked.");
            }
            RandomAccessFile raf = new RandomAccessFile(f, "rw");
            FileChannel writeChannel = raf.getChannel();
            AttributeIndexWriter indexWriter = new AttributeIndexWriter(attribute, writeChannel, getDBFChannel(),cacheSize);
            indexWriter.buildIndex();
        }
    }

    /**
     * Returns the index for specified attribute
     * 
     * @param attribute
     *                attribute to search for
     * @return Index reader or null if such attribute doesn't have an index
     */
    public AttributeIndexReader getIndex(String attribute) throws FileNotFoundException, IOException {
        URL url = getIndexURL(attribute);
        if (url == null)
            return null;
        File f = new File(url.getFile());
        if (!f.exists())
            return null;
        RandomAccessFile raf = new RandomAccessFile(new File(url.getFile()), "r");
        return new AttributeIndexReader(attribute, raf.getChannel());
    }

    public boolean hasIndex(String attribute) throws FileNotFoundException, IOException {
        URL url = getIndexURL(attribute);
        if (url == null)
            return false;
        return (new File(url.getFile())).exists();
    }

    /**
     * Tests whether an index for this attribute exists.
     * 
     * @param attribute
     * @return
     */
    public boolean existsIndex(String attribute) throws FileNotFoundException, IOException {
        URL url = getIndexURL(attribute);
        if (url == null)
            return false;
        File f = new File(url.getFile());
        return f.exists();
    }

    /**
     * Returns the index URL for specified attribute
     * 
     * @param attribute
     *                attribute to search for
     * @return URL to index file or null if such attribute doesn't have an index
     */
    protected URL getIndexURL(String attribute) throws FileNotFoundException, IOException {
        File f = new File(summaryURL.getFile());
        BufferedReader in = new BufferedReader(new FileReader(f));
        int count = 0;
        while (in.ready()) {
            String s = in.readLine();
            count++;
            if (s.equals(attribute)) {
                // index name: filename + number of row in index summary +
                // extension
                return new URL(filename+INDEX_EXT+SUFFIX.format(count));
            }
        }
        return null; // index not found
    }

    protected synchronized void addIndex(String attribute) throws FileNotFoundException, IOException {
        File f = new File(summaryURL.getFile());
        PrintWriter out = new PrintWriter(new FileWriter(f,true));
        out.println(attribute);
        out.flush();
        out.close();
    }

    protected FileChannel getDBFChannel() throws FileNotFoundException, MalformedURLException {
        URL url = new URL(filename+".dbf");
        File f = new File(url.getFile());
        if (!f.exists())
            url = new URL(filename+".DBF");
        f = new File(url.getFile());
        if (!f.exists())
            throw new FileNotFoundException("DBF file not found");
        RandomAccessFile raf = new RandomAccessFile(f, "r");
        return raf.getChannel();
    }

}