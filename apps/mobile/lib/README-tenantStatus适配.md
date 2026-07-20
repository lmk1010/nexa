# Flutter应用tenantStatus字段适配说明

## 修改概述

为了适配后端新增的`tenantStatus`字段，对Flutter应用进行了以下修改：

## 后端修改

### 1. 数据库查询修改
- 修改了`InviteCodeMapper.xml`中的所有查询，添加了`t.status as tenantStatus`字段
- 包括以下查询方法：
  - `selectByCodeWithTenant`
  - `selectListByTenantId`
  - `selectListByTenantIdAndType`
  - `selectValidInviteCodes`
  - `selectExpiredInviteCodes`
  - `selectUsedUpInviteCodes`
  - `selectPageByXml`

### 2. 数据对象修改
- `InviteCodeDO.java`: 添加了`tenantStatus`字段
- `InviteCodeValidateRespVO.java`: 添加了`tenantStatus`字段
- `InviteCodeRespVO.java`: 添加了`tenantStatus`字段
- `InviteCodeRespDTO.java`: 添加了`tenantStatus`字段

### 3. 服务层修改
- `InviteCodeServiceImpl.java`: 在`validateInviteCodeWithDetails`方法中设置`tenantStatus`字段
- `InviteCodeController.java`: 在响应转换中设置`tenantStatus`字段

## Flutter应用修改

### 1. 邀请码验证逻辑增强
在`company_selection_page.dart`的`_validateInvitationCode`方法中添加了租户状态检查：

```dart
// 检查租户状态
final tenantStatus = companyInfo['tenantStatus'] as int?;
if (tenantStatus != null && tenantStatus == 0) {
  // 租户被禁用，显示错误信息
  final themeService = Provider.of<ThemeService>(context, listen: false);
  final theme = themeService.currentTheme;
  ScaffoldMessenger.of(context).showSnackBar(
    SnackBar(
      content: Text('该公司的邀请码已被禁用，请联系管理员'),
      backgroundColor: theme['error'],
      duration: const Duration(seconds: 3),
    ),
  );
  return;
}
```

### 2. 公司信息显示
在`_buildCompanyInfo`方法中，根据`tenantStatus`显示不同的状态：

```dart
Container(
  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
  decoration: BoxDecoration(
    color: _companyInfo!['tenantStatus'] == 1 
        ? theme['success']!.withOpacity(0.1)
        : theme['error']!.withOpacity(0.1),
    borderRadius: BorderRadius.circular(12),
  ),
  child: Text(
    _companyInfo!['tenantStatus'] == 1 ? '正常' : '禁用',
    style: TextStyle(
      fontSize: 12,
      fontWeight: FontWeight.w500,
      color: _companyInfo!['tenantStatus'] == 1 
          ? theme['success']
          : theme['error'],
    ),
  ),
),
```

## 状态说明

### 邀请码状态 (status)
- `0`: 启用 - 邀请码可以正常使用
- `1`: 禁用 - 邀请码被禁用
- `2`: 已过期 - 邀请码已过期
- `3`: 已用完 - 邀请码使用次数已达上限

### 租户状态 (tenantStatus)
- `0`: 正常 - 租户可以正常使用邀请码
- `1`: 禁用 - 租户被禁用，无法使用邀请码

## 用户体验改进

### 1. 错误提示
当用户输入无效邀请码时，会显示相应的错误提示：
- 邀请码已被禁用："邀请码已被禁用"
- 邀请码已过期："邀请码已过期"
- 邀请码已用完："邀请码已用完"
- 租户被禁用："该公司的邀请码已被禁用，请联系管理员"

### 2. 状态显示
在公司信息卡片中，会显示邀请码状态和租户状态：
- 邀请码状态：根据status字段显示"启用"、"禁用"、"已过期"、"已用完"
- 租户状态：根据tenantStatus字段显示"正常"（0）或"禁用"（1）
- 正常状态：绿色背景
- 异常状态：红色背景

### 3. 验证流程
1. 用户输入邀请码
2. 系统验证邀请码有效性
3. 检查邀请码状态（status字段）
4. 如果邀请码无效（禁用、过期、已用完），显示相应错误信息并阻止继续
5. 检查租户状态（tenantStatus字段）
6. 如果租户被禁用，显示错误信息并阻止继续
7. 如果所有检查都通过，显示公司信息并允许继续

## 兼容性说明

- 修改向后兼容，不会影响现有功能
- 如果后端没有返回`tenantStatus`字段，前端会正常处理（显示为null）
- 所有现有的邀请码验证逻辑保持不变

## 测试建议

1. 测试正常租户的邀请码验证
2. 测试禁用租户的邀请码验证
3. 测试邀请码不存在的情况
4. 测试网络错误的情况
5. 验证UI显示是否正确 