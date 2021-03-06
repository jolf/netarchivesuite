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

package dk.netarkivet.harvester.harvesting.distribute;

import java.io.Serializable;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.distribute.HarvesterChannels;
import dk.netarkivet.harvester.distribute.HarvesterMessage;
import dk.netarkivet.harvester.distribute.HarvesterMessageVisitor;
import dk.netarkivet.harvester.harvesting.report.HarvestReport;

/**
 * Instances of this class are sent by a HarvestControllerServer to the THE_SCHED queue to indicate the progress of a
 * heritrix crawl. They are collected and processed by the HarvestSchedulerMonitorServer.
 * <p>
 * This class is immutable
 */
@SuppressWarnings({"serial"})
public class CrawlStatusMessage extends HarvesterMessage implements Serializable {

    /** the id for the crawlJob, for which this message reports. */
    private long jobID;
    /** The current state of the crawl-job. */
    private JobStatus statusCode;
    /** A harvestReport created at the end of the crawl. */
    private HarvestReport harvestReport;
    /** harvest errors encountered. */
    private String harvestErrors;
    /** harvest errors encountered with details. */
    private String harvestErrorDetails;
    /** upload errors encountered. */
    private String uploadErrors;
    /** upload errors encountered with details. */
    private String uploadErrorDetails;

    /**
     * Creates an instance of this class corresponding to a job.
     *
     * @param jobID the unique identifier for the crawl job to which this message refers
     * @param statusCode All values are accepted, except null
     * @param harvestReport A calculated domain harvest report produced by the crawl. May be null for no domain harvest
     * report.
     * @throws ArgumentNotValid If invalid arguments: jobID < 0L statusCode == null
     */
    public CrawlStatusMessage(long jobID, JobStatus statusCode, HarvestReport harvestReport) {
        super(HarvesterChannels.getTheSched(), Channels.getError());
        ArgumentNotValid.checkNotNegative(jobID, "jobID");
        ArgumentNotValid.checkNotNull(statusCode, "statusCode");
        this.jobID = jobID;
        this.statusCode = statusCode;
        this.harvestReport = harvestReport;
    }

    /**
     * Alternate constructor, which does not have the DomainHarvestreport as argument.
     *
     * @param jobID (see description for the other constructor)
     * @param statusCode (see description for the other constructor)
     * @see CrawlStatusMessage#CrawlStatusMessage(long, JobStatus, HarvestReport)
     */
    public CrawlStatusMessage(long jobID, JobStatus statusCode) {
        this(jobID, statusCode, null);
    }

    /**
     * Returns the jobID of this crawl job.
     *
     * @return the jobID
     */
    public long getJobID() {
        return jobID;
    }

    /**
     * Returns the status code of this crawl job.
     *
     * @return the status code
     */
    public JobStatus getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the generated domain harvest report object by this crawl job. May be null on non-finished status, or job
     * finished with no log file
     *
     * @return the hosts report.
     */
    public HarvestReport getDomainHarvestReport() {
        return harvestReport;
    }

    /**
     * Should be implemented as a part of the visitor pattern. fx.: public void accept(HarvesterMessageVisitor v) {
     * v.visit(this); }
     *
     * @param v A message visitor
     */
    public void accept(HarvesterMessageVisitor v) {
        v.visit(this);
    }

    /**
     * Human readable version of object.
     *
     * @return Human readable version of object.
     */
    public String toString() {
        String dhr = "";
        if (harvestReport != null) {
            dhr = harvestReport.toString();
        }

        return "CrawlStatusMessage:\n" + "JobID: " + jobID + '\n' + "StatusCode: " + statusCode + '\n' + dhr + '\n'
                + super.toString();
    }

    /**
     * Get-method for private field harvestErrors.
     *
     * @return harvestErrors
     */
    public String getHarvestErrors() {
        return harvestErrors;
    }

    /**
     * Set-method for private field harvestErrors.
     *
     * @param harvestErrors The value for harvest errors.
     * @throws ArgumentNotValid if null argument
     */
    public void setHarvestErrors(String harvestErrors) {
        ArgumentNotValid.checkNotNull(harvestErrors, "String harvestErrors");
        this.harvestErrors = harvestErrors;
    }

    /**
     * Get-method for private field harvestErrorDetails.
     *
     * @return harvestErrorDetails
     */
    public String getHarvestErrorDetails() {
        return harvestErrorDetails;
    }

    /**
     * Set-method for private field harvestErrorDetails.
     *
     * @param harvestErrorDetails The value for harvest error details.
     * @throws ArgumentNotValid if null argument
     */
    public void setHarvestErrorDetails(String harvestErrorDetails) {
        ArgumentNotValid.checkNotNull(harvestErrorDetails, "String harvestErrorDetails");
        this.harvestErrorDetails = harvestErrorDetails;
    }

    /**
     * Get-method for private field uploadErrors.
     *
     * @return uploadErrors
     */
    public String getUploadErrors() {
        return uploadErrors;
    }

    /**
     * Set-method for private field uploadErrors.
     *
     * @param uploadErrors The value for upload errors.
     * @throws ArgumentNotValid if null argument
     */
    public void setUploadErrors(String uploadErrors) {
        ArgumentNotValid.checkNotNull(uploadErrors, "String uploadErrors");
        this.uploadErrors = uploadErrors;
    }

    /**
     * Get-method for private field uploadErrorDetails.
     *
     * @return uploadErrorDetails
     */
    public String getUploadErrorDetails() {
        return uploadErrorDetails;
    }

    /**
     * Set-method for private field uploadErrorDetails.
     *
     * @param uploadErrorDetails The value for upload error details.
     * @throws ArgumentNotValid if null argument
     */
    public void setUploadErrorDetails(String uploadErrorDetails) {
        ArgumentNotValid.checkNotNull(uploadErrorDetails, "String uploadErrorDetails");
        this.uploadErrorDetails = uploadErrorDetails;
    }

}
