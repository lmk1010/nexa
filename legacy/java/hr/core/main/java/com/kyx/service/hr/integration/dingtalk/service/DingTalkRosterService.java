package com.kyx.service.hr.integration.dingtalk.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.kyx.service.hr.config.DingTalkProperties;
import com.kyx.service.hr.integration.dingtalk.client.DingTalkOpenApiClient;
import com.kyx.service.hr.integration.dingtalk.model.DingTalkRosterSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Reads DingTalk intelligent HR roster fields.
 * Field codes from: roster meta get + smartwork hrm employee list.
 */
@Service
@Slf4j
public class DingTalkRosterService {

    private static final String ROSTER_QUERY_PATH = "/v1.0/hrm/rosters/lists/query";
    private static final int MAX_USER_BATCH_SIZE = 100;

    // sys00 基础
    public static final String FIELD_NAME = "sys00-name";
    public static final String FIELD_EMAIL = "sys00-email";
    public static final String FIELD_MOBILE = "sys00-mobile";
    public static final String FIELD_JOB_NUMBER = "sys00-jobNumber";
    public static final String FIELD_POSITION = "sys00-position";
    public static final String FIELD_WORK_PLACE = "sys00-workPlace";
    public static final String FIELD_REMARK = "sys00-remark";
    public static final String FIELD_REPORT_MANAGER = "sys00-reportManager";
    public static final String FIELD_REPORT_MANAGER_ID = "sys00-reportManagerId";
    public static final String FIELD_CONFIRM_JOIN_TIME = "sys00-confirmJoinTime";

    // sys01 工作
    public static final String FIELD_EMPLOYEE_TYPE = "sys01-employeeType";
    public static final String FIELD_EMPLOYEE_STATUS = "sys01-employeeStatus";
    public static final String FIELD_PROBATION_PERIOD_TYPE = "sys01-probationPeriodType";
    public static final String FIELD_REGULAR_TIME = "sys01-regularTime";
    public static final String FIELD_PLAN_REGULAR_TIME = "sys01-planRegularTime";
    public static final String FIELD_POSITION_LEVEL = "sys01-positionLevel";

    // sys02 个人信息
    public static final String FIELD_REAL_NAME = "sys02-realName";
    public static final String FIELD_CERT_NO = "sys02-certNo";
    public static final String FIELD_BIRTH_TIME = "sys02-birthTime";
    public static final String FIELD_SEX_TYPE = "sys02-sexType";
    public static final String FIELD_NATION_TYPE = "sys02-nationType";
    public static final String FIELD_CERT_ADDRESS = "sys02-certAddress";
    public static final String FIELD_CERT_END_TIME = "sys02-certEndTime";
    public static final String FIELD_RESIDENCE_TYPE = "sys02-residenceType";
    public static final String FIELD_ADDRESS = "sys02-address";
    public static final String FIELD_POLITICAL_STATUS = "sys02-politicalStatus";
    public static final String FIELD_MARRIAGE = "sys02-marriage";
    public static final String FIELD_JOIN_WORKING_TIME = "sys02-joinWorkingTime";
    public static final String FIELD_PERSONAL_SI = "sys09-personalSi";
    public static final String FIELD_PERSONAL_HF = "sys09-personalHf";

    // sys03 学历
    public static final String FIELD_HIGHEST_EDU = "sys03-highestEdu";
    public static final String FIELD_GRADUATE_SCHOOL = "sys03-graduateSchool";
    public static final String FIELD_GRADUATION_TIME = "sys03-graduationTime";
    public static final String FIELD_MAJOR = "sys03-major";

    // sys04 银行卡
    public static final String FIELD_BANK_ACCOUNT_NO = "sys04-bankAccountNo";
    public static final String FIELD_ACCOUNT_BANK = "sys04-accountBank";

    // sys05 合同
    public static final String FIELD_CONTRACT_COMPANY_NAME = "sys05-contractCompanyName";
    public static final String FIELD_CONTRACT_TYPE = "sys05-contractType";
    public static final String FIELD_FIRST_CONTRACT_START_TIME = "sys05-firstContractStartTime";
    public static final String FIELD_FIRST_CONTRACT_END_TIME = "sys05-firstContractEndTime";
    public static final String FIELD_NOW_CONTRACT_START_TIME = "sys05-nowContractStartTime";
    public static final String FIELD_NOW_CONTRACT_END_TIME = "sys05-nowContractEndTime";
    public static final String FIELD_CONTRACT_PERIOD_TYPE = "sys05-contractPeriodType";
    public static final String FIELD_CONTRACT_RENEW_COUNT = "sys05-contractRenewCount";

    // sys06 紧急联系人
    public static final String FIELD_URGENT_CONTACTS_NAME = "sys06-urgentContactsName";
    public static final String FIELD_URGENT_CONTACTS_RELATION = "sys06-urgentContactsRelation";
    public static final String FIELD_URGENT_CONTACTS_PHONE = "sys06-urgentContactsPhone";

    // sys07 家庭
    public static final String FIELD_HAVE_CHILD = "sys07-haveChild";
    public static final String FIELD_CHILD_NAME = "sys07-childName";
    public static final String FIELD_CHILD_SEX = "sys07-childSex";
    public static final String FIELD_CHILD_BIRTH_DATE = "sys07-childBirthDate";

    /**
     * 全量业务字段（不含证件照片等附件字段，附件单独处理）。
     * 来源：topapi/smartwork/hrm/roster/meta/get + 实测 employee/list。
     */
    private static final List<String> FIELD_FILTER_LIST = Arrays.asList(
            FIELD_NAME, FIELD_EMAIL, FIELD_MOBILE, FIELD_JOB_NUMBER, FIELD_POSITION, FIELD_WORK_PLACE,
            FIELD_REMARK, FIELD_REPORT_MANAGER, FIELD_REPORT_MANAGER_ID, FIELD_CONFIRM_JOIN_TIME,
            FIELD_EMPLOYEE_TYPE, FIELD_EMPLOYEE_STATUS, FIELD_PROBATION_PERIOD_TYPE, FIELD_REGULAR_TIME,
            FIELD_PLAN_REGULAR_TIME, FIELD_POSITION_LEVEL,
            FIELD_REAL_NAME, FIELD_CERT_NO, FIELD_BIRTH_TIME, FIELD_SEX_TYPE, FIELD_NATION_TYPE,
            FIELD_CERT_ADDRESS, FIELD_CERT_END_TIME, FIELD_RESIDENCE_TYPE, FIELD_ADDRESS,
            FIELD_POLITICAL_STATUS, FIELD_MARRIAGE, FIELD_JOIN_WORKING_TIME, FIELD_PERSONAL_SI, FIELD_PERSONAL_HF,
            FIELD_HIGHEST_EDU, FIELD_GRADUATE_SCHOOL, FIELD_GRADUATION_TIME, FIELD_MAJOR,
            FIELD_BANK_ACCOUNT_NO, FIELD_ACCOUNT_BANK,
            FIELD_CONTRACT_COMPANY_NAME, FIELD_CONTRACT_TYPE, FIELD_FIRST_CONTRACT_START_TIME,
            FIELD_FIRST_CONTRACT_END_TIME, FIELD_NOW_CONTRACT_START_TIME, FIELD_NOW_CONTRACT_END_TIME,
            FIELD_CONTRACT_PERIOD_TYPE, FIELD_CONTRACT_RENEW_COUNT,
            FIELD_URGENT_CONTACTS_NAME, FIELD_URGENT_CONTACTS_RELATION, FIELD_URGENT_CONTACTS_PHONE,
            FIELD_HAVE_CHILD, FIELD_CHILD_NAME, FIELD_CHILD_SEX, FIELD_CHILD_BIRTH_DATE
    );

    @Resource
    private DingTalkOpenApiClient dingTalkOpenApiClient;
    @Resource
    private DingTalkProperties dingTalkProperties;

    public Map<String, DingTalkRosterSnapshot> listRosterByUserIds(List<String> userIds) {
        Map<String, DingTalkRosterSnapshot> result = new LinkedHashMap<>();
        List<String> normalizedUserIds = normalizeUserIds(userIds);
        if (normalizedUserIds.isEmpty()) {
            return result;
        }

        Object appAgentId = resolveAppAgentId();
        long intervalMs = safeRosterBatchIntervalMs();
        for (int from = 0; from < normalizedUserIds.size(); from += MAX_USER_BATCH_SIZE) {
            int to = Math.min(from + MAX_USER_BATCH_SIZE, normalizedUserIds.size());
            List<String> batch = normalizedUserIds.subList(from, to);
            Map<String, Object> body = dingTalkOpenApiClient.body();
            body.put("userIdList", batch);
            body.put("fieldFilterList", FIELD_FILTER_LIST);
            body.put("appAgentId", appAgentId);
            body.put("text2SelectConvert", true);

            JsonNode root = dingTalkOpenApiClient.postOpenApiWithRetry(ROSTER_QUERY_PATH, body);
            mergeBatchResult(result, batch, root);
            sleepBetweenBatches(intervalMs);
        }
        return result;
    }

    private void mergeBatchResult(Map<String, DingTalkRosterSnapshot> result,
                                  List<String> requestUserIds,
                                  JsonNode root) {
        JsonNode rows = resolveRows(root);
        if (!rows.isArray()) {
            return;
        }
        int index = 0;
        for (JsonNode row : rows) {
            DingTalkRosterSnapshot snapshot = toSnapshot(row);
            if (!StringUtils.hasText(snapshot.getUserId()) && index < requestUserIds.size()) {
                snapshot.setUserId(requestUserIds.get(index));
            }
            index++;
            if (StringUtils.hasText(snapshot.getUserId())) {
                result.put(snapshot.getUserId(), snapshot);
            }
        }
    }

    private DingTalkRosterSnapshot toSnapshot(JsonNode row) {
        DingTalkRosterSnapshot snapshot = new DingTalkRosterSnapshot();
        snapshot.setUserId(text(row, "userid", "userId", "user_id"));
        snapshot.setRawPayload(row.toString());

        JsonNode fieldDataList = firstArray(row, "fieldDataList", "field_data_list", "fieldList", "fields", "field_list");
        if (fieldDataList == null) {
            return snapshot;
        }
        for (JsonNode fieldNode : fieldDataList) {
            String fieldCode = text(fieldNode, "fieldCode", "field_code", "code");
            if (!StringUtils.hasText(fieldCode)) {
                continue;
            }
            String value = fieldValue(fieldNode);
            snapshot.getFieldValues().put(fieldCode, value);
            applyKnownField(snapshot, fieldCode, value);
        }
        return snapshot;
    }

    private void applyKnownField(DingTalkRosterSnapshot snapshot, String fieldCode, String value) {
        switch (fieldCode) {
            case FIELD_NAME:
                snapshot.setName(value);
                break;
            case FIELD_EMAIL:
                snapshot.setEmail(value);
                break;
            case FIELD_MOBILE:
                snapshot.setMobile(value);
                break;
            case FIELD_JOB_NUMBER:
                snapshot.setJobNumber(value);
                break;
            case FIELD_POSITION:
                snapshot.setPosition(value);
                break;
            case FIELD_WORK_PLACE:
                snapshot.setWorkPlace(value);
                break;
            case FIELD_REMARK:
                snapshot.setRemark(value);
                break;
            case FIELD_REPORT_MANAGER:
                snapshot.setReportManager(value);
                break;
            case FIELD_REPORT_MANAGER_ID:
                snapshot.setReportManagerId(value);
                break;
            case FIELD_CONFIRM_JOIN_TIME:
                snapshot.setConfirmJoinTime(value);
                break;
            case FIELD_EMPLOYEE_TYPE:
                snapshot.setEmployeeType(value);
                break;
            case FIELD_EMPLOYEE_STATUS:
                snapshot.setEmployeeStatus(value);
                break;
            case FIELD_PROBATION_PERIOD_TYPE:
                snapshot.setProbationPeriodType(value);
                break;
            case FIELD_REGULAR_TIME:
                snapshot.setRegularTime(value);
                break;
            case FIELD_PLAN_REGULAR_TIME:
                snapshot.setPlanRegularTime(value);
                break;
            case FIELD_POSITION_LEVEL:
                snapshot.setPositionLevel(value);
                break;
            case FIELD_REAL_NAME:
                snapshot.setRealName(value);
                break;
            case FIELD_CERT_NO:
                snapshot.setCertNo(value);
                break;
            case FIELD_BIRTH_TIME:
                snapshot.setBirthTime(value);
                break;
            case FIELD_SEX_TYPE:
                snapshot.setSexType(value);
                break;
            case FIELD_NATION_TYPE:
                snapshot.setNationType(value);
                break;
            case FIELD_CERT_ADDRESS:
                snapshot.setCertAddress(value);
                break;
            case FIELD_CERT_END_TIME:
                snapshot.setCertEndTime(value);
                break;
            case FIELD_RESIDENCE_TYPE:
                snapshot.setResidenceType(value);
                break;
            case FIELD_ADDRESS:
                snapshot.setAddress(value);
                break;
            case FIELD_POLITICAL_STATUS:
                snapshot.setPoliticalStatus(value);
                break;
            case FIELD_MARRIAGE:
                snapshot.setMarriage(value);
                break;
            case FIELD_JOIN_WORKING_TIME:
                snapshot.setJoinWorkingTime(value);
                break;
            case FIELD_PERSONAL_SI:
                snapshot.setPersonalSi(value);
                break;
            case FIELD_PERSONAL_HF:
                snapshot.setPersonalHf(value);
                break;
            case FIELD_HIGHEST_EDU:
                snapshot.setHighestEdu(value);
                break;
            case FIELD_GRADUATE_SCHOOL:
                snapshot.setGraduateSchool(value);
                break;
            case FIELD_GRADUATION_TIME:
                snapshot.setGraduationTime(value);
                break;
            case FIELD_MAJOR:
                snapshot.setMajor(value);
                break;
            case FIELD_BANK_ACCOUNT_NO:
                snapshot.setBankAccountNo(value);
                break;
            case FIELD_ACCOUNT_BANK:
                snapshot.setAccountBank(value);
                break;
            case FIELD_CONTRACT_COMPANY_NAME:
                snapshot.setContractCompanyName(value);
                break;
            case FIELD_CONTRACT_TYPE:
                snapshot.setContractType(value);
                break;
            case FIELD_FIRST_CONTRACT_START_TIME:
                snapshot.setFirstContractStartTime(value);
                break;
            case FIELD_FIRST_CONTRACT_END_TIME:
                snapshot.setFirstContractEndTime(value);
                break;
            case FIELD_NOW_CONTRACT_START_TIME:
                snapshot.setNowContractStartTime(value);
                break;
            case FIELD_NOW_CONTRACT_END_TIME:
                snapshot.setNowContractEndTime(value);
                break;
            case FIELD_CONTRACT_PERIOD_TYPE:
                snapshot.setContractPeriodType(value);
                break;
            case FIELD_CONTRACT_RENEW_COUNT:
                snapshot.setContractRenewCount(value);
                break;
            case FIELD_URGENT_CONTACTS_NAME:
                snapshot.setUrgentContactsName(value);
                break;
            case FIELD_URGENT_CONTACTS_RELATION:
                snapshot.setUrgentContactsRelation(value);
                break;
            case FIELD_URGENT_CONTACTS_PHONE:
                snapshot.setUrgentContactsPhone(value);
                break;
            case FIELD_HAVE_CHILD:
                snapshot.setHaveChild(value);
                break;
            case FIELD_CHILD_NAME:
                snapshot.setChildName(value);
                break;
            case FIELD_CHILD_SEX:
                snapshot.setChildSex(value);
                break;
            case FIELD_CHILD_BIRTH_DATE:
                snapshot.setChildBirthDate(value);
                break;
            default:
                break;
        }
    }

    private String fieldValue(JsonNode fieldNode) {
        JsonNode valueList = firstArray(fieldNode, "fieldValueList", "field_value_list", "values", "valueList");
        if (valueList != null) {
            List<String> values = new ArrayList<>();
            for (JsonNode item : valueList) {
                String value = text(item, "label", "value", "text", "name");
                if (StringUtils.hasText(value)) {
                    values.add(value);
                }
            }
            return values.isEmpty() ? null : String.join(",", values);
        }
        return text(fieldNode, "fieldValue", "field_value", "value", "label");
    }

    private JsonNode resolveRows(JsonNode root) {
        if (root == null || root.isMissingNode() || root.isNull()) {
            return com.fasterxml.jackson.databind.node.MissingNode.getInstance();
        }
        JsonNode result = root.path("result");
        if (result.isArray()) {
            return result;
        }
        JsonNode rows = firstArray(result, "list", "data", "rows", "result");
        if (rows != null) {
            return rows;
        }
        rows = firstArray(root, "list", "data", "rows");
        return rows == null ? com.fasterxml.jackson.databind.node.MissingNode.getInstance() : rows;
    }

    private JsonNode firstArray(JsonNode node, String... names) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        for (String name : names) {
            JsonNode value = node.path(name);
            if (value.isArray()) {
                return value;
            }
        }
        return null;
    }

    private List<String> normalizeUserIds(List<String> userIds) {
        List<String> result = new ArrayList<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }
        for (String userId : userIds) {
            String normalized = trim(userId);
            if (StringUtils.hasText(normalized) && !result.contains(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }

    private Object resolveAppAgentId() {
        String agentId = trim(dingTalkProperties.getApp() == null ? null : dingTalkProperties.getApp().getAgentId());
        if (!StringUtils.hasText(agentId)) {
            throw new IllegalStateException("Missing dingtalk.app.agent-id");
        }
        try {
            return Long.valueOf(agentId);
        } catch (NumberFormatException ignored) {
            return agentId;
        }
    }

    private long safeRosterBatchIntervalMs() {
        Long value = dingTalkProperties.getSync() == null ? null : dingTalkProperties.getSync().getRosterBatchIntervalMs();
        return value == null ? 0L : Math.max(value, 0L);
    }

    private void sleepBetweenBatches(long intervalMs) {
        if (intervalMs <= 0L) {
            return;
        }
        try {
            TimeUnit.MILLISECONDS.sleep(intervalMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private String text(JsonNode node, String... names) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        for (String name : names) {
            JsonNode value = node.path(name);
            if (!value.isMissingNode() && !value.isNull()) {
                String text = value.asText("");
                if (StringUtils.hasText(text)) {
                    return text.trim();
                }
            }
        }
        return null;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

}
