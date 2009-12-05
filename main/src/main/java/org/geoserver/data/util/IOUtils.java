package org.geoserver.data.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.geotools.util.logging.Logging;

/**
 * Utility class for IO related utilities
 * 
 * @author Andrea Aime - TOPP
 * 
 */
public class IOUtils {
    private static final Logger LOGGER = Logging.getLogger(IOUtils.class);
    
    private IOUtils() {
        // singleton
    }

    /**
     * Copies the provided input stream onto a file
     * 
     * @param from
     * @param to
     * @throws IOException
     */
    public static void copy(InputStream from, File to) throws IOException {
        OutputStream out = null;
        try {
            out = new FileOutputStream(to);

            byte[] buffer = new byte[1024 * 16];
            int bytes = 0;
            while ((bytes = from.read(buffer)) != -1)
                out.write(buffer, 0, bytes);

            out.flush();
        } finally {
            from.close();
            out.close();
        }
    }

    /**
     * Copies from a file to another by performing a filtering on certain
     * specified tokens. In particular, each key in the filters map will be
     * looked up in the reader as ${key} and replaced with the associated value.
     * @param to
     * @param filters
     * @param reader
     * 
     * @throws IOException
     */
    public static void filteredCopy(File from, File to, Map<String, String> filters)
            throws IOException {
        filteredCopy(new BufferedReader(new FileReader(from)), to, filters);
    }

    /**
     * Copies from a reader to a file by performing a filtering on certain
     * specified tokens. In particular, each key in the filters map will be
     * looked up in the reader as ${key} and replaced with the associated value.
     * @param to
     * @param filters
     * @param reader
     * 
     * @throws IOException
     */
    public static void filteredCopy(BufferedReader from, File to, Map<String, String> filters)
            throws IOException {
        BufferedWriter out = null;
        // prepare the escaped ${key} keys so that it won't be necessary to do
        // it over and over
        // while parsing the file
        Map<String, String> escapedMap = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            escapedMap.put("${" + entry.getKey() + "}", entry.getValue());
        }
        try {
            out = new BufferedWriter(new FileWriter(to));

            String line = null;
            while ((line = from.readLine()) != null) {
                for (Map.Entry<String, String> entry : escapedMap.entrySet()) {
                    line = line.replace(entry.getKey(), entry.getValue());
                }
                out.write(line);
                out.newLine();
            }
            out.flush();
        } finally {
            from.close();
            out.close();
        }
    }

    /**
     * Copies the provided file onto the specified destination file
     * 
     * @param from
     * @param to
     * @throws IOException
     */
    public static void copy(File from, File to) throws IOException {
        copy(new FileInputStream(from), to);
    }

    /**
     * Copy the contents of fromDir into toDir (if the latter is missing it will
     * be created)
     * 
     * @param fromDir
     * @param toDir
     * @throws IOException
     */
    public static void deepCopy(File fromDir, File toDir) throws IOException {
        if (!fromDir.isDirectory() || !fromDir.exists())
            throw new IllegalArgumentException("Invalid source directory "
                    + "(it's either not a directory, or does not exist");
        if (toDir.exists() && toDir.isFile())
            throw new IllegalArgumentException("Invalid destination directory, "
                    + "it happens to be a file instead");

        // create destination if not available
        if (!toDir.exists())
            if (!toDir.mkdir())
                throw new IOException("Could not create " + toDir);

        File[] files = fromDir.listFiles();
        for (File file : files) {
            File destination = new File(toDir, file.getName());
            if (file.isDirectory())
                deepCopy(file, destination);
            else
                copy(file, destination);
        }
    }

    /**
     * Creates a directory as a child of baseDir. The directory name will be
     * preceded by prefix and followed by suffix
     * 
     * @param basePath
     * @param prefix
     * @return
     * @throws IOException
     */
    public static File createRandomDirectory(String baseDir, String prefix, String suffix)
            throws IOException {
        File tempDir = File.createTempFile(prefix, suffix, new File(baseDir));
        tempDir.delete();
        if (!tempDir.mkdir())
            throw new IOException("Could not create the temp directory " + tempDir.getPath());
        return tempDir;
    }
    
    /**
     * Creates a temporary directory whose name will start by prefix
     *
     * Strategy is to leverage the system temp directory, then create a sub-directory.
     * @return
     */
    public static File createTempDirectory(String prefix) throws IOException {
        File dummyTemp = File.createTempFile("blah", null);
        String sysTempDir = dummyTemp.getParentFile().getAbsolutePath();
        dummyTemp.delete();

        File reqTempDir = new File(sysTempDir + File.separator + prefix + Math.random());
        reqTempDir.mkdir();

        return reqTempDir;
    }

    /**
     * Recursively deletes the contents of the specified directory, 
     * and finally wipes out the directory itself. For each
     * file that cannot be deleted a warning log will be issued. 
     * 
     * @param dir
     * @throws IOException
     * @returns true if the directory could be deleted, false otherwise
     */
    public static boolean delete(File directory) throws IOException {
        emptyDirectory(directory);
        return directory.delete();
    }

    /**
     * Recursively deletes the contents of the specified directory 
     * (but not the directory itself). For each
     * file that cannot be deleted a warning log will be issued.
     * 
     * @param dir
     * @throws IOException
     * @returns true if all the directory contents could be deleted, false otherwise
     */
    public static boolean emptyDirectory(File directory) throws IOException {
        if (!directory.isDirectory())
            throw new IllegalArgumentException(directory
                    + " does not appear to be a directory at all...");

        boolean allClean = true;
        File[] files = directory.listFiles();

        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                allClean &= delete(files[i]);
            } else {
                if (!files[i].delete()) {
                    LOGGER.log(Level.WARNING, "Could not delete {0}", files[i].getAbsolutePath());
                    allClean = false;
                }
            }
        }
        
        return allClean;
    }
    
    /**
     * Zips up the directory contents into the specified {@link ZipOutputStream}. 
     * @param directory The directory whose contents have to be zipped up
     * @param zipout The {@link ZipOutputStream} that will be populated by the files found
     * @param filter An optional filter that can be used to select only certain files. 
     * Can be null, in that case all files in the directory will be zipped
     * @throws IOException
     * @throws FileNotFoundException
     */
    public static void zipDirectory(File directory, ZipOutputStream zipout, final FilenameFilter filter)
            throws IOException, FileNotFoundException {
        zipDirectory(directory, "", zipout, filter);
    }
    
    /**
     * See {@link #zipDirectory(File, ZipOutputStream, FilenameFilter)}, this version handles the prefix needed
     * to recursively zip data preserving the relative path of each
     */
    private static void zipDirectory(File directory, String prefix, ZipOutputStream zipout, final FilenameFilter filter)
            throws IOException, FileNotFoundException {
        File[] files = directory.listFiles(filter);
        for (File file : files) {
            if (file.exists()) {
                if(file.isDirectory()) {
                    // recurse and append 
                    zipDirectory(file, prefix + file.getName() + "/", zipout, filter);
                } else {
                    ZipEntry entry = new ZipEntry(prefix  + file.getName());
                    zipout.putNextEntry(entry);
    
                    // copy file by reading 4k at a time (faster than buffered reading)
                    InputStream in = new FileInputStream(file);
                    int c;
                    byte[] buffer = new byte[4 * 1024];
                    while (-1 != (c = in.read(buffer))) {
                        zipout.write(buffer, 0, c);
                    }
                    zipout.closeEntry();
                    in.close();
                }
            }
        }
        zipout.finish();
        zipout.flush();
    }
}
