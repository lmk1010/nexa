package com.kyx.service.hr.integration.dingtalk.service;

import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;
import com.kyx.service.hr.dal.dataobject.integration.DingTalkUserBindingDO;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import com.kyx.service.hr.dal.mysql.integration.DingTalkUserBindingMapper;
import com.kyx.service.hr.integration.dingtalk.model.DingTalkUserSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Mobile-based user binding implementation.
 *
 * Mapping is persisted into local table for stream realtime path.
 */
@Service
@Slf4j
public class MobileDingTalkUserBindingService implements DingTalkUserBindingService {

    private static final String MATCH_TYPE_MOBILE = "MOBILE";
    private static final String MATCH_TYPE_NAME = "NAME";
    private static final String SOURCE_TYPE_AUTO = "AUTO_SYNC";

    @Resource
    private DingTalkDirectoryService dingTalkDirectoryService;
    @Resource
    private EmployeeProfileMapper employeeProfileMapper;
    @Resource
    private DingTalkUserBindingMapper dingTalkUserBindingMapper;

    private volatile Map<String, Long> cachedMapping = Collections.emptyMap();

    @PostConstruct
    public void initCache() {
        reloadCacheFromLocalTable();
    }

    @Override
    public Map<String, Long> buildDingUserToOaUserMap() {
        return refreshDingUserToOaUserMap();
    }

    @Override
    public Map<String, Long> getCachedDingUserToOaUserMap() {
        if (cachedMapping.isEmpty()) {
            reloadCacheFromLocalTable();
        }
        return cachedMapping;
    }

    @Override
    public Map<String, Long> refreshDingUserToOaUserMap() {
        List<DingTalkUserSnapshot> dingUsers = dingTalkDirectoryService.listAllUsers();
        boolean hasIdentity = false;
        for (DingTalkUserSnapshot dingUser : dingUsers) {
            String mobile = normalizeMobile(dingUser.getMobile());
            if (StringUtils.hasText(mobile)) {
                hasIdentity = true;
            }
            String name = normalizeName(dingUser.getName());
            if (StringUtils.hasText(name)) {
                hasIdentity = true;
            }
        }
        if (!hasIdentity) {
            return getCachedDingUserToOaUserMap();
        }

        List<EmployeeProfileDO> profiles = employeeProfileMapper.selectList(new LambdaQueryWrapperX<>());
        Map<String, Long> oaUserByMobile = buildUniqueKeyMap(profiles, profile -> normalizeMobile(profile.getMobile()));
        Map<String, Long> oaUserByName = buildUniqueKeyMap(profiles, profile -> normalizeName(profile.getName()));
        Map<Long, EmployeeProfileDO> profileByUserId = buildProfileByUserId(profiles);

        Map<String, Long> mapping = new HashMap<>();
        Map<String, String> matchTypeByDingUserId = new HashMap<>();
        Set<Long> usedOaUserIds = new HashSet<>();
        int matchedByMobile = 0;
        int matchedByName = 0;
        int unmatched = 0;
        for (DingTalkUserSnapshot dingUser : dingUsers) {
            String dingUserId = normalizeDingUserId(dingUser.getUserId());
            if (!StringUtils.hasText(dingUserId)) {
                continue;
            }
            Long oaUserId = oaUserByMobile.get(normalizeMobile(dingUser.getMobile()));
            String matchType = MATCH_TYPE_MOBILE;
            if (oaUserId == null) {
                oaUserId = oaUserByName.get(normalizeName(dingUser.getName()));
                matchType = MATCH_TYPE_NAME;
            }
            if (oaUserId != null) {
                if (!usedOaUserIds.add(oaUserId)) {
                    unmatched++;
                    continue;
                }
                mapping.put(dingUserId, oaUserId);
                matchTypeByDingUserId.put(dingUserId, matchType);
                if (MATCH_TYPE_NAME.equals(matchType)) {
                    matchedByName++;
                } else {
                    matchedByMobile++;
                }
            } else {
                unmatched++;
            }
        }

        persistLocalBindings(mapping, matchTypeByDingUserId, dingUsers, profileByUserId);
        cachedMapping = Collections.unmodifiableMap(new HashMap<>(mapping));
        log.info("DingTalk user binding built: totalDingUsers={}, mapped={}, byMobile={}, byName={}, unmatched={}",
                dingUsers.size(), mapping.size(), matchedByMobile, matchedByName, unmatched);
        return cachedMapping;
    }

    @Override
    public Map<String, Long> refreshCacheFromLocalBindings() {
        return reloadCacheFromLocalTable();
    }

    @Override
    public String findDingUserIdByOaUserId(Long oaUserId) {
        if (oaUserId == null) {
            return null;
        }
        DingTalkUserBindingDO binding = dingTalkUserBindingMapper.selectByOaUserId(oaUserId);
        if (binding != null && StringUtils.hasText(binding.getDingUserId())) {
            return binding.getDingUserId().trim();
        }
        for (Map.Entry<String, Long> entry : getCachedDingUserToOaUserMap().entrySet()) {
            if (oaUserId.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void persistLocalBindings(Map<String, Long> mapping,
                                      Map<String, String> matchTypeByDingUserId,
                                      List<DingTalkUserSnapshot> dingUsers,
                                      Map<Long, EmployeeProfileDO> profileByUserId) {
        if (mapping.isEmpty()) {
            return;
        }
        Map<String, DingTalkUserSnapshot> dingUserById = new HashMap<>();
        for (DingTalkUserSnapshot dingUser : dingUsers) {
            String dingUserId = normalizeDingUserId(dingUser.getUserId());
            if (StringUtils.hasText(dingUserId)) {
                dingUserById.put(dingUserId, dingUser);
            }
        }

        List<DingTalkUserBindingDO> existedRows = dingTalkUserBindingMapper.selectListByDingUserIds(mapping.keySet());
        Map<String, DingTalkUserBindingDO> existedByDingUserId = new HashMap<>();
        for (DingTalkUserBindingDO row : existedRows) {
            if (row != null && StringUtils.hasText(row.getDingUserId())) {
                existedByDingUserId.put(row.getDingUserId().trim(), row);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        List<DingTalkUserBindingDO> inserts = new java.util.ArrayList<>();
        List<DingTalkUserBindingDO> updates = new java.util.ArrayList<>();
        for (Map.Entry<String, Long> entry : mapping.entrySet()) {
            String dingUserId = entry.getKey();
            Long oaUserId = entry.getValue();
            DingTalkUserBindingDO entity = existedByDingUserId.get(dingUserId);
            if (entity == null) {
                entity = new DingTalkUserBindingDO();
                entity.setDingUserId(dingUserId);
            }
            DingTalkUserSnapshot snapshot = dingUserById.get(dingUserId);
            EmployeeProfileDO profile = profileByUserId.get(oaUserId);

            entity.setOaUserId(oaUserId);
            entity.setProfileId(profile == null ? null : profile.getId());
            entity.setDingUserName(snapshot == null ? null : snapshot.getName());
            entity.setDingMobile(normalizeMobile(snapshot == null ? null : snapshot.getMobile()));
            entity.setDingEmail(snapshot == null ? null : snapshot.getEmail());
            entity.setDingDeptId(snapshot == null ? null : snapshot.getDeptId());
            entity.setDingActive(snapshot == null ? null : snapshot.getActive());
            entity.setMatchType(matchTypeByDingUserId.get(dingUserId));
            entity.setSourceType(SOURCE_TYPE_AUTO);
            entity.setLastSeenTime(now);
            entity.setSyncTime(now);
            entity.setRawPayload(snapshot == null ? null : snapshot.getRawPayload());
            if (entity.getId() == null) {
                inserts.add(entity);
            } else {
                updates.add(entity);
            }
        }
        if (!inserts.isEmpty()) {
            dingTalkUserBindingMapper.insertBatch(inserts);
        }
        if (!updates.isEmpty()) {
            dingTalkUserBindingMapper.updateBatch(updates);
        }
    }

    private Map<Long, EmployeeProfileDO> buildProfileByUserId(List<EmployeeProfileDO> profiles) {
        Map<Long, EmployeeProfileDO> result = new HashMap<>();
        for (EmployeeProfileDO profile : profiles) {
            if (profile != null && profile.getUserId() != null) {
                result.put(profile.getUserId(), profile);
            }
        }
        return result;
    }

    private Map<String, Long> reloadCacheFromLocalTable() {
        List<DingTalkUserBindingDO> rows = dingTalkUserBindingMapper.selectListAll();
        Map<String, Long> mapping = new HashMap<>();
        for (DingTalkUserBindingDO row : rows) {
            String dingUserId = normalizeDingUserId(row.getDingUserId());
            if (StringUtils.hasText(dingUserId) && row.getOaUserId() != null) {
                mapping.put(dingUserId, row.getOaUserId());
            }
        }
        cachedMapping = Collections.unmodifiableMap(mapping);
        return cachedMapping;
    }

    private Map<String, Long> buildUniqueKeyMap(List<EmployeeProfileDO> profiles, Function<EmployeeProfileDO, String> keyExtractor) {
        Map<String, Long> uniqueMap = new HashMap<>();
        Set<String> duplicatedKeys = new HashSet<>();
        for (EmployeeProfileDO profile : profiles) {
            if (profile == null || profile.getUserId() == null) {
                continue;
            }
            String key = keyExtractor.apply(profile);
            if (!StringUtils.hasText(key)) {
                continue;
            }
            Long existing = uniqueMap.putIfAbsent(key, profile.getUserId());
            if (existing != null && !existing.equals(profile.getUserId())) {
                duplicatedKeys.add(key);
            }
        }
        duplicatedKeys.forEach(uniqueMap::remove);
        return uniqueMap;
    }

    private String normalizeDingUserId(String dingUserId) {
        return dingUserId == null ? null : dingUserId.trim();
    }

    private String normalizeMobile(String mobile) {
        if (mobile == null) {
            return null;
        }
        String normalized = mobile.trim()
                .replaceAll("[\\s-]", "")
                .replace("(", "")
                .replace(")", "");
        if (normalized.startsWith("+86")) {
            normalized = normalized.substring(3);
        } else if (normalized.startsWith("86") && normalized.length() > 11) {
            normalized = normalized.substring(2);
        }
        return normalized;
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim().replaceAll("\\s+", "");
    }

}
