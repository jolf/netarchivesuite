/*
 * #%L
 * Netarchivesuite - harvester
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.harvester.harvesting;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.archive.crawler.Heritrix;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.datamodel.FetchStatusCodes;
import org.archive.crawler.deciderules.recrawl.IdenticalDigestDecideRule;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.extractor.Link;
import org.archive.crawler.framework.WriterPoolProcessor;
import org.archive.crawler.settings.MapType;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;
import org.archive.io.ReplayInputStream;
import org.archive.io.WriterPoolMember;
import org.archive.io.WriterPoolSettings;
import org.archive.io.warc.WARCConstants;
import org.archive.io.warc.WARCWriter;
import org.archive.io.warc.WARCWriterPool;
import org.archive.uid.GeneratorFactory;
import org.archive.util.ArchiveUtils;
import org.archive.util.XmlUtils;
import org.archive.util.anvl.ANVLRecord;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import dk.netarkivet.harvester.datamodel.H1HeritrixTemplate;

/**
 * WARCWriterProcessor. Goes against the 0.18 version of the WARC specification (which is functionally identical to 0.17
 * except in the protocol identifier string). See http://archive-access.sourceforge.net/warc/
 * <p>
 * Based on the WARCWriterProcessor in package org.archive.crawler.writer With modifications to the WARC-info record..
 *
 * @author stack
 * @author svc
 * 
 * // template for adding this metadata to a H1 template.
/*
       <map name="metadata-items">
            <string name="harvestInfo.version">Vilhelm</string>
            <string name="harvestInfo.jobId">Caroline</string>
            <string name="harvestInfo.channel">Login</string>
			<string name="harvestInfo.harvestNum">ffff</string>                        
			<string name="harvestInfo.origHarvestDefinitionID">ffff</string>
			<string name="harvestInfo.maxBytesPerDomain">ffff</string>
			<string name="harvestInfo.maxObjectsPerDomain">ffff</string>
			
			<string name="harvestInfo.orderXMLName">Default Orderxml</string>
			<string name="harvestInfo.origHarvestDefinitionName">ddddd</string>
			<string name="harvestInfo.scheduleName">Every Hour</string>
			<string name="harvestInfo.harvestFilenamePrefix">1-1</string>
			<string name="harvestInfo.jobSubmitDate">NOW</string>
			<string name="harvestInfo.performer">performer</string>
			<string name="harvestInfo.audience">audience</string>
      </map>
*/
public class WARCWriterProcessor extends WriterPoolProcessor implements CoreAttributeConstants, CrawlStatusListener,
        WriterPoolSettings, FetchStatusCodes, WARCConstants {

   private static final Logger logger = Logger.getLogger(WARCWriterProcessor.class.getName());

   private static final long serialVersionUID = -2006725968882994351L;

     public long getDefaultMaxFileSize() {
        return 1000000000L; // 1 SI giga-byte (109 bytes), per WARC appendix A
    }

    /** Key for whether to write 'request' type records where possible */
    public static final String ATTR_WRITE_REQUESTS = "write-requests";

    /** Key for whether to write 'metadata' type records where possible */
    public static final String ATTR_WRITE_METADATA = "write-metadata";

    /**
     * Key for whether to write 'revisit' type records when consecutive identical digest
     */
    public static final String ATTR_WRITE_REVISIT_FOR_IDENTICAL_DIGESTS = "write-revisit-for-identical-digests";

    /**
     * Key for whether to write 'revisit' type records for server "304 not modified" responses
     */
    public static final String ATTR_WRITE_REVISIT_FOR_NOT_MODIFIED = "write-revisit-for-not-modified";
    /**
     * Key for metadata-items to include in the warcinfo.
     */
    public static final String ATTR_METADATA_ITEMS = "metadata-items";

    /** Default path list. */
    private static final String[] DEFAULT_PATH = {"warcs"};

    protected String[] getDefaultPath() {
        return DEFAULT_PATH;
    }
    
    private Map metadataMap;

    private static final String HARVESTINFO_VERSION = "harvestInfo.version";
    private static final String HARVESTINFO_JOBID = "harvestInfo.jobId";
    private static final String HARVESTINFO_CHANNEL = "harvestInfo.channel";

    private static final String HARVESTINFO_HARVESTNUM = "harvestInfo.harvestNum";

    private static final String HARVESTINFO_ORIGHARVESTDEFINITIONID = "harvestInfo.origHarvestDefinitionID";

    private static final String HARVESTINFO_MAXBYTESPERDOMAIN = "harvestInfo.maxBytesPerDomain";

    private static final String HARVESTINFO_MAXOBJECTSPERDOMAIN = "harvestInfo.maxObjectsPerDomain";

    private static final String HARVESTINFO_ORDERXMLNAME = "harvestInfo.orderXMLName";

    private static final String HARVESTINFO_ORIGHARVESTDEFINITIONNAME = "harvestInfo.origHarvestDefinitionName";

    private static final String HARVESTINFO_SCHEDULENAME = "harvestInfo.scheduleName";

    private static final String HARVESTINFO_HARVESTFILENAMEPREFIX = "harvestInfo.harvestFilenamePrefix";
    private static final String HARVESTINFO_JOBSUBMITDATE = "harvestInfo.jobSubmitDate";

    private static final String HARVESTINFO_PERFORMER = "harvestInfo.performer";

    private static final String HARVESTINFO_AUDIENCE = "harvestInfo.audience";

    /**
     * @param name Name of this writer.
     */
    public WARCWriterProcessor(final String name) {
        super(name, "Netarchivesuite WARCWriter processor (Version 1.0");
        Type e = addElementToDefinition(new SimpleType(ATTR_WRITE_REQUESTS,
                "Whether to write 'request' type records. Default is true.", new Boolean(true)));
        e.setOverrideable(true);
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(ATTR_WRITE_METADATA,
                "Whether to write 'metadata' type records. Default is true.", new Boolean(true)));
        e.setOverrideable(true);
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(ATTR_WRITE_REVISIT_FOR_IDENTICAL_DIGESTS,
                "Whether to write 'revisit' type records when a URI's "
                        + "history indicates the previous fetch had an identical " + "content digest. "
                        + "Default is true.", new Boolean(true)));
        e.setOverrideable(true);
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(ATTR_WRITE_REVISIT_FOR_NOT_MODIFIED,
                "Whether to write 'revisit' type records when a "
                        + "304-Not Modified response is received. Default is true.", new Boolean(true)));
        e.setOverrideable(true);
        e.setExpertSetting(true);

        // Add map setting to add NAS metadata to WarcInfo records. 
        e = addElementToDefinition(new MapType(ATTR_METADATA_ITEMS, "Metadata items.", String.class));
        e.setOverrideable(true);
        e.setExpertSetting(true);
    }

    protected void setupPool(final AtomicInteger serialNo) {
        setPool(new WARCWriterPool(serialNo, this, getPoolMaximumActive(), getPoolMaximumWait()));
    }

    
    /**
     * Writes a CrawlURI and its associated data to store file.
     * <p>
     * Currently this method understands the following uri types: dns, http, and https.
     *
     * @param curi CrawlURI to process.
     */
    protected void innerProcess(CrawlURI curi) {
        // If failure, or we haven't fetched the resource yet, return
        if (curi.getFetchStatus() <= 0) {
            return;
        }

        // If no recorded content at all, don't write record. Except FTP, which
        // can have empty content, since the "headers" don't count as content.
        String scheme = curi.getUURI().getScheme().toLowerCase();
        long recordLength = curi.getContentSize();
        if (recordLength <= 0 && !scheme.equals("ftp")) {
            // getContentSize() should be > 0 if any material (even just
            // HTTP headers with zero-length body) is available.
            return;
        }

        try {
            if (shouldWrite(curi)) {
                write(scheme, curi);
            } else {
                logger.info("This writer does not write out scheme " + scheme + " content");
            }
        } catch (IOException e) {
            curi.addLocalizedError(this.getName(), e, "WriteRecord: " + curi.toString());
            logger.log(Level.SEVERE, "Failed write of Record: " + curi.toString(), e);
        }
    }

    protected void write(final String lowerCaseScheme, final CrawlURI curi) throws IOException {
        logger.info("writing warc record for " + curi);
        WriterPoolMember writer = getPool().borrowFile();
        long position = writer.getPosition();
        // See if we need to open a new file because we've exceeed maxBytes.
        // Call to checkFileSize will open new file if we're at maximum for
        // current file.
        writer.checkSize();
        if (writer.getPosition() != position) {
            // We just closed the file because it was larger than maxBytes.
            // Add to the totalBytesWritten the size of the first record
            // in the file, if any.
            setTotalBytesWritten(getTotalBytesWritten() + (writer.getPosition() - position));
            position = writer.getPosition();
        }

        WARCWriter w = (WARCWriter) writer;
        try {
            // Write a request, response, and metadata all in the one
            // 'transaction'.
            final URI baseid = getRecordID();
            final String timestamp = ArchiveUtils.getLog14Date(curi.getLong(A_FETCH_BEGAN_TIME));
            if (lowerCaseScheme.startsWith("http")) {
                writeHttpRecords(w, curi, baseid, timestamp);
            } else if (lowerCaseScheme.equals("dns")) {
                writeDnsRecords(w, curi, baseid, timestamp);
            } else if (lowerCaseScheme.equals("ftp")) {
                writeFtpRecords(w, curi, baseid, timestamp);
            } else {
                logger.warning("No handler for scheme " + lowerCaseScheme);
            }
        } catch (IOException e) {
            // Invalidate this file (It gets a '.invalid' suffix).
            getPool().invalidateFile(writer);
            // Set the writer to null otherwise the pool accounting
            // of how many active writers gets skewed if we subsequently
            // do a returnWriter call on this object in the finally block.
            writer = null;
            throw e;
        } finally {
            if (writer != null) {
                setTotalBytesWritten(getTotalBytesWritten() + (writer.getPosition() - position));
                getPool().returnFile(writer);
            }
        }
        checkBytesWritten();
    }

    private void writeFtpRecords(WARCWriter w, final CrawlURI curi, final URI baseid, final String timestamp)
            throws IOException {
        ANVLRecord headers = new ANVLRecord(3);
        headers.addLabelValue(HEADER_KEY_IP, getHostAddress(curi));
        String controlConversation = curi.getString(A_FTP_CONTROL_CONVERSATION);
        URI rid = writeFtpControlConversation(w, timestamp, baseid, curi, headers, controlConversation);

        if (curi.getContentDigest() != null) {
            headers.addLabelValue(HEADER_KEY_PAYLOAD_DIGEST, curi.getContentDigestSchemeString());
        }

        if (curi.getHttpRecorder() != null) {
            if (IdenticalDigestDecideRule.hasIdenticalDigest(curi)
                    && ((Boolean) getUncheckedAttribute(curi, ATTR_WRITE_REVISIT_FOR_IDENTICAL_DIGESTS))) {
                rid = writeRevisitDigest(w, timestamp, null, baseid, curi, headers);
            } else {
                headers = new ANVLRecord(3);
                if (curi.isTruncatedFetch()) {
                    String value = curi.isTimeTruncatedFetch() ? NAMED_FIELD_TRUNCATED_VALUE_TIME : curi
                            .isLengthTruncatedFetch() ? NAMED_FIELD_TRUNCATED_VALUE_LENGTH : curi
                            .isHeaderTruncatedFetch() ? NAMED_FIELD_TRUNCATED_VALUE_HEAD :
                    // TODO: Add this to spec.
                            TRUNCATED_VALUE_UNSPECIFIED;
                    headers.addLabelValue(HEADER_KEY_TRUNCATED, value);
                }
                if (curi.getContentDigest() != null) {
                    headers.addLabelValue(HEADER_KEY_PAYLOAD_DIGEST, curi.getContentDigestSchemeString());
                }
                headers.addLabelValue(HEADER_KEY_CONCURRENT_TO, '<' + rid.toString() + '>');
                rid = writeResource(w, timestamp, curi.getContentType(), baseid, curi, headers);
            }
        }
        if (((Boolean) getUncheckedAttribute(curi, ATTR_WRITE_METADATA))) {
            headers = new ANVLRecord(1);
            headers.addLabelValue(HEADER_KEY_CONCURRENT_TO, '<' + rid.toString() + '>');
            writeMetadata(w, timestamp, baseid, curi, headers);
        }
    }

    private void writeDnsRecords(WARCWriter w, final CrawlURI curi, final URI baseid, final String timestamp)
            throws IOException {
        ANVLRecord headers = null;
        String ip = curi.getString(A_DNS_SERVER_IP_LABEL);
        if (ip != null && ip.length() > 0) {
            headers = new ANVLRecord(1);
            headers.addLabelValue(HEADER_KEY_IP, ip);
        }
        writeResponse(w, timestamp, curi.getContentType(), baseid, curi, headers);
    }

    private void writeHttpRecords(WARCWriter w, final CrawlURI curi, final URI baseid, final String timestamp)
            throws IOException {
        // Add named fields for ip, checksum, and relate the metadata
        // and request to the resource field.
        // TODO: Use other than ANVL (or rename ANVL as NameValue or
        // use RFC822 (commons-httpclient?).
        ANVLRecord headers = new ANVLRecord(5);
        if (curi.getContentDigest() != null) {
            headers.addLabelValue(HEADER_KEY_PAYLOAD_DIGEST, curi.getContentDigestSchemeString());
        }
        headers.addLabelValue(HEADER_KEY_IP, getHostAddress(curi));
        URI rid;

        if (IdenticalDigestDecideRule.hasIdenticalDigest(curi)
                && ((Boolean) getUncheckedAttribute(curi, ATTR_WRITE_REVISIT_FOR_IDENTICAL_DIGESTS))) {
            rid = writeRevisitDigest(w, timestamp, HTTP_RESPONSE_MIMETYPE, baseid, curi, headers);
        } else if (curi.getFetchStatus() == HttpStatus.SC_NOT_MODIFIED
                && ((Boolean) getUncheckedAttribute(curi, ATTR_WRITE_REVISIT_FOR_NOT_MODIFIED))) {
            rid = writeRevisitNotModified(w, timestamp, baseid, curi, headers);
        } else {
            if (curi.isTruncatedFetch()) {
                String value = curi.isTimeTruncatedFetch() ? NAMED_FIELD_TRUNCATED_VALUE_TIME : curi
                        .isLengthTruncatedFetch() ? NAMED_FIELD_TRUNCATED_VALUE_LENGTH
                        : curi.isHeaderTruncatedFetch() ? NAMED_FIELD_TRUNCATED_VALUE_HEAD :
                        // TODO: Add this to spec.
                                TRUNCATED_VALUE_UNSPECIFIED;
                headers.addLabelValue(HEADER_KEY_TRUNCATED, value);
            }
            rid = writeResponse(w, timestamp, HTTP_RESPONSE_MIMETYPE, baseid, curi, headers);
        }

        headers = new ANVLRecord(1);
        headers.addLabelValue(HEADER_KEY_CONCURRENT_TO, '<' + rid.toString() + '>');

        if (((Boolean) getUncheckedAttribute(curi, ATTR_WRITE_REQUESTS))) {
            writeRequest(w, timestamp, HTTP_REQUEST_MIMETYPE, baseid, curi, headers);
        }
        if (((Boolean) getUncheckedAttribute(curi, ATTR_WRITE_METADATA))) {
            writeMetadata(w, timestamp, baseid, curi, headers);
        }
    }

    protected URI writeFtpControlConversation(WARCWriter w, String timestamp, URI baseid, CrawlURI curi,
            ANVLRecord headers, String controlConversation) throws IOException {
        final URI uid = qualifyRecordID(baseid, TYPE, METADATA);
        byte[] b = controlConversation.getBytes("UTF-8");
        w.writeMetadataRecord(curi.toString(), timestamp, FTP_CONTROL_CONVERSATION_MIMETYPE, uid, headers,
                new ByteArrayInputStream(b), b.length);
        return uid;
    }

    protected URI writeRequest(final WARCWriter w, final String timestamp, final String mimetype, final URI baseid,
            final CrawlURI curi, final ANVLRecord namedFields) throws IOException {
        final URI uid = qualifyRecordID(baseid, TYPE, REQUEST);
        ReplayInputStream ris = curi.getHttpRecorder().getRecordedOutput().getReplayInputStream();
        try {
            w.writeRequestRecord(curi.toString(), timestamp, mimetype, uid, namedFields, ris, curi.getHttpRecorder()
                    .getRecordedOutput().getSize());
        } finally {
            if (ris != null) {
                ris.close();
            }
        }
        return uid;
    }

    protected URI writeResponse(final WARCWriter w, final String timestamp, final String mimetype, final URI baseid,
            final CrawlURI curi, final ANVLRecord namedFields) throws IOException {
        ReplayInputStream ris = curi.getHttpRecorder().getRecordedInput().getReplayInputStream();
        try {
            w.writeResponseRecord(curi.toString(), timestamp, mimetype, baseid, namedFields, ris, curi
                    .getHttpRecorder().getRecordedInput().getSize());
        } finally {
            if (ris != null) {
                ris.close();
            }
        }
        return baseid;
    }

    protected URI writeResource(final WARCWriter w, final String timestamp, final String mimetype, final URI baseid,
            final CrawlURI curi, final ANVLRecord namedFields) throws IOException {
        ReplayInputStream ris = curi.getHttpRecorder().getRecordedInput().getReplayInputStream();
        try {
            w.writeResourceRecord(curi.toString(), timestamp, mimetype, baseid, namedFields, ris, curi
                    .getHttpRecorder().getRecordedInput().getSize());
        } finally {
            if (ris != null) {
                ris.close();
            }
        }
        return baseid;
    }

    protected URI writeRevisitDigest(final WARCWriter w, final String timestamp, final String mimetype,
            final URI baseid, final CrawlURI curi, final ANVLRecord namedFields) throws IOException {
        namedFields.addLabelValue(HEADER_KEY_PROFILE, PROFILE_REVISIT_IDENTICAL_DIGEST);
        namedFields.addLabelValue(HEADER_KEY_TRUNCATED, NAMED_FIELD_TRUNCATED_VALUE_LENGTH);

        ReplayInputStream ris = null;
        long revisedLength = 0;

        // null mimetype implies no payload
        if (mimetype != null) {
            ris = curi.getHttpRecorder().getRecordedInput().getReplayInputStream();
            revisedLength = curi.getHttpRecorder().getRecordedInput().getContentBegin();
            revisedLength = revisedLength > 0 ? revisedLength : curi.getHttpRecorder().getRecordedInput().getSize();
        }

        try {
            w.writeRevisitRecord(curi.toString(), timestamp, mimetype, baseid, namedFields, ris, revisedLength);
        } finally {
            if (ris != null) {
                ris.close();
            }
        }
        curi.addAnnotation("warcRevisit:digest");
        return baseid;
    }

    protected URI writeRevisitNotModified(final WARCWriter w, final String timestamp, final URI baseid,
            final CrawlURI curi, final ANVLRecord namedFields) throws IOException {
        namedFields.addLabelValue(HEADER_KEY_PROFILE, PROFILE_REVISIT_NOT_MODIFIED);
        // save just enough context to understand basis of not-modified
        if (curi.containsKey(A_HTTP_TRANSACTION)) {
            HttpMethodBase method = (HttpMethodBase) curi.getObject(A_HTTP_TRANSACTION);
            saveHeader(A_ETAG_HEADER, method, namedFields, HEADER_KEY_ETAG);
            saveHeader(A_LAST_MODIFIED_HEADER, method, namedFields, HEADER_KEY_LAST_MODIFIED);
        }
        // truncate to zero-length (all necessary info is above)
        namedFields.addLabelValue(HEADER_KEY_TRUNCATED, NAMED_FIELD_TRUNCATED_VALUE_LENGTH);
        ReplayInputStream ris = curi.getHttpRecorder().getRecordedInput().getReplayInputStream();
        try {
            w.writeRevisitRecord(curi.toString(), timestamp, null, baseid, namedFields, ris, 0);
        } finally {
            if (ris != null) {
                ris.close();
            }
        }
        curi.addAnnotation("warcRevisit:notModified");
        return baseid;
    }

    /**
     * Save a header from the given HTTP operation into the provider headers under a new name
     *
     * @param origName header name to get if present
     * @param method http operation containing headers
     */
    protected void saveHeader(String origName, HttpMethodBase method, ANVLRecord headers, String newName) {
        Header header = method.getResponseHeader(origName);
        if (header != null) {
            headers.addLabelValue(newName, header.getValue());
        }
    }

    protected URI writeMetadata(final WARCWriter w, final String timestamp, final URI baseid, final CrawlURI curi,
            final ANVLRecord namedFields) throws IOException {
        final URI uid = qualifyRecordID(baseid, TYPE, METADATA);
        // Get some metadata from the curi.
        // TODO: Get all curi metadata.
        // TODO: Use other than ANVL (or rename ANVL as NameValue or use
        // RFC822 (commons-httpclient?).
        ANVLRecord r = new ANVLRecord();
        if (curi.isSeed()) {
            r.addLabel("seed");
        } else {
            if (curi.forceFetch()) {
                r.addLabel("force-fetch");
            }
            r.addLabelValue("via", curi.flattenVia());
            r.addLabelValue("hopsFromSeed", curi.getPathFromSeed());
            if (curi.containsKey(A_SOURCE_TAG)) {
                r.addLabelValue("sourceTag", curi.getString(A_SOURCE_TAG));
            }
        }
        long duration = curi.getFetchDuration();
        if (duration > -1) {
            r.addLabelValue("fetchTimeMs", Long.toString(duration));
        }

        if (curi.containsKey(A_FTP_FETCH_STATUS)) {
            r.addLabelValue("ftpFetchStatus", curi.getString(A_FTP_FETCH_STATUS));
        }

        // Add outlinks though they are effectively useless without anchor text.
        Collection<Link> links = curi.getOutLinks();
        if (links != null && links.size() > 0) {
            for (Link link : links) {
                r.addLabelValue("outlink", link.toString());
            }
        }

        // TODO: Other curi fields to write to metadata.
        //
        // Credentials
        //
        // fetch-began-time: 1154569278774
        // fetch-completed-time: 1154569281816
        //
        // Annotations.

        byte[] b = r.getUTF8Bytes();
        w.writeMetadataRecord(curi.toString(), timestamp, ANVLRecord.MIMETYPE, uid, namedFields,
                new ByteArrayInputStream(b), b.length);
        return uid;
    }

    protected URI getRecordID() throws IOException {
        URI result;
        try {
            result = GeneratorFactory.getFactory().getRecordID();
        } catch (URISyntaxException e) {
            throw new IOException(e.toString());
        }
        return result;
    }

    protected URI qualifyRecordID(final URI base, final String key, final String value) throws IOException {
        URI result;
        Map<String, String> qualifiers = new HashMap<String, String>(1);
        qualifiers.put(key, value);
        try {
            result = GeneratorFactory.getFactory().qualifyRecordID(base, qualifiers);
        } catch (URISyntaxException e) {
            throw new IOException(e.toString());
        }
        return result;
    }

    @Override
    protected String getFirstrecordStylesheet() {
        return "/warcinfobody.xsl";
    }

    /**
     * Return relevant values as header-like fields (here ANVLRecord, but spec-defined "application/warc-fields" type
     * when written). Field names from from DCMI Terms and the WARC/0.17 specification.
     *
     * @see org.archive.crawler.framework.WriterPoolProcessor#getFirstrecordBody(java.io.File)
     */
    @Override
    protected String getFirstrecordBody(File orderFile) {
        ANVLRecord record = new ANVLRecord(7);
        record.addLabelValue("software", "Heritrix/" + Heritrix.getVersion() + " http://crawler.archive.org");

        try {
            InetAddress host = InetAddress.getLocalHost();
            record.addLabelValue("ip", host.getHostAddress());
            record.addLabelValue("hostname", host.getCanonicalHostName());
        } catch (UnknownHostException e) {
            logger.log(Level.WARNING, "unable top obtain local crawl engine host", e);
        }

        // conforms to ISO 28500:2009 as of May 2009
        // as described at http://bibnum.bnf.fr/WARC/
        // latest draft as of November 2008
        record.addLabelValue("format", "WARC File Format 1.0");
        record.addLabelValue("conformsTo", "http://bibnum.bnf.fr/WARC/WARC_ISO_28500_version1_latestdraft.pdf");

        // Get other values from order.xml
        try {
            Document doc = XmlUtils.getDocument(orderFile);
            addIfNotBlank(record, "operator", XmlUtils.xpathOrNull(doc, "//meta/operator"));
            addIfNotBlank(record, "publisher", XmlUtils.xpathOrNull(doc, "//meta/organization"));
            addIfNotBlank(record, "audience", XmlUtils.xpathOrNull(doc, "//meta/audience"));
            addIfNotBlank(record, "isPartOf", XmlUtils.xpathOrNull(doc, "//meta/name"));

            // disabling "created" field per HER-1634
            // though it's theoretically useful as a means of distinguishing
            // one crawl from another, the current usage/specification is too
            // vague... in particular a 'created' field in the 'warcinfo' is
            // reasonable to interpret as applying to the WARC-unit, rather
            // than the crawl-job-unit so we remove it and see if anyone
            // complains or makes a case for restoring it in a less-ambiguous
            // manner
            // String rawDate = XmlUtils.xpathOrNull(doc,"//meta/date");
            // if(StringUtils.isNotBlank(rawDate)) {
            // Date date;
            // try {
            // date = ArchiveUtils.parse14DigitDate(rawDate);
            // addIfNotBlank(record,"created",ArchiveUtils.getLog14Date(date));
            // } catch (ParseException e) {
            // logger.log(Level.WARNING,"obtaining warc created date",e);
            // }
            // }

            addIfNotBlank(record, "description", XmlUtils.xpathOrNull(doc, "//meta/description"));
            addIfNotBlank(record, "robots",
                    XmlUtils.xpathOrNull(doc, "//newObject[@name='robots-honoring-policy']/string[@name='type']"));
            addIfNotBlank(record, "http-header-user-agent",
                    XmlUtils.xpathOrNull(doc, "//map[@name='http-headers']/string[@name='user-agent']"));
            addIfNotBlank(record, "http-header-from",
                    XmlUtils.xpathOrNull(doc, "//map[@name='http-headers']/string[@name='from']"));
            if (metadataMap == null) {
                //metadataMap = getMetadataItems();
                XPathFactory factory = XPathFactory.newInstance();
                XPath xpath = factory.newXPath();
                XPathExpression expr = xpath.compile(H1HeritrixTemplate.METADATA_ITEMS_XPATH);
                Node node = (Node) expr.evaluate(doc, XPathConstants.NODE);
                //NodeList nodeList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
                //Node node = nodeList.item(0);
                if (node != null) {
                    NodeList nodeList = node.getChildNodes();
                    if (nodeList != null) {
                        metadataMap = new HashMap();
                    	for (int i=0; i<nodeList.getLength(); ++i) {
                    		node = nodeList.item(i);
                    		if (node.getNodeType() == Node.ELEMENT_NODE) {
                    			String typeName = node.getNodeName();
                    			if ("string".equals(typeName)) {
                        			Node attribute = node.getAttributes().getNamedItem("name");
                        			if (attribute != null && attribute.getNodeType() == Node.ATTRIBUTE_NODE) {
                        				String key = attribute.getNodeValue();
                        				if (key != null && key.length() > 0) {
                        					String value = node.getTextContent();
                        					metadataMap.put(key, value);
                        					// debug
                        					//System.out.println(key + "=" + value);
                        				}
                        			}
                    			}
                    		}
                    	}
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error obtaining warcinfo", e);
        } catch (XPathExpressionException e) {
            logger.log(Level.WARNING, "Error obtaining metadata items", e);
        }
        
        // add fields from harvesInfo.xml version 0.4
        /*
         * <harvestInfo> <version>0.4</version> <jobId>1</jobId> <priority>HIGHPRIORITY</priority>
         * <harvestNum>0</harvestNum> <origHarvestDefinitionID>1</origHarvestDefinitionID>
         * <maxBytesPerDomain>500000000</maxBytesPerDomain> <maxObjectsPerDomain>2000</maxObjectsPerDomain>
         * <orderXMLName>default_orderxml</orderXMLName>
         * <origHarvestDefinitionName>netarkivet</origHarvestDefinitionName> <scheduleName>Once_a_week</scheduleName>
         * <harvestFilenamePrefix>1-1</harvestFilenamePrefix> <jobSubmitDate>Some date</jobSubmitDate>
         * <performer>undefined</performer> </harvestInfo>
         */
        String netarchiveSuiteComment = "#added by NetarchiveSuite "
                + dk.netarkivet.common.Constants.getVersionString();
        ANVLRecord recordNAS = new ANVLRecord(7);

        if (metadataMap != null) {
            // Add the data from the metadataMap to the WarcInfoRecord.
            recordNAS.addLabelValue(HARVESTINFO_VERSION, (String) metadataMap.get(HARVESTINFO_VERSION));
            recordNAS.addLabelValue(HARVESTINFO_JOBID, (String) metadataMap.get(HARVESTINFO_JOBID));
            recordNAS.addLabelValue(HARVESTINFO_CHANNEL, (String) metadataMap.get(HARVESTINFO_CHANNEL));
            recordNAS.addLabelValue(HARVESTINFO_HARVESTNUM, (String) metadataMap.get(HARVESTINFO_HARVESTNUM));
            recordNAS.addLabelValue(HARVESTINFO_ORIGHARVESTDEFINITIONID,  (String) metadataMap.get(HARVESTINFO_ORIGHARVESTDEFINITIONID));
            recordNAS.addLabelValue(HARVESTINFO_MAXBYTESPERDOMAIN, (String) metadataMap.get(HARVESTINFO_MAXBYTESPERDOMAIN));

            recordNAS.addLabelValue(HARVESTINFO_MAXOBJECTSPERDOMAIN, (String) metadataMap.get(HARVESTINFO_MAXOBJECTSPERDOMAIN));
            recordNAS.addLabelValue(HARVESTINFO_ORDERXMLNAME, (String) metadataMap.get(HARVESTINFO_ORDERXMLNAME));
            recordNAS.addLabelValue(HARVESTINFO_ORIGHARVESTDEFINITIONNAME, (String) metadataMap.get(HARVESTINFO_ORIGHARVESTDEFINITIONNAME));

            if (metadataMap.containsKey((HARVESTINFO_SCHEDULENAME))) {
                recordNAS.addLabelValue(HARVESTINFO_SCHEDULENAME, (String) metadataMap.get(HARVESTINFO_SCHEDULENAME));
            }
            recordNAS.addLabelValue(HARVESTINFO_HARVESTFILENAMEPREFIX, (String) metadataMap.get(HARVESTINFO_HARVESTFILENAMEPREFIX));
     
            recordNAS.addLabelValue(HARVESTINFO_JOBSUBMITDATE, (String) metadataMap.get(HARVESTINFO_JOBSUBMITDATE));
    	
            if (metadataMap.containsKey(HARVESTINFO_PERFORMER)) {
    		    recordNAS.addLabelValue(HARVESTINFO_PERFORMER, (String) metadataMap.get(HARVESTINFO_PERFORMER));
            }

            if (metadataMap.containsKey(HARVESTINFO_AUDIENCE)) { 
                recordNAS.addLabelValue(HARVESTINFO_AUDIENCE, (String) metadataMap.get(HARVESTINFO_AUDIENCE));
            }
        } else {
			logger.log(Level.SEVERE, "Error missing metadata");
        }

        // really ugly to return as string, when it may just be merged with
        // a couple other fields at write time, but changing would require
        // larger refactoring
        return record.toString() + netarchiveSuiteComment + "\n" + recordNAS.toString();
    }

    protected void addIfNotBlank(ANVLRecord record, String label, String value) {
        if (StringUtils.isNotBlank(value)) {
            record.addLabelValue(label, value);
        }
    }

}
