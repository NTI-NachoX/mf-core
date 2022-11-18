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
package org.apache.fineract.integrationtests.common.system;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.apache.fineract.integrationtests.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatatableHelper {

    private static final Logger LOG = LoggerFactory.getLogger(DatatableHelper.class);
    private final RequestSpecification requestSpec;
    private final ResponseSpecification responseSpec;

    private static final String DATATABLE_URL = "/fineract-provider/api/v1/datatables";

    public DatatableHelper(final RequestSpecification requestSpec, final ResponseSpecification responseSpec) {
        this.requestSpec = requestSpec;
        this.responseSpec = responseSpec;
    }

    public String createDatatable(final String apptableName, final boolean multiRow) {
        return Utils.performServerPost(this.requestSpec, this.responseSpec, DATATABLE_URL + "?" + Utils.TENANT_IDENTIFIER,
                getTestDatatableAsJSON(apptableName, multiRow), "resourceIdentifier");
    }

    public Integer createDatatableEntry(final String apptableName, final String datatableName, final Integer apptableId,
            final boolean genericResultSet, final String dateFormat, final String jsonAttributeToGetBack) {
        return Utils.performServerPost(
                this.requestSpec, this.responseSpec, DATATABLE_URL + "/" + datatableName + "/" + apptableId + "?genericResultSet="
                        + Boolean.toString(genericResultSet) + "&" + Utils.TENANT_IDENTIFIER,
                getTestDatatableEntryAsJSON(dateFormat), jsonAttributeToGetBack);
    }

    public String readDatatableEntry(final String datatableName, final Integer resourceId, final boolean genericResultset) {
        return Utils.performServerGet(this.requestSpec, this.responseSpec, DATATABLE_URL + "/" + datatableName + "/" + resourceId
                + "?genericResultSet=" + String.valueOf(genericResultset) + "&" + Utils.TENANT_IDENTIFIER);
    }

    public List<String> readDatatableEntry(final String datatableName, final Integer resourceId, final boolean genericResultset,
            final Integer datatableResourceId, final String jsonAttributeToGetBack) {
        if (datatableResourceId == null) {
            return Utils.performServerGetList(this.requestSpec, this.responseSpec, DATATABLE_URL + "/" + datatableName + "/" + resourceId
                    + "?genericResultSet=" + String.valueOf(genericResultset) + "&" + Utils.TENANT_IDENTIFIER, jsonAttributeToGetBack);
        } else {
            return Utils.performServerGetList(
                    this.requestSpec, this.responseSpec, DATATABLE_URL + "/" + datatableName + "/" + resourceId + "/" + datatableResourceId
                            + "?genericResultSet=" + String.valueOf(genericResultset) + "&" + Utils.TENANT_IDENTIFIER,
                    jsonAttributeToGetBack);
        }
    }

    public Date readDatatableEntry(final String datatableName, final Integer resourceId, final boolean genericResultset, final int position,
            final String jsonAttributeToGetBack) {
        final JsonElement jsonElement = Utils.performServerGetArray(this.requestSpec, this.responseSpec, DATATABLE_URL + "/" + datatableName
                + "/" + resourceId + "?genericResultSet=" + String.valueOf(genericResultset) + "&" + Utils.TENANT_IDENTIFIER, position,
                jsonAttributeToGetBack);
        return Utils.convertJsonElementAsDate(jsonElement);
    }

    public String deleteDatatable(final String datatableName) {
        return Utils.performServerDelete(this.requestSpec, this.responseSpec,
                DATATABLE_URL + "/" + datatableName + "?" + Utils.TENANT_IDENTIFIER, "resourceIdentifier");
    }

    public Integer deleteDatatableEntries(final String datatableName, final Integer apptableId, String jsonAttributeToGetBack) {
        final String deleteEntryUrl = DATATABLE_URL + "/" + datatableName + "/" + apptableId + "?genericResultSet=true" + "&"
                + Utils.TENANT_IDENTIFIER;
        return Utils.performServerDelete(this.requestSpec, this.responseSpec, deleteEntryUrl, jsonAttributeToGetBack);
    }

    public static void verifyDatatableCreatedOnServer(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final String generatedDatatableName) {
        LOG.info("------------------------------CHECK DATATABLE DETAILS------------------------------------\n");
        final String responseRegisteredTableName = Utils.performServerGet(requestSpec, responseSpec,
                DATATABLE_URL + "/" + generatedDatatableName + "?" + Utils.TENANT_IDENTIFIER, "registeredTableName");
        assertEquals(generatedDatatableName, responseRegisteredTableName, "ERROR IN CREATING THE DATATABLE");
    }

    public static String getTestDatatableAsJSON(final String apptableName, final boolean multiRow) {
        final HashMap<String, Object> map = new HashMap<>();
        final List<HashMap<String, Object>> datatableColumnsList = new ArrayList<>();
        map.put("datatableName", Utils.randomNameGenerator(apptableName + "_", 5));
        map.put("apptableName", apptableName);
        map.put("entitySubType", "PERSON");
        map.put("multiRow", multiRow);
        addDatatableColumns(datatableColumnsList, "Spouse Name", "String", true, 25);
        addDatatableColumns(datatableColumnsList, "Number of Dependents", "Number", true, null);
        addDatatableColumns(datatableColumnsList, "Time of Visit", "DateTime", false, null);
        addDatatableColumns(datatableColumnsList, "Date of Approval", "Date", false, null);
        map.put("columns", datatableColumnsList);
        String requestJsonString = new Gson().toJson(map);
        LOG.info("map : {}", requestJsonString);
        return requestJsonString;
    }

    public static String getTestDatatableEntryAsJSON(final String dateFormat) {
        final HashMap<String, Object> map = new HashMap<>();
        map.put("Spouse Name", Utils.randomNameGenerator("Spouse_Name_", 5));
        map.put("Number of Dependents", Utils.randomNumberGenerator(1));
        map.put("Date of Approval", Utils.convertDateToURLFormat(Calendar.getInstance(), dateFormat));
        map.put("locale", "en");
        map.put("dateFormat", dateFormat);

        String requestJsonString = new Gson().toJson(map);
        LOG.info("map : {}", requestJsonString);
        return requestJsonString;
    }

    public static List<HashMap<String, Object>> addDatatableColumns(List<HashMap<String, Object>> datatableColumnsList, String columnName,
            String columnType, boolean isMandatory, Integer length) {

        final HashMap<String, Object> datatableColumnMap = new HashMap<>();

        datatableColumnMap.put("name", columnName);
        datatableColumnMap.put("type", columnType);
        datatableColumnMap.put("mandatory", isMandatory);
        if (length != null) {
            datatableColumnMap.put("length", length);
        }

        datatableColumnsList.add(datatableColumnMap);
        return datatableColumnsList;
    }

}
