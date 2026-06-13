#!/usr/bin/env bash
set -uo pipefail

ROOT="${1:-$(pwd)}"
STRICT="${STRICT:-0}"
BACKEND="$ROOT/backend/src"
FRONTEND="$ROOT/frontend/src"

pass_count=0
warn_count=0
fail_count=0

pass() { printf '[PASS] %s\n' "$1"; pass_count=$((pass_count + 1)); }
warn() { printf '[WARN] %s\n' "$1"; warn_count=$((warn_count + 1)); }
fail() { printf '[FAIL] %s\n' "$1"; fail_count=$((fail_count + 1)); }
legacy() {
  if [ "$STRICT" = "1" ]; then
    fail "$1"
  else
    warn "$1"
  fi
}

print_block() {
  title="$1"
  body="$2"
  if [ -n "$body" ]; then
    printf '%s\n%s\n' "$title" "$body"
  fi
}

if [ ! -d "$ROOT" ]; then
  fail "项目根目录不存在：$ROOT"
  exit 1
fi

[ -f "$ROOT/DIRECTORY_LAYERING_GUIDE.md" ] && pass "存在 DIRECTORY_LAYERING_GUIDE.md" || fail "缺少 DIRECTORY_LAYERING_GUIDE.md"
[ -f "$ROOT/AGENTS.md" ] && pass "存在 AGENTS.md" || fail "缺少 AGENTS.md"

if [ ! -d "$BACKEND" ] || [ ! -d "$FRONTEND" ]; then
  fail "缺少 backend/src 或 frontend/src"
  exit 1
fi

printf '%s\n' '--- shared 拆分目标目录 ---'
missing_foundation=""
for dir in platform auth db bootstrap domain media promotion; do
  [ -d "$BACKEND/$dir" ] || missing_foundation="$missing_foundation$BACKEND/$dir\n"
done
if [ -z "$missing_foundation" ]; then
  pass "backend/src/shared 已拆分到 platform/auth/db/bootstrap/domain/media/promotion 等语义目录"
else
  fail "缺少 shared 拆分后的目标目录"
  printf '%s' "$missing_foundation"
fi

shared_scala=$(find "$BACKEND/shared" -type f -name '*.scala' 2>/dev/null | sort || true)
if [ -z "$shared_scala" ]; then
  pass "backend/src/shared 下无 Scala 实现文件"
else
  legacy "backend/src/shared 仍存在 Scala 实现文件，应迁移到明确目录"
  print_block "涉及文件：" "$shared_scala"
fi

printf '%s\n' '--- API 层支撑文件 ---'
api_support=$(find "$BACKEND" -path '*/api/*APIMessageSupport.scala' -type f 2>/dev/null | sort || true)
any_support=$(find "$BACKEND" -name '*APIMessageSupport.scala' -type f 2>/dev/null | sort || true)
if [ -z "$api_support" ] && [ -z "$any_support" ]; then
  pass "无 *APIMessageSupport.scala 模糊支撑文件"
else
  legacy "存在 *APIMessageSupport.scala，应拆入 services/validators/utils/media"
  print_block "涉及文件：" "$any_support"
fi

printf '%s\n' '--- 媒体模块 ---'
media_missing=""
for file in \
  "$BACKEND/media/routes/StoredImageRoutes.scala" \
  "$BACKEND/media/services/StoredImageService.scala" \
  "$BACKEND/media/validators/ImageUploadValidator.scala" \
  "$BACKEND/media/objects/StoredImage.scala" \
  "$BACKEND/media/tables/storedimage/StoredImageTable.scala" \
  "$BACKEND/media/tables/storedimage/StoredImageMigration.scala" \
  "$BACKEND/media/tables/storedimage/StoredImageTableInitializer.scala"; do
  [ -f "$file" ] || media_missing="$media_missing$file\n"
done
if [ -z "$media_missing" ]; then
  pass "StoredImage 媒体能力已归入 backend/src/media"
else
  fail "media 模块缺少必要文件"
  printf '%s' "$media_missing"
fi

printf '%s\n' '--- 前端 store 内聚性 ---'
page_stores=$(find "$FRONTEND/stores/pages" -type f \( -name '*.ts' -o -name '*.tsx' \) 2>/dev/null | sort || true)
if [ -z "$page_stores" ]; then
  pass "frontend/src/stores/pages 无页面私有 store"
else
  legacy "页面私有 store 仍集中在 frontend/src/stores/pages，建议迁移到 pages/{Page}/stores"
  count=$(printf '%s\n' "$page_stores" | sed '/^$/d' | wc -l | tr -d ' ')
  printf '涉及文件数量：%s\n' "$count"
fi

printf '%s\n' '--- objects 文件复杂度 ---'
large_object_files=""
while IFS= read -r file; do
  [ -z "$file" ] && continue
  count=$(grep -E '^[[:space:]]*(final[[:space:]]+)?case[[:space:]]+class[[:space:]]+' "$file" 2>/dev/null | wc -l | tr -d ' ')
  if [ "$count" -gt 3 ]; then
    large_object_files="$large_object_files$file ($count case classes)\n"
  fi
done <<EOF
$(find "$BACKEND" -path '*/objects/*.scala' -type f 2>/dev/null | sort || true)
EOF
if [ -z "$large_object_files" ]; then
  pass "未发现明显堆叠过多 case class 的 objects 根文件"
else
  legacy "部分 objects 根文件包含较多 case class，建议按领域对象拆分"
  printf '%s' "$large_object_files"
fi

printf '%s\n' '--- JSON codec 可发现性 ---'
codec_file="$BACKEND/platform/json/ApiJsonCodecs.scala"
missing_codec_modules=""
for module in admin ai merchant order review rider user promotion; do
  [ -d "$BACKEND/$module/json" ] || missing_codec_modules="$missing_codec_modules$module: 缺少 json/\n"
done
if [ ! -f "$codec_file" ]; then
  fail "缺少 platform/json/ApiJsonCodecs.scala 聚合入口"
elif [ -z "$missing_codec_modules" ]; then
  pass "存在 platform/json 聚合入口与各模块 json/ codec 入口"
  lines=$(wc -l < "$codec_file" | tr -d ' ')
  if [ "$lines" -gt 500 ]; then
    legacy "ApiJsonCodecs.scala 已超过 500 行，建议继续把具体 codec 下沉到模块文件"
  fi
else
  legacy "部分模块缺少 json/ codec 入口"
  printf '%s' "$missing_codec_modules"
fi

printf '%s\n' '--- service / validator 层 ---'
missing_layers=""
for module in admin ai merchant order review rider user; do
  if [ -d "$BACKEND/$module" ]; then
    [ -d "$BACKEND/$module/services" ] || missing_layers="$missing_layers$module: 缺少 services/\n"
    [ -d "$BACKEND/$module/validators" ] || missing_layers="$missing_layers$module: 缺少 validators/\n"
  fi
done
if [ -z "$missing_layers" ]; then
  pass "主要模块均存在 services/ 与 validators/"
else
  legacy "部分模块尚未形成 services/validators 分层，可在触碰相关模块时渐进补齐"
  printf '%s' "$missing_layers"
fi

printf '\nSummary: %s pass, %s warn, %s fail\n' "$pass_count" "$warn_count" "$fail_count"
if [ "$fail_count" -gt 0 ]; then
  exit 1
fi
exit 0
