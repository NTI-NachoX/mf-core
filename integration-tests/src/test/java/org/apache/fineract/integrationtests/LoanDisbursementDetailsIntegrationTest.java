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
package org.apache.fineract.integrationtests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import org.apache.fineract.integrationtests.common.ClientHelper;
import org.apache.fineract.integrationtests.common.CollateralManagementHelper;
import org.apache.fineract.integrationtests.common.Utils;
import org.apache.fineract.integrationtests.common.loans.LoanApplicationTestBuilder;
import org.apache.fineract.integrationtests.common.loans.LoanDisbursementTestBuilder;
import org.apache.fineract.integrationtests.common.loans.LoanProductTestBuilder;
import org.apache.fineract.integrationtests.common.loans.LoanStatusChecker;
import org.apache.fineract.integrationtests.common.loans.LoanTransactionHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class LoanDisbursementDetailsIntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(LoanDisbursementDetailsIntegrationTest.class);
    private ResponseSpecification responseSpec;
    private RequestSpecification requestSpec;
    private LoanTransactionHelper loanTransactionHelper;
    private Integer loanID;
    private Integer disbursementId;
    final String approveDate = "01 March 2014";
    final String expectedDisbursementDate = "01 March 2014";
    final String proposedAmount = "5000";
    final String approvalAmount = "5000";

    @BeforeEach
    public void setup() {
        Utils.initializeRESTAssured();
        this.requestSpec = new RequestSpecBuilder().setContentType(ContentType.JSON).build();
        this.requestSpec.header("Authorization", "Basic " + Utils.loginIntoServerAndGetBase64EncodedAuthenticationKey());
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
        this.loanTransactionHelper = new LoanTransactionHelper(this.requestSpec, this.responseSpec);
    }

    @Test
    public void createAndValidateMultiDisburseLoansBasedOnEmi() {
        List<HashMap> createTranches = new ArrayList<>();
        String id = null;
        String installmentAmount = "800";
        String withoutInstallmentAmount = "";
        String proposedAmount = "10000";
        createTranches.add(this.loanTransactionHelper.createTrancheDetail(id, "01 June 2015", "5000"));
        createTranches.add(this.loanTransactionHelper.createTrancheDetail(id, "01 September 2015", "5000"));

        final Integer clientID = ClientHelper.createClient(this.requestSpec, this.responseSpec, "01 January 2014");
        LOG.info("---------------------------------CLIENT CREATED WITH ID---------------------------------------------------{}", clientID);

        final Integer loanProductID = this.loanTransactionHelper.getLoanProductId(new LoanProductTestBuilder()
                .withInterestTypeAsDecliningBalance().withMoratorium("", "").withAmortizationTypeAsEqualInstallments().withTranches(true)
                .withInterestCalculationPeriodTypeAsRepaymentPeriod(true).build(null));
        LOG.info("----------------------------------LOAN PRODUCT CREATED WITH ID------------------------------------------- {}",
                loanProductID);

        final Integer loanIDWithEmi = applyForLoanApplicationWithEmiAmount(clientID, loanProductID, proposedAmount, createTranches,
                installmentAmount);

        LOG.info("-----------------------------------LOAN CREATED WITH EMI LOANID------------------------------------------------- {}",
                loanIDWithEmi);

        HashMap repaymentScheduleWithEmi = (HashMap) this.loanTransactionHelper.getLoanDetail(this.requestSpec, this.responseSpec,
                loanIDWithEmi, "repaymentSchedule");

        ArrayList<HashMap> periods = (ArrayList<HashMap>) repaymentScheduleWithEmi.get("periods");
        assertEquals(15, periods.size());

        this.validateRepaymentScheduleWithEMI(periods);

        HashMap loanStatusHashMap = LoanStatusChecker.getStatusOfLoan(this.requestSpec, this.responseSpec, loanIDWithEmi);
        LoanStatusChecker.verifyLoanIsPending(loanStatusHashMap);

        LOG.info("-----------------------------------APPROVE LOAN-----------------------------------------------------------");
        loanStatusHashMap = this.loanTransactionHelper.approveLoanWithApproveAmount("01 June 2015", "01 June 2015", "10000", loanIDWithEmi,
                createTranches);
        LoanStatusChecker.verifyLoanIsApproved(loanStatusHashMap);
        LoanStatusChecker.verifyLoanIsWaitingForDisbursal(loanStatusHashMap);
        LOG.info(
                "-----------------------------------MULTI DISBURSAL LOAN WITH EMI APPROVED SUCCESSFULLY---------------------------------------");

        final Integer loanIDWithoutEmi = applyForLoanApplicationWithEmiAmount(clientID, loanProductID, proposedAmount, createTranches,
                withoutInstallmentAmount);

        HashMap repaymentScheduleWithoutEmi = (HashMap) this.loanTransactionHelper.getLoanDetail(this.requestSpec, this.responseSpec,
                loanIDWithoutEmi, "repaymentSchedule");

        ArrayList<HashMap> periods1 = (ArrayList<HashMap>) repaymentScheduleWithEmi.get("periods");
        assertEquals(15, periods1.size());

        LOG.info("-----------------------------------LOAN CREATED WITHOUT EMI LOANID------------------------------------------------- {}",
                loanIDWithoutEmi);

        /* To be uncommented once issue MIFOSX-2006 is closed. */
        // this.validateRepaymentScheduleWithoutEMI(periods1);

        HashMap loanStatusMap = LoanStatusChecker.getStatusOfLoan(this.requestSpec, this.responseSpec, loanIDWithoutEmi);
        LoanStatusChecker.verifyLoanIsPending(loanStatusMap);

        LOG.info("-----------------------------------APPROVE LOAN-----------------------------------------------------------");
        loanStatusHashMap = this.loanTransactionHelper.approveLoanWithApproveAmount("01 June 2015", "01 June 2015", "10000",
                loanIDWithoutEmi, createTranches);
        LoanStatusChecker.verifyLoanIsApproved(loanStatusHashMap);
        LoanStatusChecker.verifyLoanIsWaitingForDisbursal(loanStatusHashMap);
        LOG.info(
                "-----------------------------------MULTI DISBURSAL LOAN WITHOUT EMI APPROVED SUCCESSFULLY---------------------------------------");

    }

    private void validateRepaymentScheduleWithEMI(ArrayList<HashMap> periods) {
        LoanDisbursementTestBuilder expectedRepaymentSchedule0 = new LoanDisbursementTestBuilder("[2015, 6, 1]", 0.0f, 0.0f, null, null,
                5000.0f, null, null, null);

        LoanDisbursementTestBuilder expectedRepaymentSchedule1 = new LoanDisbursementTestBuilder("[2015, 7, 1]", 800f, 800.0f, 50.0f,
                750.0f, 4250.0f, 750.0f, 750.0f, "[2015, 6, 1]");

        LoanDisbursementTestBuilder expectedRepaymentSchedule2 = new LoanDisbursementTestBuilder("[2015, 8, 1]", 800.0f, 800.0f, 42.5f,
                757.5f, 3492.5f, 757.5f, 757.5f, "[2015, 7, 1]");

        LoanDisbursementTestBuilder expectedRepaymentSchedule3 = new LoanDisbursementTestBuilder("[2015, 9, 1]", 0.0f, 0.0f, null, null,
                5000.0f, null, null, null);

        LoanDisbursementTestBuilder expectedRepaymentSchedule4 = new LoanDisbursementTestBuilder("[2015, 9, 1]", 800.0f, 800.0f, 34.92f,
                765.08f, 7727.42f, 765.08f, 765.08f, "[2015, 8, 1]");

        LoanDisbursementTestBuilder expectedRepaymentSchedule5 = new LoanDisbursementTestBuilder("[2015, 10, 1]", 800.0f, 800.0f, 77.27f,
                722.73f, 7004.69f, 722.73f, 722.73f, "[2015, 9, 1]");

        LoanDisbursementTestBuilder expectedRepaymentSchedule6 = new LoanDisbursementTestBuilder("[2015, 11, 1]", 800.0f, 800.0f, 70.05f,
                729.95f, 6274.74f, 729.95f, 729.95f, "[2015, 10, 1]");

        LoanDisbursementTestBuilder expectedRepaymentSchedule7 = new LoanDisbursementTestBuilder("[2015, 12, 1]", 800.0f, 800.0f, 62.75f,
                737.25f, 5537.49f, 737.25f, 737.25f, "[2015, 11, 1]");

        LoanDisbursementTestBuilder expectedRepaymentSchedule8 = new LoanDisbursementTestBuilder("[2016, 1, 1]", 800.0f, 800.0f, 55.37f,
                744.63f, 4792.86f, 744.63f, 744.63f, "[2015, 12, 1]");

        LoanDisbursementTestBuilder expectedRepaymentSchedule9 = new LoanDisbursementTestBuilder("[2016, 2, 1]", 800.0f, 800.0f, 47.93f,
                752.07f, 4040.79f, 752.07f, 752.07f, "[2016, 1, 1]");

        LoanDisbursementTestBuilder expectedRepaymentSchedule10 = new LoanDisbursementTestBuilder("[2016, 3, 1]", 800.0f, 800.0f, 40.41f,
                759.59f, 3281.2f, 759.59f, 759.59f, "[2016, 2, 1]");

        LoanDisbursementTestBuilder expectedRepaymentSchedule11 = new LoanDisbursementTestBuilder("[2016, 4, 1]", 800.0f, 800.0f, 32.81f,
                767.19f, 2514.01f, 767.19f, 767.19f, "[2016, 3, 1]");

        LoanDisbursementTestBuilder expectedRepaymentSchedule12 = new LoanDisbursementTestBuilder("[2016, 5, 1]", 800.0f, 800.0f, 25.14f,
                774.86f, 1739.15f, 774.86f, 774.86f, "[2016, 4, 1]");

        LoanDisbursementTestBuilder expectedRepaymentSchedule13 = new LoanDisbursementTestBuilder("[2016, 6, 1]", 800.0f, 800.0f, 17.39f,
                782.61f, 956.54f, 782.61f, 782.61f, "[2016, 5, 1]");

        LoanDisbursementTestBuilder expectedRepaymentSchedule14 = new LoanDisbursementTestBuilder("[2016, 7, 1]", 966.11f, 966.11f, 9.57f,
                956.54f, 0.0f, 956.54f, 956.54f, "[2016, 6, 1]");

        ArrayList<LoanDisbursementTestBuilder> list = new ArrayList<LoanDisbursementTestBuilder>();
        list.add(expectedRepaymentSchedule0);
        list.add(expectedRepaymentSchedule1);
        list.add(expectedRepaymentSchedule2);
        list.add(expectedRepaymentSchedule3);
        list.add(expectedRepaymentSchedule4);
        list.add(expectedRepaymentSchedule5);
        list.add(expectedRepaymentSchedule6);
        list.add(expectedRepaymentSchedule7);
        list.add(expectedRepaymentSchedule8);
        list.add(expectedRepaymentSchedule9);
        list.add(expectedRepaymentSchedule10);
        list.add(expectedRepaymentSchedule11);
        list.add(expectedRepaymentSchedule12);
        list.add(expectedRepaymentSchedule13);
        list.add(expectedRepaymentSchedule14);

        for (int i = 0; i < list.size(); i++) {
            this.assertRepaymentScheduleValuesWithEMI(periods.get(i), list.get(i), i);
        }
    }

    private void assertRepaymentScheduleValuesWithEMI(HashMap period, LoanDisbursementTestBuilder expectedRepaymentSchedule, int position) {

        assertEquals(period.get("dueDate").toString(), expectedRepaymentSchedule.getDueDate());
        assertEquals(period.get("principalLoanBalanceOutstanding"), expectedRepaymentSchedule.getPrincipalLoanBalanceOutstanding());
        LOG.info("{}", period.get("totalOriginalDueForPeriod").toString());
        assertEquals(Float.parseFloat(period.get("totalOriginalDueForPeriod").toString()),
                expectedRepaymentSchedule.getTotalOriginalDueForPeriod().floatValue(), 0.0f);

        assertEquals(Float.parseFloat(period.get("totalOutstandingForPeriod").toString()),
                expectedRepaymentSchedule.getTotalOutstandingForPeriod(), 0.0f);

        if (position != 0 && position != 3) {

            assertEquals(Float.parseFloat(period.get("interestOutstanding").toString()), expectedRepaymentSchedule.getInterestOutstanding(),
                    0.0f);
            assertEquals(Float.parseFloat(period.get("principalOutstanding").toString()),
                    expectedRepaymentSchedule.getPrincipalOutstanding(), 0.0f);
            assertEquals(Float.parseFloat(period.get("principalDue").toString()), expectedRepaymentSchedule.getPrincipalDue(), 0.0f);
            assertEquals(Float.parseFloat(period.get("principalOriginalDue").toString()),
                    expectedRepaymentSchedule.getPrincipalOriginalDue(), 0.0f);
            assertEquals(period.get("fromDate").toString(), expectedRepaymentSchedule.getFromDate());
        }
    }

    /* Uncomment and modify test builder values once MIFOSX-2006 is closed. */
    /*
     * private void validateRepaymentScheduleWithoutEMI(ArrayList<HashMap> periods){ LoanDisbursementTestBuilder
     * expectedRepaymentSchedule0 = new LoanDisbursementTestBuilder( "[2015, 6, 1]", 0.0f, 0.0f, null, null, 5000.0f,
     * null, null, null);
     *
     * LoanDisbursementTestBuilder expectedRepaymentSchedule1 = new LoanDisbursementTestBuilder( "[2015, 7, 1]", 800.0f,
     * 800.0f, 50.0f, 750.0f, 4250.0f, 750.0f, 750.0f, "[2015, 6, 1]");
     *
     * LoanDisbursementTestBuilder expectedRepaymentSchedule2 = new LoanDisbursementTestBuilder( "[2015, 8, 1]", 800.0f,
     * 800.0f, 42.5f, 757.5f, 3492.5f, 757.5f, 757.5f, "[2015, 7, 1]");
     *
     * LoanDisbursementTestBuilder expectedRepaymentSchedule3 = new LoanDisbursementTestBuilder( "[2015, 9, 1]", 0.0f,
     * 0.0f, null, null, 5000.0f, null, null, null);
     *
     * LoanDisbursementTestBuilder expectedRepaymentSchedule4 = new LoanDisbursementTestBuilder( "[2015, 9, 1]", 800.0f,
     * 800.0f, 34.92f, 765.08f, 7727.42f, 765.08f, 765.08f, "[2015, 8, 1]");
     *
     * LoanDisbursementTestBuilder expectedRepaymentSchedule5 = new LoanDisbursementTestBuilder( "[2015, 10, 1]",
     * 800.0f, 800.0f, 77.27f, 722.73f, 7004.69f, 722.73f, 722.73f, "[2015, 9, 1]");
     *
     * LoanDisbursementTestBuilder expectedRepaymentSchedule6 = new LoanDisbursementTestBuilder( "[2015, 11, 1]",
     * 800.0f, 800.0f, 70.05f, 729.95f, 6274.74f, 729.95f, 729.95f, "[2015, 10, 1]");
     *
     * LoanDisbursementTestBuilder expectedRepaymentSchedule7 = new LoanDisbursementTestBuilder( "[2015, 12, 1]",
     * 800.0f, 800.0f, 62.75f, 737.25f, 5537.49f, 737.25f, 737.25f, "[2015, 11, 1]");
     *
     * LoanDisbursementTestBuilder expectedRepaymentSchedule8 = new LoanDisbursementTestBuilder( "[2016, 1, 1]", 800.0f,
     * 800.0f, 55.37f, 744.63f, 4792.86f, 744.63f, 744.63f, "[2015, 12, 1]");
     *
     * LoanDisbursementTestBuilder expectedRepaymentSchedule9 = new LoanDisbursementTestBuilder( "[2016, 2, 1]", 800.0f,
     * 800.0f, 47.93f, 752.07f, 4040.79f, 752.07f, 752.07f, "[2016, 1, 1]");
     *
     * LoanDisbursementTestBuilder expectedRepaymentSchedule10 = new LoanDisbursementTestBuilder( "[2016, 3, 1]",
     * 800.0f, 800.0f, 40.41f, 759.59f, 3281.2f, 759.59f, 759.59f, "[2016, 2, 1]");
     *
     * LoanDisbursementTestBuilder expectedRepaymentSchedule11 = new LoanDisbursementTestBuilder( "[2016, 4, 1]",
     * 800.0f, 800.0f, 32.81f, 767.19f, 2514.01f, 767.19f, 767.19f, "[2016, 3, 1]");
     *
     * LoanDisbursementTestBuilder expectedRepaymentSchedule12 = new LoanDisbursementTestBuilder( "[2016, 5, 1]",
     * 800.0f, 800.0f, 25.14f, 774.86f, 1739.15f, 774.86f, 774.86f, "[2016, 4, 1]");
     *
     * LoanDisbursementTestBuilder expectedRepaymentSchedule13 = new LoanDisbursementTestBuilder( "[2016, 6, 1]",
     * 800.0f, 800.0f, 17.39f, 782.61f, 956.54f, 782.61f, 782.61f, "[2016, 5, 1]");
     *
     * LoanDisbursementTestBuilder expectedRepaymentSchedule14 = new LoanDisbursementTestBuilder( "[2016, 7, 1]",
     * 966.11f, 966.11f, 9.57f, 956.54f, 0.0f, 956.54f, 956.54f, "[2016, 6, 1]");
     *
     * ArrayList<LoanDisbursementTestBuilder> list = new ArrayList<LoanDisbursementTestBuilder>();
     * list.add(expectedRepaymentSchedule0); list.add(expectedRepaymentSchedule1); list.add(expectedRepaymentSchedule2);
     * list.add(expectedRepaymentSchedule3); list.add(expectedRepaymentSchedule4); list.add(expectedRepaymentSchedule5);
     * list.add(expectedRepaymentSchedule6); list.add(expectedRepaymentSchedule7); list.add(expectedRepaymentSchedule8);
     * list.add(expectedRepaymentSchedule9); list.add(expectedRepaymentSchedule10);
     * list.add(expectedRepaymentSchedule11); list.add(expectedRepaymentSchedule12);
     * list.add(expectedRepaymentSchedule13); list.add(expectedRepaymentSchedule14);
     *
     * for (int i = 0; i < list.size(); i++) { this.assertRepaymentScheduleValuesWithoutEMI(periods.get(i), list.get(i),
     * i); } }
     *
     * private void assertRepaymentScheduleValuesWithoutEMI(HashMap period, LoanDisbursementTestBuilder
     * expectedRepaymentSchedule, int position) {
     *
     * assertEquals(period.get("dueDate").toString(), expectedRepaymentSchedule.getDueDate());
     * assertEquals(period.get("principalLoanBalanceOutstanding"),
     * expectedRepaymentSchedule.getPrincipalLoanBalanceOutstanding());
     * assertEquals(period.get("totalOriginalDueForPeriod"), expectedRepaymentSchedule.getTotalOriginalDueForPeriod());
     * assertEquals(period.get("totalOutstandingForPeriod"), expectedRepaymentSchedule.getTotalOutstandingForPeriod());
     *
     * if (position != 0 && position != 3) {
     *
     * assertEquals(period.get("interestOutstanding"), expectedRepaymentSchedule.getInterestOutstanding());
     * assertEquals(period.get("principalOutstanding"), expectedRepaymentSchedule.getPrincipalOutstanding());
     * assertEquals(period.get("principalDue"), expectedRepaymentSchedule.getPrincipalDue());
     * assertEquals(period.get("principalOriginalDue"), expectedRepaymentSchedule.getPrincipalOriginalDue());
     * assertEquals(period.get("fromDate").toString(), expectedRepaymentSchedule.getFromDate()); } }
     */
    private Integer applyForLoanApplicationWithEmiAmount(final Integer clientID, final Integer loanProductID, final String proposedAmount,
            List<HashMap> tranches, final String installmentAmount) {

        LOG.info("--------------------------------APPLYING FOR LOAN APPLICATION--------------------------------");
        List<HashMap> collaterals = new ArrayList<>();
        final Integer collateralId = CollateralManagementHelper.createCollateralProduct(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(collateralId);
        final Integer clientCollateralId = CollateralManagementHelper.createClientCollateral(this.requestSpec, this.responseSpec,
                clientID.toString(), collateralId);
        Assertions.assertNotNull(clientCollateralId);
        addCollaterals(collaterals, clientCollateralId, BigDecimal.valueOf(1));

        final String loanApplicationJSON = new LoanApplicationTestBuilder()
                //
                .withPrincipal(proposedAmount)
                //
                .withLoanTermFrequency("12")
                //
                .withLoanTermFrequencyAsMonths()
                //
                .withNumberOfRepayments("12").withRepaymentEveryAfter("1").withRepaymentFrequencyTypeAsMonths() //
                .withInterestRatePerPeriod("1") //
                .withExpectedDisbursementDate("01 June 2015") //
                .withTranches(tranches) //
                .withFixedEmiAmount(installmentAmount) //
                .withInterestTypeAsDecliningBalance() //
                .withSubmittedOnDate("01 June 2015") //
                .withAmortizationTypeAsEqualInstallments() //
                .withCollaterals(collaterals).build(clientID.toString(), loanProductID.toString(), null);

        return this.loanTransactionHelper.getLoanId(loanApplicationJSON);

    }

    @Test
    public void createApproveAndValidateMultiDisburseLoan() throws ParseException {

        List<HashMap> createTranches = new ArrayList<>();
        String id = null;
        createTranches.add(this.loanTransactionHelper.createTrancheDetail(id, "01 March 2014", "1000"));

        final Integer clientID = ClientHelper.createClient(this.requestSpec, this.responseSpec, "01 January 2014");
        LOG.info("---------------------------------CLIENT CREATED WITH ID--------------------------------------------------- {}", clientID);

        final Integer loanProductID = this.loanTransactionHelper
                .getLoanProductId(new LoanProductTestBuilder().withInterestTypeAsDecliningBalance().withTranches(true)
                        .withInterestCalculationPeriodTypeAsRepaymentPeriod(true).build(null));
        LOG.info("----------------------------------LOAN PRODUCT CREATED WITH ID------------------------------------------- {}",
                loanProductID);

        this.loanID = applyForLoanApplicationWithTranches(clientID, loanProductID, proposedAmount, createTranches);
        LOG.info("-----------------------------------LOAN CREATED WITH LOANID------------------------------------------------- {}", loanID);

        HashMap loanStatusHashMap = LoanStatusChecker.getStatusOfLoan(this.requestSpec, this.responseSpec, loanID);
        LoanStatusChecker.verifyLoanIsPending(loanStatusHashMap);

        LOG.info("-----------------------------------APPROVE LOAN-----------------------------------------------------------");
        loanStatusHashMap = this.loanTransactionHelper.approveLoanWithApproveAmount(approveDate, expectedDisbursementDate, approvalAmount,
                loanID, createTranches);
        LoanStatusChecker.verifyLoanIsApproved(loanStatusHashMap);
        LoanStatusChecker.verifyLoanIsWaitingForDisbursal(loanStatusHashMap);
        LOG.info("-----------------------------------MULTI DISBURSAL LOAN APPROVED SUCCESSFULLY---------------------------------------");
        ArrayList<HashMap> disbursementDetails = (ArrayList<HashMap>) this.loanTransactionHelper.getLoanDetail(this.requestSpec,
                this.responseSpec, this.loanID, "disbursementDetails");
        this.disbursementId = (Integer) disbursementDetails.get(0).get("id");
        this.editLoanDisbursementDetails();
    }

    private void editLoanDisbursementDetails() throws ParseException {
        this.editDateAndPrincipalOfExistingTranche();
        this.addNewDisbursementDetails();
        this.deleteDisbursmentDetails();
    }

    private void addNewDisbursementDetails() throws ParseException {
        List<HashMap> addTranches = new ArrayList<>();
        ArrayList<HashMap> disbursementDetails = (ArrayList<HashMap>) this.loanTransactionHelper.getLoanDetail(this.requestSpec,
                this.responseSpec, this.loanID, "disbursementDetails");
        ArrayList expectedDisbursementDate = (ArrayList) disbursementDetails.get(0).get("expectedDisbursementDate");
        String date = formatExpectedDisbursementDate(expectedDisbursementDate.toString());

        String id = null;
        addTranches.add(this.loanTransactionHelper.createTrancheDetail(disbursementDetails.get(0).get("id").toString(), date,
                disbursementDetails.get(0).get("principal").toString()));
        addTranches.add(this.loanTransactionHelper.createTrancheDetail(id, "03 March 2014", "2000"));
        addTranches.add(this.loanTransactionHelper.createTrancheDetail(id, "04 March 2014", "500"));

        /* Add disbursement detail */
        this.loanTransactionHelper.addAndDeleteDisbursementDetail(this.loanID, this.approvalAmount, this.expectedDisbursementDate,
                addTranches, "");
    }

    private void deleteDisbursmentDetails() throws ParseException {
        List<HashMap> deleteTranches = new ArrayList<>();
        ArrayList<HashMap> disbursementDetails = (ArrayList<HashMap>) this.loanTransactionHelper.getLoanDetail(this.requestSpec,
                this.responseSpec, this.loanID, "disbursementDetails");
        /* Delete the last tranche */
        for (int i = 0; i < disbursementDetails.size() - 1; i++) {
            ArrayList expectedDisbursementDate = (ArrayList) disbursementDetails.get(i).get("expectedDisbursementDate");
            String disbursementDate = formatExpectedDisbursementDate(expectedDisbursementDate.toString());
            deleteTranches.add(this.loanTransactionHelper.createTrancheDetail(disbursementDetails.get(i).get("id").toString(),
                    disbursementDate, disbursementDetails.get(i).get("principal").toString()));
        }

        /* Add disbursement detail */
        this.loanTransactionHelper.addAndDeleteDisbursementDetail(this.loanID, this.approvalAmount, this.expectedDisbursementDate,
                deleteTranches, "");
    }

    private void editDateAndPrincipalOfExistingTranche() throws ParseException {
        String updatedExpectedDisbursementDate = "01 March 2014";
        String updatedPrincipal = "900";
        /* Update */
        this.loanTransactionHelper.editDisbursementDetail(this.loanID, this.disbursementId, this.approvalAmount,
                this.expectedDisbursementDate, updatedExpectedDisbursementDate, updatedPrincipal, "");
        /* Validate Edit */
        ArrayList<HashMap> disbursementDetails = (ArrayList<HashMap>) this.loanTransactionHelper.getLoanDetail(this.requestSpec,
                this.responseSpec, this.loanID, "disbursementDetails");
        assertEquals(Float.parseFloat(updatedPrincipal), disbursementDetails.get(0).get("principal"));
        ArrayList expectedDisbursementDate = (ArrayList) disbursementDetails.get(0).get("expectedDisbursementDate");
        String date = formatExpectedDisbursementDate(expectedDisbursementDate.toString());
        assertEquals(updatedExpectedDisbursementDate, date);

    }

    private String formatExpectedDisbursementDate(String expectedDisbursementDate) throws ParseException {

        SimpleDateFormat source = new SimpleDateFormat("[yyyy, MM, dd]");
        SimpleDateFormat target = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
        String date = target.format(source.parse(expectedDisbursementDate));

        return date;
    }

    private Integer applyForLoanApplicationWithTranches(final Integer clientID, final Integer loanProductID, String principal,
            List<HashMap> tranches) {
        LOG.info("--------------------------------APPLYING FOR LOAN APPLICATION--------------------------------");
        List<HashMap> collaterals = new ArrayList<>();
        final Integer collateralId = CollateralManagementHelper.createCollateralProduct(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(collateralId);
        final Integer clientCollateralId = CollateralManagementHelper.createClientCollateral(this.requestSpec, this.responseSpec,
                clientID.toString(), collateralId);
        Assertions.assertNotNull(clientCollateralId);
        addCollaterals(collaterals, clientCollateralId, BigDecimal.valueOf(1));
        final String loanApplicationJSON = new LoanApplicationTestBuilder()
                //
                .withPrincipal(principal)
                //
                .withLoanTermFrequency("5")
                //
                .withLoanTermFrequencyAsMonths()
                //
                .withNumberOfRepayments("5").withRepaymentEveryAfter("1").withRepaymentFrequencyTypeAsMonths() //
                .withInterestRatePerPeriod("2") //
                .withExpectedDisbursementDate("01 March 2014") //
                .withTranches(tranches) //
                .withInterestTypeAsDecliningBalance() //
                .withSubmittedOnDate("01 March 2014") //
                .withCollaterals(collaterals).build(clientID.toString(), loanProductID.toString(), null);

        return this.loanTransactionHelper.getLoanId(loanApplicationJSON);
    }

    private void addCollaterals(List<HashMap> collaterals, Integer collateralId, BigDecimal quantity) {
        collaterals.add(collaterals(collateralId, quantity));
    }

    private HashMap<String, String> collaterals(Integer collateralId, BigDecimal quantity) {
        HashMap<String, String> collateral = new HashMap<String, String>(2);
        collateral.put("clientCollateralId", collateralId.toString());
        collateral.put("quantity", quantity.toString());
        return collateral;
    }

}
