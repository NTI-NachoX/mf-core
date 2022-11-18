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
package org.apache.fineract.portfolio.floatingrates.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.floatingrates.data.FloatingRateDTO;
import org.apache.fineract.portfolio.floatingrates.data.FloatingRatePeriodData;
import org.apache.fineract.useradministration.domain.AppUser;

//TODO: refactor to use AbstractAuditableCustom!
@Entity
@Table(name = "m_floating_rates_periods")
public class FloatingRatePeriod extends AbstractPersistableCustom {

    @ManyToOne
    @JoinColumn(name = "floating_rates_id", nullable = false)
    private FloatingRate floatingRate;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "interest_rate", scale = 6, precision = 19, nullable = false)
    private BigDecimal interestRate;

    @Column(name = "is_differential_to_base_lending_rate", nullable = false)
    private boolean isDifferentialToBaseLendingRate;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "createdby_id", nullable = false)
    private AppUser createdBy;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "lastmodifiedby_id", nullable = false)
    private AppUser modifiedBy;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdOn;

    @Column(name = "lastmodified_date", nullable = false)
    private LocalDateTime modifiedOn;

    public FloatingRatePeriod() {

    }

    public FloatingRatePeriod(LocalDate fromDate, BigDecimal interestRate, boolean isDifferentialToBaseLendingRate, boolean isActive,
            AppUser createdBy, AppUser modifiedBy, LocalDateTime createdOn, LocalDateTime modifiedOn) {
        this.fromDate = fromDate;
        this.interestRate = interestRate;
        this.isDifferentialToBaseLendingRate = isDifferentialToBaseLendingRate;
        this.isActive = isActive;
        this.createdBy = createdBy;
        this.modifiedBy = modifiedBy;
        this.createdOn = createdOn;
        this.modifiedOn = modifiedOn;
    }

    public void updateFloatingRate(FloatingRate floatingRate) {
        this.floatingRate = floatingRate;
    }

    public FloatingRate getFloatingRatesId() {
        return this.floatingRate;
    }

    public LocalDate getFromDate() {
        return this.fromDate;
    }

    public BigDecimal getInterestRate() {
        return this.interestRate;
    }

    public boolean isDifferentialToBaseLendingRate() {
        return this.isDifferentialToBaseLendingRate;
    }

    public boolean isActive() {
        return this.isActive;
    }

    public AppUser getCreatedBy() {
        return this.createdBy;
    }

    public AppUser getModifiedBy() {
        return this.modifiedBy;
    }

    public LocalDateTime getCreatedOn() {
        return this.createdOn;
    }

    public LocalDateTime getModifiedOn() {
        return this.modifiedOn;
    }

    public void setModifiedBy(AppUser modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public void setModifiedOn(LocalDateTime modifiedOn) {
        this.modifiedOn = modifiedOn;
    }

    public void setActive(boolean b) {
        this.isActive = b;
    }

    public LocalDate fetchFromDate() {
        return fromDate;
    }

    public FloatingRatePeriodData toData(final FloatingRateDTO floatingRateDTO) {

        BigDecimal interest = getInterestRate().add(floatingRateDTO.getInterestRateDiff());
        if (isDifferentialToBaseLendingRate()) {
            interest = interest.add(floatingRateDTO.fetchBaseRate(fetchFromDate()));
        }

        final LocalDate fromDate = getFromDate();
        final LocalDateTime createdOn = getCreatedOn();
        final LocalDateTime modifiedOn = getModifiedOn();

        String createdBy = getCreatedBy() != null ? getCreatedBy().getUsername() : null;
        String modifiedBy = getModifiedBy() != null ? getModifiedBy().getUsername() : null;
        return new FloatingRatePeriodData(getId(), fromDate, interest, isDifferentialToBaseLendingRate(), isActive(), createdBy, createdOn,
                modifiedBy, modifiedOn);
    }

}
