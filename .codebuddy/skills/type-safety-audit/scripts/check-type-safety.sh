#!/usr/bin/env bash
# Type-Safe Project 类型安全自动检查脚本
# 用法: bash check-type-safety.sh [项目根目录]

set -uo pipefail

ROOT="${1:-.}"
BACKEND="$ROOT/backend/src"
FRONTEND="$ROOT/frontend/src"
MODULES=(ai user merchant order rider)
OBJECT_MODULES=(ai user merchant order rider)

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

PASS=0
FAIL=0

pass() { echo -e "${GREEN}✅ PASS${NC}: $1"; ((PASS++)); }
fail() { echo -e "${RED}❌ FAIL${NC}: $1"; ((FAIL++)); }
warn() { echo -e "${YELLOW}⚠️  WARN${NC}: $1"; }

names_in_dir() {
  local dir="$1"
  local ext="$2"
  if [ -d "$dir" ]; then
    find "$dir" -maxdepth 1 -type f -name "*$ext" -print | sed "s#^.*/##" | sed "s#$ext\$##" | sort
  fi
}

compare_name_sets() {
  local title="$1"
  local left_label="$2"
  local right_label="$3"
  local left_names="$4"
  local right_names="$5"
  local missing=0

  for name in $left_names; do
    if ! echo "$right_names" | grep -qx "$name"; then
      fail "$title: $left_label/$name 在 $right_label 中无对应"
      missing=1
    fi
  done
  for name in $right_names; do
    if ! echo "$left_names" | grep -qx "$name"; then
      fail "$title: $right_label/$name 在 $left_label 中无对应"
      missing=1
    fi
  done
  if [ "$missing" -eq 0 ]; then
    pass "$title: 文件名一一对应"
  fi
}

api_class_count() {
  grep -E "class .*extends APIMessage" "$1" 2>/dev/null | wc -l | tr -d '[:space:]'
}

case_class_count() {
  grep -E "final case class .*APIMessage" "$1" 2>/dev/null | wc -l | tr -d '[:space:]'
}

echo "========================================="
echo " Type-Safe Project 类型安全检查"
echo "========================================="
echo ""

# 检查 1: API 路径与一文件一消息
echo "--- 检查 1: API 一文件一消息 ---"

if [ -d "$FRONTEND/api" ]; then
  fail "前端残留 frontend/src/api，应迁移到 frontend/src/apis"
else
  pass "前端无 frontend/src/api 残留"
fi

legacy_backend=$(find "$BACKEND" -path '*/api/*APIMessages.scala' -type f 2>/dev/null || true)
legacy_frontend=$(find "$FRONTEND" -path '*/apis/*/*APIMessages.ts' -type f 2>/dev/null || true)
legacy_ts_api=$(find "$FRONTEND/apis" -path '*/shared/*' -prune -o -name '*Api.ts' -type f -print 2>/dev/null || true)

[ -z "$legacy_backend" ] && pass "后端无 *APIMessages.scala 聚合文件" || { fail "后端残留聚合 API 文件"; echo "$legacy_backend"; }
[ -z "$legacy_frontend" ] && pass "前端无 *APIMessages.ts 桶文件" || { fail "前端残留 APIMessages 桶文件"; echo "$legacy_frontend"; }
[ -z "$legacy_ts_api" ] && pass "前端业务 API 文件均使用 *API.ts" || { fail "前端残留 *Api.ts 文件"; echo "$legacy_ts_api"; }

for module in "${MODULES[@]}"; do
  be_dir="$BACKEND/$module/api"
  fe_dir="$FRONTEND/apis/$module"
  if [ ! -d "$be_dir" ] || [ ! -d "$fe_dir" ]; then
    fail "$module: API 目录缺失"
    continue
  fi

  be_names=$(find "$be_dir" -maxdepth 1 -type f -name '*APIMessage.scala' -print | sed 's#^.*/##' | sed 's#.scala$##' | grep -v 'Support$' | sort || true)
  fe_names=$(find "$fe_dir" -maxdepth 1 -type f -name '*API.ts' -print | sed 's#^.*/##' | sed 's#.ts$##' | sort || true)
  be_as_fe=$(echo "$be_names" | sed 's/APIMessage$/API/' | sort)
  compare_name_sets "$module API" "backend" "frontend" "$be_as_fe" "$fe_names"

  for name in $be_names; do
    file="$be_dir/$name.scala"
    count=$(case_class_count "$file")
    if [ "$count" -ne 1 ]; then
      fail "$file 应只定义 1 个 APIMessage，实际 $count 个"
    elif ! grep -q "final case class $name" "$file"; then
      fail "$file 文件名与 case class 不一致"
    fi
  done
  for name in $fe_names; do
    file="$fe_dir/$name.ts"
    count=$(api_class_count "$file")
    if [ "$count" -ne 1 ]; then
      fail "$file 应只定义 1 个 API class，实际 $count 个"
    elif ! grep -q "class $name extends APIMessage" "$file"; then
      fail "$file 文件名与 class 不一致"
    fi
  done
done

echo ""

# 检查 2: objects 与 apiTypes 对齐
echo "--- 检查 2: Objects / apiTypes 分层对应 ---"

for module in "${OBJECT_MODULES[@]}"; do
  be_root="$BACKEND/$module/objects"
  fe_root="$FRONTEND/objects/$module"
  be_api="$be_root/apiTypes"
  fe_api="$fe_root/apiTypes"

  if [ ! -d "$be_root" ] || [ ! -d "$fe_root" ]; then
    fail "$module: objects 目录缺失"
    continue
  fi

  be_root_names=$(names_in_dir "$be_root" ".scala")
  fe_root_names=$(names_in_dir "$fe_root" ".ts")
  compare_name_sets "$module root objects" "backend" "frontend" "$be_root_names" "$fe_root_names"

  be_api_names=$(names_in_dir "$be_api" ".scala")
  fe_api_names=$(names_in_dir "$fe_api" ".ts")
  compare_name_sets "$module apiTypes" "backend" "frontend" "$be_api_names" "$fe_api_names"

  misplaced_be=$(find "$be_root" -maxdepth 1 -type f \( -name '*Request.scala' -o -name '*Response.scala' \) -print 2>/dev/null || true)
  misplaced_fe=$(find "$fe_root" -maxdepth 1 -type f \( -name '*Request.ts' -o -name '*Response.ts' \) -print 2>/dev/null || true)
  [ -z "$misplaced_be$misplaced_fe" ] && pass "$module: Request/Response 均位于 apiTypes" || { fail "$module: Request/Response 位于 objects 根目录"; echo "$misplaced_be$misplaced_fe"; }

  if [ -f "$be_root/index.scala" ] || [ -f "$fe_root/index.ts" ]; then
    fail "$module: objects 目录存在 index barrel 文件"
  fi
done

# shared 前端契约对应后端拆分后的语义目录
if [ -f "$FRONTEND/objects/shared/ids.ts" ] && [ -f "$BACKEND/domain/ids.scala" ]; then
  pass "shared ids: 前端 shared/ids.ts 对应后端 domain/ids.scala"
else
  fail "shared ids: 缺少前端 shared/ids.ts 或后端 domain/ids.scala"
fi
if [ -f "$FRONTEND/objects/shared/Promotion.ts" ] && [ -f "$BACKEND/promotion/objects/Promotion.scala" ]; then
  pass "Promotion: 前端 shared/Promotion.ts 对应后端 promotion/objects/Promotion.scala"
else
  fail "Promotion: 缺少前端 Promotion.ts 或后端 promotion Promotion.scala"
fi
if [ -f "$FRONTEND/objects/shared/Voucher.ts" ] && [ -f "$BACKEND/promotion/objects/Voucher.scala" ]; then
  pass "Voucher: 前端 shared/Voucher.ts 对应后端 promotion/objects/Voucher.scala"
else
  fail "Voucher: 缺少前端 Voucher.ts 或后端 promotion Voucher.scala"
fi
if [ -f "$FRONTEND/objects/shared/apiTypes/OkResponse.ts" ] && [ -f "$BACKEND/domain/apiTypes/OkResponse.scala" ]; then
  pass "OkResponse: 前端 shared/apiTypes 对应后端 domain/apiTypes"
else
  fail "OkResponse: 缺少前端 shared/apiTypes 或后端 domain/apiTypes"
fi

shared_backend_scala=$(find "$BACKEND/shared" -type f -name '*.scala' 2>/dev/null | sort || true)
[ -z "$shared_backend_scala" ] && pass "后端 shared 目录无 Scala 实现残留" || { fail "后端 shared 目录仍有 Scala 实现"; echo "$shared_backend_scala"; }

echo ""

# 检查 3: 页面 sample 风格拆分
echo "--- 检查 3: 页面目录拆分 ---"

for page in CustomerPortal MerchantConsole RiderApp Login Register; do
  dir="$FRONTEND/pages/$page"
  if [ ! -f "$dir/index.tsx" ]; then
    fail "$page: 缺少 index.tsx"
    continue
  fi
  root_components=$(find "$dir" -maxdepth 1 -type f ! -name 'index.tsx' \( -name '*.tsx' -o -name '*.ts' \) -print 2>/dev/null || true)
  if [ -n "$root_components" ]; then
    fail "$page: 根目录仍有非 index 文件"
    echo "$root_components"
  else
    pass "$page: 根目录仅保留页面入口"
  fi
  [ -d "$dir/components" ] && pass "$page: 存在 components/" || warn "$page: 暂无 components/"
done

echo ""

# 检查 4: 前端越权状态
echo "--- 检查 4: 前端越权状态 ---"

wallet_patch=$(grep -rn "patchCustomerProfileIO(.*walletBalance\|walletBalance .*+.*patchCustomerProfileIO\|walletBalance .*-.*patchCustomerProfileIO" "$FRONTEND" --include='*.ts' --include='*.tsx' 2>/dev/null | head -5 || true)
[ -z "$wallet_patch" ] && pass "未发现前端本地计算 walletBalance 后写回 profile" || { fail "前端存在 walletBalance 越权写回"; echo "$wallet_patch"; }

if grep -q "walletBalance" "$FRONTEND/objects/user/CustomerProfilePatch.ts" 2>/dev/null; then
  fail "CustomerProfilePatch 包含 walletBalance 字段"
else
  pass "CustomerProfilePatch 不含 walletBalance"
fi

echo ""

# 检查 5: var 使用
echo "--- 检查 5: 后端 var 使用 ---"
var_count=$(grep -rn "\bvar\b " "$BACKEND" --include='*.scala' 2>/dev/null | grep -v "// " | grep -v "def " | wc -l | tr -d '[:space:]')
if [ "${var_count:-0}" -gt 0 ]; then
  fail "后端代码中存在 $var_count 处 var 使用"
else
  pass "后端代码中无 var 使用"
fi

echo ""

# 检查 6: API 路由与旧路径残留
echo "--- 检查 6: 路由与旧路径残留 ---"
string_routes=$(grep -rn "Root / \"" "$BACKEND" --include='*.scala' 2>/dev/null | grep -v "health" | grep -v "store-images" | grep -v "APIMessage" | head -10 || true)
[ -z "$string_routes" ] && pass "后端无非 APIMessage 的业务字符串路由" || { fail "后端存在非 APIMessage 字符串路由"; echo "$string_routes"; }

old_imports=$(grep -rn "@/api/\|frontend/src/api\|src/api" "$FRONTEND" --include='*.ts' --include='*.tsx' 2>/dev/null | head -10 || true)
[ -z "$old_imports" ] && pass "前端无旧 API 路径导入" || { fail "前端存在旧 API 路径导入"; echo "$old_imports"; }

echo ""

# 检查 7: AI API 跨层依赖与前端 API DTO 落位
echo "--- 检查 7: AI API 跨层依赖与前端 API DTO 落位 ---"

ai_api_to_api=$(grep -R "APIMessage().plan(connection)" "$BACKEND/ai/api" --include='*.scala' 2>/dev/null || true)
if [ -z "$ai_api_to_api" ]; then
  pass "AI API 未检测到 API 调 APIMessage"
else
  fail "AI API 存在 API 调 APIMessage 依赖"
  echo "$ai_api_to_api"
fi

routes_generic_auto=$(find "$BACKEND" -path '*/routes/*Routes.scala' -type f -exec grep -l 'io.circe.generic.auto' {} + 2>/dev/null | sort || true)
if [ -z "$routes_generic_auto" ]; then
  pass "所有 routes 已移除 generic.auto 兜底"
else
  fail "routes 仍使用 generic.auto 兜底"
  echo "$routes_generic_auto"
fi

inline_api_dto=$(grep -R -E '^export interface .*?(Request|Response)\b' "$FRONTEND/apis" --include='*.ts' 2>/dev/null | grep -v '/shared/' || true)
if [ -z "$inline_api_dto" ]; then
  pass "前端 API 文件无内联 Request/Response 类型"
else
  fail "前端 API 文件存在内联 Request/Response 类型"
  echo "$inline_api_dto"
fi

echo ""

# 检查 8: Unsafe 组件与逃逸口
echo "--- 检查 8: Unsafe 组件与逃逸口 ---"
frontend_unsafe=$(grep -rnE "Unsafe|Unsfe|dangerouslySetInnerHTML|eval[[:space:]]*\(|new[[:space:]]+Function[[:space:]]*\(|as[[:space:]]+any|@ts-ignore|@ts-expect-error" "$FRONTEND" --include='*.ts' --include='*.tsx' 2>/dev/null | head -20 || true)
backend_unsafe=$(grep -rnE "Unsafe|Unsfe|unsafeRun|unsafeFromString|asInstanceOf" "$BACKEND" --include='*.scala' 2>/dev/null | head -20 || true)
if [ -z "$frontend_unsafe$backend_unsafe" ]; then
  pass "未发现 Unsafe 组件、Unsfe 拼写、危险 DOM/JS 逃逸或后端 unsafe API"
else
  fail "发现 Unsafe 组件或类型安全逃逸口"
  [ -n "$frontend_unsafe" ] && echo "$frontend_unsafe"
  [ -n "$backend_unsafe" ] && echo "$backend_unsafe"
fi

echo ""
echo "========================================="
echo " 检查结果: $PASS 通过, $FAIL 失败"
echo "========================================="

if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
