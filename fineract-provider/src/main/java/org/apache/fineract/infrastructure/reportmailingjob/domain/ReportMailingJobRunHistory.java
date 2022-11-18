/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.infrastructure.reportmailingjob.domain;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

@Entity
@Table(name = "m_report_mailing_job_run_history")
public class ReportMailingJobRunHistory extends AbstractPersistableCustom {

    private static final long serialVersionUID = -3757370929988421076L;

    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    private ReportMailingJob reportMailingJob;

    @Column(name = "start_datetime", nullable = false)
    private LocalDateTime startDateTime;

    @Column(name = "end_datetime", nullable = false)
    private LocalDateTime endDateTime;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "error_message", nullable = false)
    private String errorMessage;

    @Column(name = "error_log", nullable = false)
    private String errorLog;

    /**
     * ReportMailingJobRunHistory protected constructor
     **/
    protected ReportMailingJobRunHistory() {}

    /**
     * ReportMailingJobRunHistory private constructor
     **/
    private ReportMailingJobRunHistory(final ReportMailingJob reportMailingJob, final LocalDateTime startDateTime,
            final LocalDateTime endDateTime, final String status, final String errorMessage, final String errorLog) {
        this.reportMailingJob = reportMailingJob;
        this.startDateTime = null;

        if (startDateTime != null) {
            this.startDateTime = startDateTime;
        }

        this.endDateTime = null;

        if (endDateTime != null) {
            this.endDateTime = endDateTime;
        }

        this.status = status;
        this.errorMessage = errorMessage;
        this.errorLog = errorLog;
    }

    /**
     * Creates an instance of the ReportMailingJobRunHistory class
     *
     * @return ReportMailingJobRunHistory object
     **/
    public static ReportMailingJobRunHistory newInstance(final ReportMailingJob reportMailingJob, final LocalDateTime startDateTime,
            final LocalDateTime endDateTime, final String status, final String errorMessage, final String errorLog) {
        return new ReportMailingJobRunHistory(reportMailingJob, startDateTime, endDateTime, status, errorMessage, errorLog);
    }

    /**
     * @return the reportMailingJobId
     */
    public ReportMailingJob getReportMailingJob() {
        return this.reportMailingJob;
    }

    /**
     * @return the startDateTime
     */
    public LocalDateTime getStartDateTime() {
        return this.startDateTime;
    }

    /**
     * @return the endDateTime
     */
    public LocalDateTime getEndDateTime() {
        return this.endDateTime;
    }

    /**
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @return the errorMessage
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @return the errorLog
     */
    public String getErrorLog() {
        return errorLog;
    }
}
