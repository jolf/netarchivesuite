/* File:                 $Id$
 * Revision:         $Revision$
 * Author:                $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.archive.bitarchive;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import junit.framework.TestCase;
import org.archive.io.arc.ARCRecord;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.JMSConnectionTestMQ;
import dk.netarkivet.common.distribute.TestRemoteFile;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.arc.ARCBatchJob;
import dk.netarkivet.common.utils.arc.BatchFilter;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;


/**
 * Unit test for Bitarchive API
 * The logging of bitarchive opertions is tested
*/
public class BitarchiveTesterLog extends TestCase {
    private UseTestRemoteFile rf = new UseTestRemoteFile();

    protected final Logger log = Logger.getLogger(getClass().getName());

    /**
     * The properties-file containg properties for logging in unit-tests
     */
    private static final File TESTLOGPROP = new File("tests/dk/netarkivet/testlog.prop");

    private static File ARCHIVE_DIR =
            new File("tests/dk/netarkivet/archive/bitarchive/data/log/working/");
    private static File EXISTING_ARCHIVE_DIR =
            new File("tests/dk/netarkivet/archive/bitarchive/data/log/existing/");
    private static File LOG_FILE = new File("tests/testlogs", "netarkivtest.log");
    private static String ARC_FILE_NAME1 = "Upload1.ARC";
    private static String ARC_FILE_NAME2 = "Upload2.ARC";
    private static File ARC_FILE =
            new File("tests/dk/netarkivet/archive/bitarchive/data/log/originals/", ARC_FILE_NAME1);
    private Bitarchive archive;

    public BitarchiveTesterLog(String sTestName) {
        super(sTestName);
    }

    protected void setUp() {
        JMSConnectionTestMQ.useJMSConnectionTestMQ();
        try {
            // This forces an emptying of the log file.
            FileInputStream fis = new FileInputStream(TESTLOGPROP);
            LogManager.getLogManager().readConfiguration(fis);
            fis.close();
        } catch (IOException e) {
            fail("Could not load the testlog.prop file: " + e);
        }
        FileUtils.removeRecursively(ARCHIVE_DIR);
        rf.setUp();
        try {
            TestFileUtils.copyDirectoryNonCVS(EXISTING_ARCHIVE_DIR, ARCHIVE_DIR);
            Settings.set(Settings.BITARCHIVE_SERVER_FILEDIR, ARCHIVE_DIR.getAbsolutePath());
            archive = Bitarchive.getInstance();
        } catch (Exception e) {
            fail("Error copying bit archive: " + e);
        }
    }

    protected void tearDown() {
        archive.close();
        JMSConnectionTestMQ.clearTestQueues();
        rf.tearDown();
        Settings.reload();
        FileUtils.removeRecursively(ARCHIVE_DIR);
    }

    /** Asserts that a source string does not contain a given string, and prints
     * out the source string if the target string is found.
     *
     * @param msg An explanatory message
     * @param src A string to search through
     * @param str A string to search for
     */
    private void assertNotStringContains(String msg, String src, String str) {
        int index = src.indexOf(str);
        if (index != -1) {
            System.out.println("Actual string: ");
            System.out.println(src);
            assertEquals(msg, -1, index);
        }
    }

    /**
     * Test logging of upload command
     */
    public void testLogUpload() {
        try {
            String logtxt = FileUtils.readFile(LOG_FILE);
            assertNotStringContains("Log does not contain file before uploading.",
                    logtxt, ARC_FILE_NAME1); // clean log

            archive.upload(new TestRemoteFile(ARC_FILE, false, false, false), ARC_FILE.getName());

            FileAsserts.assertFileContains("Log contains file after uploading.",
                    ARC_FILE_NAME1, LOG_FILE);
            FileAsserts.assertFileContains("Log contains the word upload after uploading.",
                    "upload", LOG_FILE);
        } catch (IOException e) {
            fail("Exception while reading log file: " + e);
        }
    }

    /**
     * test logging of get
     */
    public void testLogGet() {
        try {
            String logtxt = FileUtils.readFile(LOG_FILE);
            assertNotStringContains("Unuploaded files are not mentioned in the log.",
                    logtxt, ARC_FILE_NAME2); // clean log

            archive.get(ARC_FILE_NAME2, 0);

            FileAsserts.assertFileContains("Log contains file after getting.",
                    ARC_FILE_NAME2, LOG_FILE);
            FileAsserts.assertFileContains("Log contains the word get after getting.",
                    "get", LOG_FILE);
        } catch (IOException e) {
            fail("Exception while reading log file: " + e);
        }
    }

    public void testLogBatch() {
        try {
            String logtxt = FileUtils.readFile(LOG_FILE);
            assertNotStringContains("Batch not mentioned in log before run",
                                    logtxt, "Batch"); // clean log
            // Run the empty batch job.
            try {
                archive.batch(TestInfo.baAppId, new ARCBatchJob() {
                    public BatchFilter getFilter() {
                        return BatchFilter.NO_FILTER;
                    }
                    public void initialize(OutputStream os) { }
                    public void processRecord(ARCRecord record, OutputStream os) {
                    }
                    public void finish(OutputStream os) { }
                });
            } catch (Exception e) {
                fail("Batching should not throw " + e);
            }
            FileAsserts.assertFileContains("Log contains the word 'Batch'.",
                    "Batch", LOG_FILE);
            FileAsserts.assertFileContains("Log contains the phrase 'Batch: Job'.",
                    "Batch: Job", LOG_FILE);
        } catch (IOException e) {
            fail("Exception while reading log file: " + e);
        }
    }
}
