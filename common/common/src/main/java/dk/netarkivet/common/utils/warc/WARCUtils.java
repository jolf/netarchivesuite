/* $Id$
 * $Date$
 * $Revision$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2011 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.common.utils.warc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.io.ArchiveRecord;
import org.archive.io.ArchiveRecordHeader;
import org.archive.io.warc.WARCConstants;
import org.archive.io.warc.WARCReader;
import org.archive.io.warc.WARCReaderFactory;
import org.archive.io.warc.WARCRecord;
import org.archive.io.warc.WARCWriter;
import org.archive.util.anvl.ANVLRecord;

import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.archive.ArchiveDateConverter;
import dk.netarkivet.common.utils.archive.HeritrixArchiveHeaderWrapper;

/**
* Various utilities on WARC-records.
* We have borrowed code from wayback.
* @see org.archive.wayback.resourcestore.indexer.WARCRecordToSearchResultAdapter.java
*/
public class WARCUtils {
    
    /** Logging output place. */
    protected static final Log log = LogFactory.getLog(WARCUtils.class);

    /**
     * Create new WARCWriter, writing to warcfile newFile.
     * @param newFile the WARCfile, that the WARCWriter writes to.
     * @return new WARCWriter, writing to warcfile newFile.
     */
    public static WARCWriter createWARCWriter(File newFile) {
        WARCWriter writer;
        PrintStream ps = null;
        try {
            ps = new PrintStream(new FileOutputStream(newFile));
            writer = new WARCWriter(
                    new AtomicInteger(), ps,
                    //This name is used for the first (file metadata) record
                    newFile, 
                    false, //Don't compress
                    //Use current time
                    ArchiveDateConverter.getWarcDateFormat().format(new Date()),
                    null //No particular file metadata to add
            );
        } catch (IOException e) {
            if (ps != null) {
                ps.close();
            }
            String message = "Could not create WARCWriter to file '"
                    + newFile + "'.\n";
            log.warn(message);
            throw new IOFailure(message, e);
        }
        return writer;
    }

    /** Insert the contents of a WARC file (skipping an optional initial
     *  filedesc: header) in another ARCfile. TODO
     *
     * @param warcFile An WARC file to read.
     * @param writer A place to write the arc records
     * @throws IOFailure if there are problems reading the file.
     */
    public static void insertWARCFile(File warcFile, WARCWriter writer) {
        ArgumentNotValid.checkNotNull(writer, "WARCWriter aw");
        ArgumentNotValid.checkNotNull(warcFile, "File warcFile");
        WARCReader r;

        try {
            r = WARCReaderFactory.get(warcFile);
        } catch (IOException e) {
            String message = "Error while copying ARC records from " + warcFile;
            log.warn(message, e);
            throw new IOFailure(message, e);
        }
        Iterator<ArchiveRecord> it = r.iterator();
        WARCRecord record;
        while (it.hasNext()) {
            record = (WARCRecord) it.next();
            copySingleRecord(writer, record);
        }
    }

    /**
     * Writes the given WARCRecord on the given WARCWriter.
     * 
     * Creates a new unique UUID for the copied record.
     * 
     * @param aw
     *            The WARCWriter to output the record on.
     * @param record
     *            The record to output
     */
    private static void copySingleRecord(WARCWriter aw, WARCRecord record) {
        try {
        	//Prepare metadata...
        	HeritrixArchiveHeaderWrapper header = HeritrixArchiveHeaderWrapper.wrapArchiveHeader(null, record);
        	String warcType = header.getHeaderStringValue("WARC-Type");

            String url = header.getUrl();
            Date date = header.getDate();
            String dateStr = ArchiveDateConverter.getWarcDateFormat()
                    .format(date);
            String mimetype = header.getMimetype();
            URI recordId;
    		try {
    			recordId = new URI("urn:uuid:" + UUID.randomUUID().toString());
    		} catch (URISyntaxException e) {
    			throw new IllegalState("Epic fail creating URI from UUID!");
    		}
            String ip = header.getIp();

            // TODO this throws away all original headers! Improve.
    		ANVLRecord  namedFields = new ANVLRecord();

    		InputStream in = record;
    		// getContentBegin only works for WARC and in H1.44.x!
    		Long payloadLength = header.getLength() - record.getHeader().getContentBegin();

    		// Worst API EVER!
    		if ("metadata".equals(warcType)) {
    			aw.writeMetadataRecord(url, dateStr, mimetype, recordId, namedFields, in, payloadLength);
    		} else if ("request".equals(warcType)) {
                aw.writeRequestRecord(url, dateStr, mimetype, recordId, namedFields, in, payloadLength);
        	} else if ("resource".equals(warcType)) {
                aw.writeResourceRecord(url, dateStr, mimetype, recordId, namedFields, in, payloadLength);
    		} else if ("response".equals(warcType)) {
                aw.writeResponseRecord(url, dateStr, mimetype, recordId, namedFields, in, payloadLength);
			} else if ("revisit".equals(warcType)) {
	            aw.writeRevisitRecord(url, dateStr, mimetype, recordId, namedFields, in, payloadLength);
			} else if ("warcinfo".equals(warcType)) {
	            aw.writeWarcinfoRecord(dateStr, mimetype, recordId, namedFields, in, payloadLength);
			} else {
				throw new IOFailure("Unknown WARC-Type!");
			}
        } catch (Exception e) {
            throw new IOFailure("Error occurred while writing an WARC record"
                    + record, e);
        }
    }

    /**
     * Read the contents (payload) of an WARC record into a byte array.
     * 
     * @param record
     *            An WARC record to read from. After reading, the WARC Record 
     *            will no longer have its own data available for reading.
     * @return A byte array containing the payload of the WARC record. Note 
     *         that the size of the payload is calculated by subtracting
     *         the contentBegin value from the length of the record (both values
     *         included in the record header).
     * @throws IOFailure
     *             If there is an error reading the data, or if the record is
     *             longer than Integer.MAX_VALUE (since we can't make bigger
     *             arrays).
     */
    public static byte[] readWARCRecord(WARCRecord record) throws IOFailure {
        ArgumentNotValid.checkNotNull(record, "WARCRecord record");
        if (record.getHeader().getLength() > Integer.MAX_VALUE) {
            throw new IOFailure("WARC Record too long to fit in array: "
                    + record.getHeader().getLength() + " > "
                    + Integer.MAX_VALUE);
        }
        // Calculate the length of the payload.
        // the size of the payload is calculated by subtracting
        // the contentBegin value from the length of the record.
        
        ArchiveRecordHeader header = record.getHeader();
        long length = header.getLength();
        
        int payloadLength = (int) (length - header.getContentBegin()); 
                
        // read from stream
        byte[] tmpbuffer = new byte[payloadLength];
        byte[] buffer = new byte[Constants.IO_BUFFER_SIZE];
        int bytesRead;
        int totalBytes = 0;
        try {
            for (; (totalBytes < payloadLength)
                    && ((bytesRead = record.read(buffer)) != -1); totalBytes += bytesRead) {
                System.arraycopy(buffer, 0, tmpbuffer, totalBytes, bytesRead);
            }
        } catch (IOException e) {
            throw new IOFailure("Failure when reading the WARC-record", e);
        }
        
        // Check if the number of bytes read (= totalbytes) matches the
        // size of the buffer.
        if (tmpbuffer.length != totalBytes) {
            // make sure we only return an array with bytes we actually read
            byte[] truncateBuffer = new byte[totalBytes];
            System.arraycopy(tmpbuffer, 0, truncateBuffer, 0, totalBytes);
            log.debug("Storing " + totalBytes + " bytes. Expected to store: "
                    + tmpbuffer.length);
            return truncateBuffer;
        } else {
            return tmpbuffer;
        }

    }
    
    /**
     * Find out what type of WARC-record this is.
     * @param record a given WARCRecord
     * @return the type of WARCRecord as a String.
     */
    public static String getRecordType(WARCRecord record) {
        ArchiveRecordHeader header = record.getHeader();
        return (String) header.getHeaderValue(WARCConstants.HEADER_KEY_TYPE);
    }
    
}
