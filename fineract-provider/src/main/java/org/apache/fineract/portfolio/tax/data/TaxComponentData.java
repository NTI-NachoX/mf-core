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
package org.apache.fineract.portfolio.tax.data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.fineract.accounting.glaccount.data.GLAccountData;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;

public final class TaxComponentData implements Serializable {

    @SuppressWarnings("unused")
    private final Long id;
    @SuppressWarnings("unused")
    private final String name;
    @SuppressWarnings("unused")
    private final BigDecimal percentage;
    @SuppressWarnings("unused")
    private final EnumOptionData debitAccountType;
    @SuppressWarnings("unused")
    private final GLAccountData debitAccount;
    @SuppressWarnings("unused")
    private final EnumOptionData creditAccountType;
    @SuppressWarnings("unused")
    private final GLAccountData creditAccount;
    @SuppressWarnings("unused")
    private final LocalDate startDate;
    @SuppressWarnings("unused")
    private final Collection<TaxComponentHistoryData> taxComponentHistories;

    // template options
    @SuppressWarnings("unused")
    private final Map<String, List<GLAccountData>> glAccountOptions;
    @SuppressWarnings("unused")
    private final Collection<EnumOptionData> glAccountTypeOptions;

    public static TaxComponentData instance(final Long id, final String name, final BigDecimal percentage,
            final EnumOptionData debitAccountType, final GLAccountData debitAcount, final EnumOptionData creditAccountType,
            final GLAccountData creditAcount, final LocalDate startDate, final Collection<TaxComponentHistoryData> taxComponentHistories) {
        final Map<String, List<GLAccountData>> glAccountOptions = null;
        final Collection<EnumOptionData> glAccountTypeOptions = null;
        return new TaxComponentData(id, name, percentage, debitAccountType, debitAcount, creditAccountType, creditAcount, startDate,
                taxComponentHistories, glAccountOptions, glAccountTypeOptions);
    }

    public static TaxComponentData lookup(final Long id, final String name) {
        final BigDecimal percentage = null;
        final EnumOptionData debitAccountType = null;
        final GLAccountData debitAcount = null;
        final EnumOptionData creditAccountType = null;
        final GLAccountData creditAcount = null;
        final LocalDate startDate = null;
        final Collection<TaxComponentHistoryData> taxComponentHistories = null;
        final Map<String, List<GLAccountData>> glAccountOptions = null;
        final Collection<EnumOptionData> glAccountTypeOptions = null;
        return new TaxComponentData(id, name, percentage, debitAccountType, debitAcount, creditAccountType, creditAcount, startDate,
                taxComponentHistories, glAccountOptions, glAccountTypeOptions);
    }

    public static TaxComponentData template(final Map<String, List<GLAccountData>> glAccountOptions,
            final Collection<EnumOptionData> glAccountTypeOptions) {
        final Long id = null;
        final String name = null;
        final BigDecimal percentage = null;
        final EnumOptionData debitAccountType = null;
        final GLAccountData debitAcount = null;
        final EnumOptionData creditAccountType = null;
        final GLAccountData creditAcount = null;
        final LocalDate startDate = null;
        final Collection<TaxComponentHistoryData> taxComponentHistories = null;
        return new TaxComponentData(id, name, percentage, debitAccountType, debitAcount, creditAccountType, creditAcount, startDate,
                taxComponentHistories, glAccountOptions, glAccountTypeOptions);
    }

    private TaxComponentData(final Long id, final String name, final BigDecimal percentage, final EnumOptionData debitAccountType,
            final GLAccountData debitAcount, final EnumOptionData creditAccountType, final GLAccountData creditAcount,
            final LocalDate startDate, final Collection<TaxComponentHistoryData> taxComponentHistories,
            final Map<String, List<GLAccountData>> glAccountOptions, final Collection<EnumOptionData> glAccountTypeOptions) {
        this.id = id;
        this.percentage = percentage;
        this.name = name;
        this.debitAccountType = debitAccountType;
        this.debitAccount = debitAcount;
        this.creditAccountType = creditAccountType;
        this.creditAccount = creditAcount;
        this.startDate = startDate;
        this.taxComponentHistories = taxComponentHistories;
        this.glAccountOptions = glAccountOptions;
        this.glAccountTypeOptions = glAccountTypeOptions;
    }

    private TaxComponentData(final Long id, final BigDecimal percentage, final GLAccountData debitAcount,
            final GLAccountData creditAcount) {
        this.id = id;
        this.percentage = percentage;
        this.name = null;
        this.debitAccountType = null;
        this.debitAccount = debitAcount;
        this.creditAccountType = null;
        this.creditAccount = creditAcount;
        this.startDate = null;
        this.taxComponentHistories = null;
        this.glAccountOptions = null;
        this.glAccountTypeOptions = null;
    }

    public static TaxComponentData createTaxComponent(final Long id, final BigDecimal percentage, final GLAccountData debitAccount,
            final GLAccountData creditAccount) {
        return new TaxComponentData(id, percentage, debitAccount, creditAccount);
    }

    public BigDecimal getApplicablePercentage(final LocalDate date) {
        BigDecimal percentage = null;
        if (occursOnDayFrom(date)) {
            percentage = getPercentage();
        } else {
            for (TaxComponentHistoryData componentHistory : this.taxComponentHistories) {
                if (componentHistory.occursOnDayFromAndUpToAndIncluding(date)) {
                    percentage = componentHistory.getPercentage();
                    break;
                }
            }
        }
        return percentage;
    }

    public BigDecimal getPercentage() {
        return this.percentage;
    }

    private boolean occursOnDayFrom(final LocalDate target) {
        return target != null && target.isAfter(startDate());
    }

    public LocalDate startDate() {
        LocalDate startDate = null;
        if (this.startDate != null) {
            startDate = this.startDate;
            // startDate = LocalDate.ofInstant(this.startDate.toInstant(), DateUtils.getDateTimeZoneOfTenant());
        }
        return startDate;
    }

    public GLAccountData getCreditAcount() {
        return this.creditAccount;
    }

}
