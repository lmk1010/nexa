from pathlib import Path

# Wire Flutter route
main = Path("E:/code/nexa/apps/mobile/lib/main.dart")
t = main.read_text(encoding="utf-8")
if "tenant_onboarding_page.dart" not in t:
    # insert import after material
    if "import 'package:flutter/material.dart';" in t:
        t = t.replace(
            "import 'package:flutter/material.dart';",
            "import 'package:flutter/material.dart';\nimport 'pages/tenant_onboarding_page.dart';",
            1,
        )
    else:
        t = "import 'pages/tenant_onboarding_page.dart';\n" + t
if "/onboarding" not in t:
    t = t.replace(
        "'/debug': (context) => const DebugPage(),",
        "'/debug': (context) => const DebugPage(),\n"
        "                '/onboarding': (context) => const TenantOnboardingPage(),",
    )
main.write_text(t, encoding="utf-8")
print("main wired")

# Login page: add entry to onboarding if possible
login = Path("E:/code/nexa/apps/mobile/lib/pages/login_page.dart")
lt = login.read_text(encoding="utf-8")
if "TenantOnboardingPage" not in lt and "/onboarding" not in lt:
    # add import
    if "import '" in lt:
        # after first relative import block - append near top after package imports
        lt = lt.replace(
            "import 'package:flutter/material.dart';",
            "import 'package:flutter/material.dart';\nimport 'tenant_onboarding_page.dart';",
            1,
        )
    # inject a text button near the disclaimer line
    needle = "'账号由企业管理员统一开通，登录即表示同意企业账号和数据安全规范'"
    if needle in lt:
        # find a nearby widget area is hard; add floating note via simple replacement of that string widget if Text(
        pass
    # simpler: append helper method and document route; try replace AppBar or body with link
    # Add at end of class a documented route usage - better inject TextButton after password field region
    if "开通/加入企业" not in lt:
        # insert before last closing of build if we find a unique SizedBox near bottom actions
        marker = "账号由企业管理员统一开通"
        idx = lt.find(marker)
        if idx > 0:
            # find Text( containing it and after that widget add button - too fragile
            # inject after first 'child: Text(' near marker line by adding sibling via comment
            lt = lt.replace(
                marker,
                marker
                + "',), TextButton(onPressed: () => Navigator.of(context).pushNamed('/onboarding'), child: const Text('开通/加入企业",
            )
            # This may break syntax - check
            # Actually above corrupts string. Do safer approach:
print("login inspect later")

# Safer login: rewrite the bad replace if corrupted
lt = login.read_text(encoding="utf-8")
if "开通/加入企业" in lt and "TextButton" in lt and lt.count("开通/加入企业") and "pushNamed('/onboarding')" in lt:
    # verify parse - if broken fix
    if "登录即表示同意企业账号和数据安全规范'," in lt and "TextButton" in lt[lt.find("登录即表示"):lt.find("登录即表示")+200]:
        # likely broken string - restore and use different inject
        lt = login.read_text(encoding="utf-8")
# re-read original from git? use clean inject method
lt = Path("E:/code/nexa/apps/mobile/lib/pages/login_page.dart").read_text(encoding="utf-8")
# undo corruption if any
if "开通/加入企业" in lt and "登录即表示同意企业账号和数据安全规范'," in lt:
    # corrupted - get clean from pattern
    import re
    lt = re.sub(
        r"账号由企业管理员统一开通，登录即表示同意企业账号和数据安全规范'.*?开通/加入企业",
        "账号由企业管理员统一开通，登录即表示同意企业账号和数据安全规范",
        lt,
        count=1,
        flags=re.S,
    )
    print("login uncorrupt attempt")

if "tenant_onboarding_page.dart" not in lt:
    lt = lt.replace(
        "import 'package:flutter/material.dart';",
        "import 'package:flutter/material.dart';\nimport 'tenant_onboarding_page.dart';",
        1,
    )

# Find Scaffold body and prepend a banner button using a unique import-only approach:
# Add route-only; document Navigator.pushNamed('/onboarding')
# Inject TextButton in AppBar actions if AppBar exists
if "开通/加入企业" not in lt:
    if "appBar: AppBar(" in lt and "actions:" not in lt[lt.find("appBar: AppBar(") : lt.find("appBar: AppBar(") + 300]:
        lt = lt.replace(
            "appBar: AppBar(",
            "appBar: AppBar(\n        actions: [\n          TextButton(\n            onPressed: () => Navigator.of(context).pushNamed('/onboarding'),\n            child: const Text('开通企业'),\n          ),\n        ],",
            1,
        )
        print("appbar action added")
    elif "AppBar(" in lt:
        # AppBar may already have actions
        idx = lt.find("AppBar(")
        chunk = lt[idx : idx + 400]
        if "actions:" in chunk:
            lt = lt.replace(
                "actions: [",
                "actions: [\n          TextButton(\n            onPressed: () => Navigator.of(context).pushNamed('/onboarding'),\n            child: const Text('开通企业'),\n          ),",
                1,
            )
            print("appbar actions prepend")
        else:
            print("no appbar actions inject point")
    else:
        print("no AppBar")

login.write_text(lt, encoding="utf-8")
print("login updated")
