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
package org.apache.fineract.scheduledjobs.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.fineract.accounting.glaccount.domain.TrialBalance;
import org.apache.fineract.accounting.glaccount.domain.TrialBalanceRepositoryWrapper;
import org.apache.fineract.infrastructure.core.config.FineractProperties;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.RoutingDataSourceServiceFactory;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.core.service.database.DatabaseTypeResolver;
import org.apache.fineract.infrastructure.jobs.annotation.CronTarget;
import org.apache.fineract.infrastructure.jobs.domain.ScheduledJobDetail;
import org.apache.fineract.infrastructure.jobs.domain.ScheduledJobDetailRepository;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.infrastructure.jobs.service.JobRegisterService;
import org.apache.fineract.portfolio.savings.DepositAccountType;
import org.apache.fineract.portfolio.savings.DepositAccountUtils;
import org.apache.fineract.portfolio.savings.data.DepositAccountData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountAnnualFeeData;
import org.apache.fineract.portfolio.savings.service.DepositAccountReadPlatformService;
import org.apache.fineract.portfolio.savings.service.DepositAccountWritePlatformService;
import org.apache.fineract.portfolio.savings.service.SavingsAccountChargeReadPlatformService;
import org.apache.fineract.portfolio.savings.service.SavingsAccountWritePlatformService;
import org.apache.fineract.portfolio.shareaccounts.service.ShareAccountDividendReadPlatformService;
import org.apache.fineract.portfolio.shareaccounts.service.ShareAccountSchedularService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service(value = "scheduledJobRunnerService")
public class ScheduledJobRunnerServiceImpl implements ScheduledJobRunnerService {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledJobRunnerServiceImpl.class);

    private final RoutingDataSourceServiceFactory dataSourceServiceFactory;
    private final SavingsAccountWritePlatformService savingsAccountWritePlatformService;
    private final SavingsAccountChargeReadPlatformService savingsAccountChargeReadPlatformService;
    private final DepositAccountReadPlatformService depositAccountReadPlatformService;
    private final DepositAccountWritePlatformService depositAccountWritePlatformService;
    private final ShareAccountDividendReadPlatformService shareAccountDividendReadPlatformService;
    private final ShareAccountSchedularService shareAccountSchedularService;
    private final TrialBalanceRepositoryWrapper trialBalanceRepositoryWrapper;
    private final JobRegisterService jobRegisterService;
    private final ScheduledJobDetailRepository scheduledJobDetailsRepository;
    private final FineractProperties fineractProperties;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final DatabaseTypeResolver databaseTypeResolver;

    @Autowired
    public ScheduledJobRunnerServiceImpl(final RoutingDataSourceServiceFactory dataSourceServiceFactory,
            final SavingsAccountWritePlatformService savingsAccountWritePlatformService,
            final SavingsAccountChargeReadPlatformService savingsAccountChargeReadPlatformService,
            final DepositAccountReadPlatformService depositAccountReadPlatformService,
            final DepositAccountWritePlatformService depositAccountWritePlatformService,
            final ShareAccountDividendReadPlatformService shareAccountDividendReadPlatformService,
            final ShareAccountSchedularService shareAccountSchedularService,
            final TrialBalanceRepositoryWrapper trialBalanceRepositoryWrapper, @Lazy final JobRegisterService jobRegisterService,
            final ScheduledJobDetailRepository scheduledJobDetailsRepository, final FineractProperties fineractProperties,
            DatabaseSpecificSQLGenerator sqlGenerator, DatabaseTypeResolver databaseTypeResolver) {
        this.dataSourceServiceFactory = dataSourceServiceFactory;
        this.savingsAccountWritePlatformService = savingsAccountWritePlatformService;
        this.savingsAccountChargeReadPlatformService = savingsAccountChargeReadPlatformService;
        this.depositAccountReadPlatformService = depositAccountReadPlatformService;
        this.depositAccountWritePlatformService = depositAccountWritePlatformService;
        this.shareAccountDividendReadPlatformService = shareAccountDividendReadPlatformService;
        this.shareAccountSchedularService = shareAccountSchedularService;
        this.trialBalanceRepositoryWrapper = trialBalanceRepositoryWrapper;
        this.jobRegisterService = jobRegisterService;
        this.scheduledJobDetailsRepository = scheduledJobDetailsRepository;
        this.fineractProperties = fineractProperties;
        this.sqlGenerator = sqlGenerator;
        this.databaseTypeResolver = databaseTypeResolver;
    }

    @Override
    @CronTarget(jobName = JobName.APPLY_ANNUAL_FEE_FOR_SAVINGS)
    public void applyAnnualFeeForSavings() {

        final Collection<SavingsAccountAnnualFeeData> annualFeeData = this.savingsAccountChargeReadPlatformService
                .retrieveChargesWithAnnualFeeDue();

        for (final SavingsAccountAnnualFeeData savingsAccountReference : annualFeeData) {
            try {
                this.savingsAccountWritePlatformService.applyAnnualFee(savingsAccountReference.getId(),
                        savingsAccountReference.getAccountId());
            } catch (final PlatformApiDataValidationException e) {
                final List<ApiParameterError> errors = e.getErrors();
                for (final ApiParameterError error : errors) {
                    LOG.error("Apply annual fee failed for account: {} with message {}", savingsAccountReference.getAccountNo(), error);
                }
            } catch (final Exception ex) {
                LOG.error("Apply annual fee failed for account: {}", savingsAccountReference.getAccountNo(), ex);
            }
        }

        LOG.info("{}: Records affected by applyAnnualFeeForSavings: {}", ThreadLocalContextUtil.getTenant().getName(),
                annualFeeData.size());
    }

    @Override
    @CronTarget(jobName = JobName.PAY_DUE_SAVINGS_CHARGES)
    public void applyDueChargesForSavings() throws JobExecutionException {
        final Collection<SavingsAccountAnnualFeeData> chargesDueData = this.savingsAccountChargeReadPlatformService
                .retrieveChargesWithDue();
        List<Throwable> exceptions = new ArrayList<>();
        for (final SavingsAccountAnnualFeeData savingsAccountReference : chargesDueData) {
            try {
                this.savingsAccountWritePlatformService.applyChargeDue(savingsAccountReference.getId(),
                        savingsAccountReference.getAccountId());
            } catch (final PlatformApiDataValidationException e) {
                exceptions.add(e);
                final List<ApiParameterError> errors = e.getErrors();
                for (final ApiParameterError error : errors) {
                    LOG.error("Apply Charges due for savings failed for account {} with message: {}",
                            savingsAccountReference.getAccountNo(), error.getDeveloperMessage(), e);
                }
            } catch (final Exception ex) {
                exceptions.add(ex);
                LOG.error("Apply Charges due for savings failed for account: {}", savingsAccountReference.getAccountNo(), ex);
            }
        }
        LOG.info("{}: Records affected by applyDueChargesForSavings: {}", ThreadLocalContextUtil.getTenant().getName(),
                chargesDueData.size());
        if (!exceptions.isEmpty()) {
            throw new JobExecutionException(exceptions);
        }
    }

    @Transactional
    @Override
    @CronTarget(jobName = JobName.UPDATE_NPA)
    public void updateNPA() {

        final JdbcTemplate jdbcTemplate = new JdbcTemplate(this.dataSourceServiceFactory.determineDataSourceService().retrieveDataSource());

        final StringBuilder resetNPASqlBuilder = new StringBuilder();
        resetNPASqlBuilder.append("update m_loan loan ");
        String fromPart = " (SELECT loan2.* FROM m_loan loan2 left join m_loan_arrears_aging laa on laa.loan_id = loan2.id "
                + "inner join m_product_loan mpl on mpl.id = loan2.product_id and mpl.overdue_days_for_npa is not null "
                + "WHERE loan2.loan_status_id = 300 and mpl.account_moves_out_of_npa_only_on_arrears_completion = false"
                + " or (mpl.account_moves_out_of_npa_only_on_arrears_completion = true"
                + " and laa.overdue_since_date_derived is null)) sl";
        String wherePart = " where loan.id = sl.id ";

        if (databaseTypeResolver.isMySQL()) {
            resetNPASqlBuilder.append(", ").append(fromPart).append(" set loan.is_npa = false").append(wherePart);
        } else {
            resetNPASqlBuilder.append("set is_npa = false").append(" FROM ").append(fromPart).append(wherePart);
        }
        jdbcTemplate.update(resetNPASqlBuilder.toString());

        final StringBuilder updateSqlBuilder = new StringBuilder(900);

        fromPart = " (select loan.id " + " FROM m_loan_arrears_aging laa" + " INNER JOIN  m_loan loan on laa.loan_id = loan.id "
                + " INNER JOIN m_product_loan mpl on mpl.id = loan.product_id AND mpl.overdue_days_for_npa is not null "
                + "WHERE loan.loan_status_id = 300 and " + "laa.overdue_since_date_derived < "
                + sqlGenerator.subDate(sqlGenerator.currentBusinessDate(), "COALESCE(mpl.overdue_days_for_npa, 0)", "day")
                + " group by loan.id) as sl ";
        wherePart = " where ml.id=sl.id ";
        updateSqlBuilder.append("UPDATE m_loan as ml ");
        if (databaseTypeResolver.isMySQL()) {
            updateSqlBuilder.append(", ").append(fromPart).append(" SET ml.is_npa = true").append(wherePart);
        } else {
            updateSqlBuilder.append(" SET is_npa = true").append(" FROM ").append(fromPart).append(wherePart);
        }

        final int result = jdbcTemplate.update(updateSqlBuilder.toString());

        LOG.info("{}: Records affected by updateNPA: {}", ThreadLocalContextUtil.getTenant().getName(), result);
    }

    @Override
    @CronTarget(jobName = JobName.UPDATE_DEPOSITS_ACCOUNT_MATURITY_DETAILS)
    public void updateMaturityDetailsOfDepositAccounts() {

        final Collection<DepositAccountData> depositAccounts = this.depositAccountReadPlatformService.retrieveForMaturityUpdate();

        for (final DepositAccountData depositAccount : depositAccounts) {
            try {
                final DepositAccountType depositAccountType = DepositAccountType.fromInt(depositAccount.depositType().getId().intValue());
                this.depositAccountWritePlatformService.updateMaturityDetails(depositAccount.id(), depositAccountType);
            } catch (final PlatformApiDataValidationException e) {
                final List<ApiParameterError> errors = e.getErrors();
                for (final ApiParameterError error : errors) {
                    LOG.error("Update maturity details failed for account: {} with message {}", depositAccount.accountNo(),
                            error.getDeveloperMessage());
                }
            } catch (final Exception ex) {
                LOG.error("Update maturity details failed for account: {}", depositAccount.accountNo(), ex);
            }
        }

        LOG.info("{}: Records affected by updateMaturityDetailsOfDepositAccounts: {}", ThreadLocalContextUtil.getTenant().getName(),
                depositAccounts.size());
    }

    @Override
    @CronTarget(jobName = JobName.GENERATE_RD_SCEHDULE)
    public void generateRDSchedule() {
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(this.dataSourceServiceFactory.determineDataSourceService().retrieveDataSource());
        final Collection<Map<String, Object>> scheduleDetails = this.depositAccountReadPlatformService.retriveDataForRDScheduleCreation();
        String insertSql = "INSERT INTO m_mandatory_savings_schedule (savings_account_id, duedate, installment, deposit_amount, completed_derived, created_date, lastmodified_date) VALUES ";
        StringBuilder sb = new StringBuilder();
        String currentDate = DateUtils.getLocalDateTimeOfTenant().format(DateUtils.DEFAULT_DATETIME_FORMATER);
        int iterations = 0;
        for (Map<String, Object> details : scheduleDetails) {
            Long count = (Long) details.get("futureInstallemts");
            if (count == null) {
                count = 0L;
            }
            final Long savingsId = (Long) details.get("savingsId");
            final BigDecimal amount = (BigDecimal) details.get("amount");
            final String recurrence = (String) details.get("recurrence");
            LocalDate lastDepositDate = (LocalDate) details.get("dueDate");
            Integer installmentNumber = (Integer) details.get("installment");
            while (count < DepositAccountUtils.GENERATE_MINIMUM_NUMBER_OF_FUTURE_INSTALMENTS) {
                count++;
                installmentNumber++;
                lastDepositDate = DepositAccountUtils.calculateNextDepositDate(lastDepositDate, recurrence);

                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append("(");
                sb.append(savingsId);
                sb.append(",'");
                sb.append(lastDepositDate.format(DateUtils.DEFAULT_DATE_FORMATER));
                sb.append("',");
                sb.append(installmentNumber);
                sb.append(",");
                sb.append(amount);
                sb.append(", b'0','");
                sb.append(currentDate);
                sb.append("','");
                sb.append(currentDate);
                sb.append("')");
                iterations++;
                if (iterations > 200) {
                    jdbcTemplate.update(insertSql + sb); // NOSONAR
                    sb = new StringBuilder();
                }

            }
        }

        if (sb.length() > 0) {
            jdbcTemplate.update(insertSql + sb); // NOSONAR
        }

    }

    @Override
    @CronTarget(jobName = JobName.POST_DIVIDENTS_FOR_SHARES)
    public void postDividends() throws JobExecutionException {
        List<Throwable> exceptions = new ArrayList<>();
        List<Map<String, Object>> dividendDetails = this.shareAccountDividendReadPlatformService.retriveDividendDetailsForPostDividents();
        for (Map<String, Object> dividendMap : dividendDetails) {
            Long id = null;
            Long savingsId = null;
            if (dividendMap.get("id") instanceof BigInteger) {
                // Drizzle is returningBigInteger
                id = ((BigInteger) dividendMap.get("id")).longValue();
                savingsId = ((BigInteger) dividendMap.get("savingsAccountId")).longValue();
            } else { // MySQL connector is returning Long
                id = (Long) dividendMap.get("id");
                savingsId = (Long) dividendMap.get("savingsAccountId");
            }
            try {
                this.shareAccountSchedularService.postDividend(id, savingsId);
            } catch (final PlatformApiDataValidationException e) {
                exceptions.add(e);
                final List<ApiParameterError> errors = e.getErrors();
                for (final ApiParameterError error : errors) {
                    LOG.error(
                            "Post Dividends to savings failed due to ApiParameterError for Divident detail Id: {} and savings Id: {} with message: {}",
                            id, savingsId, error.getDeveloperMessage(), e);
                }
            } catch (final Exception e) {
                LOG.error("Post Dividends to savings failed for Divident detail Id: {} and savings Id: {}", id, savingsId, e);
                exceptions.add(e);
            }
        }

        if (!exceptions.isEmpty()) {
            throw new JobExecutionException(exceptions);
        }
    }

    @Override
    @CronTarget(jobName = JobName.UPDATE_TRIAL_BALANCE_DETAILS)
    public void updateTrialBalanceDetails() throws JobExecutionException {
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(this.dataSourceServiceFactory.determineDataSourceService().retrieveDataSource());
        final StringBuilder tbGapSqlBuilder = new StringBuilder(500);
        tbGapSqlBuilder.append("select distinct(je.transaction_date) ").append("from acc_gl_journal_entry je ")
                .append("where je.transaction_date > (select coalesce(MAX(created_date),'2010-01-01') from m_trial_balance)");

        final List<LocalDate> tbGaps = jdbcTemplate.queryForList(tbGapSqlBuilder.toString(), LocalDate.class);

        for (LocalDate tbGap : tbGaps) {
            int days = Math.toIntExact(ChronoUnit.DAYS.between(tbGap, DateUtils.getBusinessLocalDate()));
            if (days < 1) {
                continue;
            }
            final StringBuilder sqlBuilder = new StringBuilder(600);
            sqlBuilder.append("Insert Into m_trial_balance(office_id, account_id, Amount, entry_date, created_date,closing_balance) ")
                    .append("Select je.office_id, je.account_id, SUM(CASE WHEN je.type_enum=1 THEN (-1) * je.amount ELSE je.amount END) ")
                    .append("as Amount, Date(je.entry_date) as Entry_Date, je.transaction_date as Created_Date,sum(je.amount) as closing_balance ")
                    .append("from acc_gl_journal_entry je WHERE je.transaction_date = ? ")
                    .append("group by je.account_id, je.office_id, je.transaction_date, Date(je.entry_date)");

            final int result = jdbcTemplate.update(sqlBuilder.toString(), tbGap);
            LOG.info("{}: Records affected by updateTrialBalanceDetails: {}", ThreadLocalContextUtil.getTenant().getName(), result);
        }

        // Updating closing balance
        String distinctOfficeQuery = "select distinct(office_id) from m_trial_balance where closing_balance is null group by office_id";
        final List<Long> officeIds = jdbcTemplate.queryForList(distinctOfficeQuery, Long.class);

        for (Long officeId : officeIds) {
            String distinctAccountQuery = "select distinct(account_id) from m_trial_balance where office_id=? and closing_balance is null group by account_id";
            final List<Long> accountIds = jdbcTemplate.queryForList(distinctAccountQuery, Long.class, officeId);
            for (Long accountId : accountIds) {
                final String closingBalanceQuery = "select closing_balance from m_trial_balance where office_id=? and account_id=? and closing_balance "
                        + "is not null order by created_date desc, entry_date desc limit 1";
                List<BigDecimal> closingBalanceData = jdbcTemplate.queryForList(closingBalanceQuery, BigDecimal.class, officeId, accountId);
                List<TrialBalance> tbRows = this.trialBalanceRepositoryWrapper.findNewByOfficeAndAccount(officeId, accountId);
                BigDecimal closingBalance = null;
                if (!CollectionUtils.isEmpty(closingBalanceData)) {
                    closingBalance = closingBalanceData.get(0);
                }
                if (CollectionUtils.isEmpty(closingBalanceData)) {
                    closingBalance = BigDecimal.ZERO;
                    for (TrialBalance row : tbRows) {
                        closingBalance = closingBalance.add(row.getAmount());
                        row.setClosingBalance(closingBalance);
                    }
                } else {
                    for (TrialBalance tbRow : tbRows) {
                        closingBalance = closingBalance.add(tbRow.getAmount());
                        tbRow.setClosingBalance(closingBalance);
                    }
                }
                this.trialBalanceRepositoryWrapper.save(tbRows);
            }
        }

    }

    @Override
    @CronTarget(jobName = JobName.EXECUTE_DIRTY_JOBS)
    public void executeMissMatchedJobs() throws JobExecutionException {
        List<ScheduledJobDetail> jobDetails = this.scheduledJobDetailsRepository.findAllMismatchedJobs(true);

        for (ScheduledJobDetail scheduledJobDetail : jobDetails) {
            if (scheduledJobDetail.getNodeId().toString().equals(fineractProperties.getNodeId())) {
                jobRegisterService.executeJob(scheduledJobDetail.getId());
            }
        }
    }

}
