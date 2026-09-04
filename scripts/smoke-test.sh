#!/usr/bin/env bash
# =============================================================================
# 熵基科技轻量招聘系统 后端冒烟测试
# 覆盖需求文档 §19 的关键验收要点（含 CSRF、撤回与重投、归属权限、快照）
# 用法：bash scripts/smoke-test.sh
# =============================================================================
set -u

BASE="${BASE:-http://127.0.0.1:8080}"
TMP="$(mktemp -d)"
PASS=0
FAIL=0

red()   { printf "\033[31m%s\033[0m\n" "$1"; }
green() { printf "\033[32m%s\033[0m\n" "$1"; }
info()  { printf "\033[36m%s\033[0m\n" "$1"; }

# 断言：实际业务码等于期望码
check() {
  local name="$1" expected="$2" actual="$3"
  if [ "$expected" = "$actual" ]; then
    PASS=$((PASS + 1))
    green "  PASS  $name (code=$actual)"
  else
    FAIL=$((FAIL + 1))
    red   "  FAIL  $name (expected=$expected actual=$actual)"
  fi
}

# 带 CSRF 的请求：$1=cookiejar $2=method $3=path $4=body(可空)
req() {
  local jar="$1" method="$2" path="$3" body="${4:-}"
  local token
  token=$(awk '/XSRF-TOKEN/{print $7}' "$jar" 2>/dev/null | tail -1)
  if [ -z "$body" ]; then
    curl -s -b "$jar" -c "$jar" -X "$method" \
      -H "X-XSRF-TOKEN: ${token}" -H "Content-Type: application/json" \
      "$BASE$path"
  else
    curl -s -b "$jar" -c "$jar" -X "$method" \
      -H "X-XSRF-TOKEN: ${token}" -H "Content-Type: application/json" \
      -d "$body" "$BASE$path"
  fi
}

get() {
  curl -s -b "$1" -c "$1" "$BASE$2"
}

# 提取 JSON 顶层 code
code_of() { echo "$1" | sed -n 's/.*"code":\([0-9-]*\).*/\1/p' | head -1; }
# 提取任意字段（数字或字符串）
field_of() {
  echo "$1" | python3 -c "
import json,sys
try:
    data=json.load(sys.stdin)
except Exception:
    print(''); sys.exit()
cur=data
for key in sys.argv[1].split('.'):
    if isinstance(cur,list):
        try: cur=cur[int(key)]
        except Exception: print(''); sys.exit()
    elif isinstance(cur,dict):
        cur=cur.get(key)
    else:
        print(''); sys.exit()
    if cur is None: print(''); sys.exit()
print(cur)
" "$2" 2>/dev/null
}

init_session() {
  local jar="$1"
  rm -f "$jar"
  curl -s -c "$jar" "$BASE/api/auth/csrf" > /dev/null
}

STAMP=$(date +%s)
CAND_EMAIL="cand${STAMP}@example.com"
HR_EMAIL="hr${STAMP}@zkteco.com"
ADMIN_JAR="$TMP/admin.txt"
CAND_JAR="$TMP/cand.txt"
HR_JAR="$TMP/hr.txt"

info "== 0. 公开接口（未登录可访问，§19.1）=="
R=$(curl -s "$BASE/api/public/job-categories"); check "职位类别字典公开可读" 0 "$(code_of "$R")"
R=$(curl -s "$BASE/api/public/jobs?type=CAMPUS"); check "校招职位列表公开可读" 0 "$(code_of "$R")"
CAMPUS_FIRST=$(field_of "$R" "data.list.0.targetAudience")
[ -n "$CAMPUS_FIRST" ] && check "校招列表返回招聘对象字段" 0 0 || check "校招列表返回招聘对象字段" 0 1
R=$(curl -s "$BASE/api/public/jobs?type=SOCIAL")
JOB_ID=$(field_of "$R" "data.list.0.id")
info "  使用职位 ID=$JOB_ID 作为投递目标"

info "== 1. CSRF 防护（§19.2）=="
init_session "$CAND_JAR"
R=$(curl -s -b "$CAND_JAR" -X POST -H "Content-Type: application/json" \
      -d '{"email":"x@x.com","password":"whatever1"}' "$BASE/api/auth/login")
check "不带 X-XSRF-TOKEN 的 POST 被拒绝" 1006 "$(code_of "$R")"
R=$(curl -s -b "$CAND_JAR" -X POST -H "Content-Type: application/json" \
      -H "X-XSRF-TOKEN: wrong-token" -d '{}' "$BASE/api/auth/login")
check "错误 token 被拒绝" 1006 "$(code_of "$R")"

info "== 2. 注册与登录（§19.2）=="
R=$(req "$CAND_JAR" POST /api/auth/register \
      "{\"name\":\"冒烟候选人\",\"email\":\"$CAND_EMAIL\",\"password\":\"Test@2026\",\"confirmPassword\":\"Test@2026\",\"agreePrivacy\":true}")
check "注册成功并自动登录" 0 "$(code_of "$R")"
check "注册结果角色为求职者" "CANDIDATE" "$(field_of "$R" "data.role")"

R=$(req "$CAND_JAR" POST /api/auth/register \
      "{\"name\":\"重复邮箱\",\"email\":\"$CAND_EMAIL\",\"password\":\"Test@2026\",\"confirmPassword\":\"Test@2026\",\"agreePrivacy\":true}")
check "重复邮箱被拒绝" 2001 "$(code_of "$R")"

init_session "$TMP/tmp.txt"
R=$(req "$TMP/tmp.txt" POST /api/auth/register \
      "{\"name\":\"密码不一致\",\"email\":\"mismatch${STAMP}@example.com\",\"password\":\"Test@2026\",\"confirmPassword\":\"Other@2026\",\"agreePrivacy\":true}")
check "两次密码不一致被拒绝" 2002 "$(code_of "$R")"

R=$(req "$TMP/tmp.txt" POST /api/auth/register \
      "{\"name\":\"未勾选隐私\",\"email\":\"noagree${STAMP}@example.com\",\"password\":\"Test@2026\",\"confirmPassword\":\"Test@2026\",\"agreePrivacy\":false}")
check "未同意隐私政策被拒绝" 1001 "$(code_of "$R")"

info "== 3. 投递前置校验（§19.3）=="
R=$(get "$CAND_JAR" "/api/candidate/jobs/$JOB_ID/apply-eligibility")
check "资格查询可用" 0 "$(code_of "$R")"
check "新账号缺少手机号与简历时不可投递" "False" "$(field_of "$R" "data.canApply")"
R=$(req "$CAND_JAR" POST "/api/candidate/jobs/$JOB_ID/apply" "")
check "资料不完整时投递被拒绝" 3003 "$(code_of "$R")"

R=$(req "$CAND_JAR" PUT /api/candidate/profile \
      "{\"name\":\"冒烟候选人\",\"email\":\"$CAND_EMAIL\",\"phone\":\"13800001111\",\"gender\":\"MALE\",\"city\":\"深圳\"}")
check "补全基本资料" 0 "$(code_of "$R")"

R=$(req "$CAND_JAR" PUT /api/candidate/resume \
      '{"intention":{"expectCategory":"技术研发","expectCity":"深圳"},"educations":[{"school":"测试大学","major":"计算机","degree":"本科","startDate":"2018-09","endDate":"2022-06"}],"skills":"Java"}')
check "保存在线简历" 0 "$(code_of "$R")"

R=$(get "$CAND_JAR" "/api/candidate/jobs/$JOB_ID/apply-eligibility")
check "资料齐备后可投递" "True" "$(field_of "$R" "data.canApply")"

info "== 4. 投递、重复投递与快照（§19.3）=="
R=$(req "$CAND_JAR" POST "/api/candidate/jobs/$JOB_ID/apply" "")
check "首次投递成功" 0 "$(code_of "$R")"
APP_ID=$(field_of "$R" "data.applicationId")
check "首次投递 attemptNo=1" 1 "$(field_of "$R" "data.attemptNo")"

R=$(req "$CAND_JAR" POST "/api/candidate/jobs/$JOB_ID/apply" "")
check "重复投递被拒绝" 3002 "$(code_of "$R")"

R=$(get "$CAND_JAR" "/api/candidate/applications/$APP_ID")
check "投递详情可读" 0 "$(code_of "$R")"
SNAP_TITLE=$(field_of "$R" "data.jobSnapshot.title")
[ -n "$SNAP_TITLE" ] && check "投递已生成职位快照" 0 0 || check "投递已生成职位快照" 0 1
SNAP_EDU=$(field_of "$R" "data.resumeSnapshot.educations.0.school")
check "投递已生成简历快照" "测试大学" "$SNAP_EDU"

info "  修改在线简历，验证快照不变"
R=$(req "$CAND_JAR" PUT /api/candidate/resume \
      '{"intention":{"expectCategory":"技术研发","expectCity":"广州"},"educations":[{"school":"改名后的大学","major":"计算机","degree":"本科","startDate":"2018-09","endDate":"2022-06"}],"skills":"Java"}')
R=$(get "$CAND_JAR" "/api/candidate/applications/$APP_ID")
check "改简历后历史投递快照不变" "测试大学" "$(field_of "$R" "data.resumeSnapshot.educations.0.school")"

info "== 5. 撤回与重新投递（§19.4）=="
R=$(req "$CAND_JAR" POST "/api/candidate/applications/$APP_ID/withdraw" '{"reason":"WRONG_APPLY","remark":"投错了"}')
check "撤回成功" 0 "$(code_of "$R")"
R=$(req "$CAND_JAR" POST "/api/candidate/applications/$APP_ID/withdraw" '{"reason":"OTHER"}')
check "重复撤回被拒绝" 3006 "$(code_of "$R")"

R=$(req "$CAND_JAR" POST "/api/candidate/jobs/$JOB_ID/apply" "")
check "撤回后可重新投递" 0 "$(code_of "$R")"
APP_ID2=$(field_of "$R" "data.applicationId")
check "重投 attemptNo=2" 2 "$(field_of "$R" "data.attemptNo")"

R=$(req "$CAND_JAR" POST "/api/candidate/applications/$APP_ID2/withdraw" '{"reason":"OTHER"}')
R=$(req "$CAND_JAR" POST "/api/candidate/jobs/$JOB_ID/apply" "")
APP_ID3=$(field_of "$R" "data.applicationId")
check "第三次投递成功" 3 "$(field_of "$R" "data.attemptNo")"
R=$(req "$CAND_JAR" POST "/api/candidate/applications/$APP_ID3/withdraw" '{"reason":"OTHER"}')
R=$(req "$CAND_JAR" POST "/api/candidate/jobs/$JOB_ID/apply" "")
check "超过 3 次上限被拒绝" 3007 "$(code_of "$R")"

info "== 5.5 准备一条有效投递用于 HR 流程测试 =="
R=$(curl -s "$BASE/api/public/jobs?type=SOCIAL")
JOB_ID2=$(field_of "$R" "data.list.1.id")
CAND2_EMAIL="cand2${STAMP}@example.com"
CAND2_JAR="$TMP/cand2.txt"
init_session "$CAND2_JAR"
R=$(req "$CAND2_JAR" POST /api/auth/register \
      "{\"name\":\"冒烟候选人二\",\"email\":\"$CAND2_EMAIL\",\"password\":\"Test@2026\",\"confirmPassword\":\"Test@2026\",\"agreePrivacy\":true}")
check "第二个候选人注册成功" 0 "$(code_of "$R")"
req "$CAND2_JAR" PUT /api/candidate/profile \
    "{\"name\":\"冒烟候选人二\",\"email\":\"$CAND2_EMAIL\",\"phone\":\"13800002222\",\"city\":\"深圳\"}" > /dev/null
req "$CAND2_JAR" PUT /api/candidate/resume \
    '{"intention":{"expectCategory":"技术研发","expectCity":"深圳"},"educations":[{"school":"某某大学","major":"软件工程","degree":"本科","startDate":"2018-09","endDate":"2022-06"}]}' > /dev/null
R=$(req "$CAND2_JAR" POST "/api/candidate/jobs/$JOB_ID2/apply" "")
check "第二个候选人投递成功" 0 "$(code_of "$R")"
ACTIVE_APP=$(field_of "$R" "data.applicationId")
info "  有效投递 ID=${ACTIVE_APP} / 职位 ${JOB_ID2} / 负责人为管理员"

info "== 6. 管理员登录与 HR 账号管理（§19.5）=="
init_session "$ADMIN_JAR"
R=$(req "$ADMIN_JAR" POST /api/auth/login '{"email":"hr.admin@zkteco.com","password":"Admin@2026"}')
check "管理员登录成功" 0 "$(code_of "$R")"
check "管理员标识正确" "True" "$(field_of "$R" "data.hrAdmin")"

R=$(req "$ADMIN_JAR" POST /api/admin/hr-users \
      "{\"name\":\"冒烟HR\",\"email\":\"$HR_EMAIL\",\"password\":\"Hr@202600\",\"hrAdmin\":false}")
check "创建普通 HR 账号" 0 "$(code_of "$R")"
HR_ID=$(field_of "$R" "data.id")

info "== 7. 普通 HR 归属权限（§19.5）=="
init_session "$HR_JAR"
R=$(req "$HR_JAR" POST /api/auth/login "{\"email\":\"$HR_EMAIL\",\"password\":\"Hr@202600\"}")
check "普通 HR 登录成功" 0 "$(code_of "$R")"

R=$(get "$HR_JAR" "/api/admin/hr-users")
check "普通 HR 访问管理员接口返回 1007" 1007 "$(code_of "$R")"

R=$(get "$HR_JAR" "/api/hr/applications?scope=ALL&includeWithdrawn=true")
check "普通 HR 可读全部投递" 0 "$(code_of "$R")"
R=$(get "$HR_JAR" "/api/hr/applications/$ACTIVE_APP")
check "普通 HR 可读他人负责的投递详情" 0 "$(code_of "$R")"
check "他人负责的记录 canWrite=false" "False" "$(field_of "$R" "data.canWrite")"
OTHER_VER=$(field_of "$R" "data.version")
R=$(req "$HR_JAR" PUT "/api/hr/applications/$ACTIVE_APP/status" \
      "{\"targetStatus\":\"VIEWED\",\"version\":$OTHER_VER}")
check "普通 HR 写他人负责的投递返回 3008" 3008 "$(code_of "$R")"
R=$(req "$HR_JAR" PUT "/api/hr/applications/$ACTIVE_APP/note" \
      "{\"note\":\"越权备注\",\"version\":$OTHER_VER}")
check "普通 HR 写他人备注返回 3008" 3008 "$(code_of "$R")"

info "== 8. HR 处理流程与状态机（§19.4）=="
TARGET_APP="$ACTIVE_APP"
R=$(get "$ADMIN_JAR" "/api/hr/applications/$TARGET_APP")
VER=$(field_of "$R" "data.version")
check "管理员可读投递详情" 0 "$(code_of "$R")"

R=$(req "$ADMIN_JAR" PUT "/api/hr/applications/$TARGET_APP/status" \
      "{\"targetStatus\":\"PASSED\",\"version\":$VER}")
check "非法转换 SUBMITTED→PASSED 被拒绝" 3005 "$(code_of "$R")"

R=$(req "$ADMIN_JAR" PUT "/api/hr/applications/$TARGET_APP/status" \
      "{\"targetStatus\":\"VIEWED\",\"version\":$VER}")
check "标记已查看成功" 0 "$(code_of "$R")"

R=$(req "$ADMIN_JAR" PUT "/api/hr/applications/$TARGET_APP/status" \
      "{\"targetStatus\":\"INTERVIEW\",\"version\":$VER}")
check "使用过期版本号返回 3004" 3004 "$(code_of "$R")"

R=$(get "$ADMIN_JAR" "/api/hr/applications/$TARGET_APP")
VER=$(field_of "$R" "data.version")
R=$(req "$ADMIN_JAR" PUT "/api/hr/applications/$TARGET_APP/interview" \
      "{\"interviewTime\":\"2026-09-10 10:00:00\",\"method\":\"OFFLINE\",\"address\":\"东莞塘厦总部\",\"contactNote\":\"请提前十分钟到达\",\"version\":$VER}")
check "安排面试成功" 0 "$(code_of "$R")"
HINT=$(field_of "$R" "data.noticeHint")
[ -n "$HINT" ] && check "面试安排返回不通知提示" 0 0 || check "面试安排返回不通知提示" 0 1

R=$(get "$ADMIN_JAR" "/api/hr/applications/$TARGET_APP")
check "安排面试后状态为待面试" "INTERVIEW" "$(field_of "$R" "data.status")"
VER=$(field_of "$R" "data.version")

R=$(req "$ADMIN_JAR" PUT "/api/hr/applications/$TARGET_APP/status" \
      "{\"targetStatus\":\"REJECTED\",\"version\":$VER,\"remark\":\"综合评估不匹配\"}")
check "标记不合适成功" 0 "$(code_of "$R")"
R=$(get "$ADMIN_JAR" "/api/hr/applications/$TARGET_APP")
VER=$(field_of "$R" "data.version")
R=$(req "$ADMIN_JAR" PUT "/api/hr/applications/$TARGET_APP/status" \
      "{\"targetStatus\":\"VIEWED\",\"version\":$VER,\"remark\":\"误操作撤销\"}")
check "撤销结论回到已查看" 0 "$(code_of "$R")"

R=$(get "$ADMIN_JAR" "/api/hr/applications/$TARGET_APP")
VER=$(field_of "$R" "data.version")
R=$(req "$ADMIN_JAR" PUT "/api/hr/applications/$TARGET_APP/note" \
      "{\"note\":\"内部备注：沟通意愿强\",\"version\":$VER}")
check "填写内部备注成功" 0 "$(code_of "$R")"

info "  验证内部备注与面试评价不出现在求职者端响应体（§19.3）"
R=$(get "$CAND2_JAR" "/api/candidate/applications/$TARGET_APP")
LEAK=$(echo "$R" | python3 -c "
import json,sys
raw=sys.stdin.read()
print('leak' if ('hrNote' in raw or 'evaluation' in raw) else 'clean')" 2>/dev/null)
check "求职者端无内部字段" "clean" "$LEAK"

R=$(get "$ADMIN_JAR" "/api/hr/applications/$TARGET_APP/logs")
LOG_LEN=$(echo "$R" | python3 -c "import json,sys; print(len(json.load(sys.stdin).get('data') or []))" 2>/dev/null)
if [ "${LOG_LEN:-0}" -ge 5 ]; then
  PASS=$((PASS+1)); green "  PASS  状态变更留痕完整 (共 $LOG_LEN 条)"
else
  FAIL=$((FAIL+1)); red "  FAIL  状态变更日志不足 (共 ${LOG_LEN:-0} 条)"
fi

info "== 9. HR 不能代替候选人撤回（§19.4）=="
R=$(get "$ADMIN_JAR" "/api/hr/applications/$TARGET_APP")
VER=$(field_of "$R" "data.version")
R=$(req "$ADMIN_JAR" PUT "/api/hr/applications/$TARGET_APP/status" \
      "{\"targetStatus\":\"WITHDRAWN\",\"version\":$VER}")
check "HR 置为已撤回被拒绝" 3005 "$(code_of "$R")"

info "== 10. 撤回记录只读（§19.4）=="
R=$(get "$ADMIN_JAR" "/api/hr/applications?scope=ALL&status=WITHDRAWN&includeWithdrawn=true")
WD_APP=$(field_of "$R" "data.list.0.id")
if [ -n "$WD_APP" ]; then
  R=$(get "$ADMIN_JAR" "/api/hr/applications/$WD_APP")
  VER=$(field_of "$R" "data.version")
  check "撤回记录 readOnly=true" "True" "$(field_of "$R" "data.readOnly")"
  R=$(req "$ADMIN_JAR" PUT "/api/hr/applications/$WD_APP/status" \
        "{\"targetStatus\":\"VIEWED\",\"version\":$VER}")
  check "操作撤回记录被拒绝" 3005 "$(code_of "$R")"
fi

info "== 11. 默认不显示撤回记录（§19.4）=="
R=$(get "$ADMIN_JAR" "/api/hr/applications?scope=ALL")
HAS_WD=$(echo "$R" | python3 -c "
import json,sys
rows=(json.load(sys.stdin).get('data') or {}).get('list') or []
print('yes' if any(r.get('status')=='WITHDRAWN' for r in rows) else 'no')" 2>/dev/null)
check "默认列表不含已撤回" "no" "$HAS_WD"

info "== 12. 职位状态机与字典（§19.1、§19.6）=="
R=$(req "$ADMIN_JAR" POST /api/hr/jobs \
      '{"title":"冒烟测试职位","recruitmentType":"SOCIAL","categoryId":1,"locationId":1,"headcount":1,"education":"本科及以上","duty":"测试职责","requirement":"测试要求"}')
check "社招缺少工作经验被拒绝" 1001 "$(code_of "$R")"

R=$(req "$ADMIN_JAR" POST /api/hr/jobs \
      '{"title":"冒烟校招职位","recruitmentType":"CAMPUS","categoryId":1,"locationId":1,"headcount":1,"education":"本科及以上","graduationYear":"2027届","duty":"测试职责","requirement":"测试要求"}')
check "校招缺少招聘对象被拒绝" 1001 "$(code_of "$R")"

R=$(req "$ADMIN_JAR" POST /api/hr/jobs \
      '{"title":"冒烟测试职位","recruitmentType":"SOCIAL","categoryId":1,"locationId":1,"headcount":1,"education":"本科及以上","experience":"1-3年","duty":"测试职责","requirement":"测试要求"}')
check "创建职位成功（草稿）" 0 "$(code_of "$R")"
NEW_JOB=$(field_of "$R" "data.id")

R=$(req "$ADMIN_JAR" PUT "/api/hr/jobs/$NEW_JOB/status" '{"targetStatus":"CLOSED","version":0}')
check "草稿直接关闭被拒绝" 3005 "$(code_of "$R")"
R=$(req "$ADMIN_JAR" PUT "/api/hr/jobs/$NEW_JOB/status" '{"targetStatus":"PUBLISHED","version":0}')
check "发布职位成功" 0 "$(code_of "$R")"

R=$(req "$ADMIN_JAR" POST /api/admin/job-categories '{"name":"技术研发"}')
check "字典名称重复被拒绝" 5002 "$(code_of "$R")"
R=$(curl -s -X DELETE -b "$ADMIN_JAR" -H "X-XSRF-TOKEN: $(awk '/XSRF-TOKEN/{print $7}' "$ADMIN_JAR" | tail -1)" "$BASE/api/admin/job-categories/1")
check "字典不提供物理删除" 5001 "$(code_of "$R")"

info "== 13. 首页内容管理接口已移除（§4.2）=="
R=$(curl -s "$BASE/api/public/site-content"); check "公开首页内容接口已下线" 1004 "$(code_of "$R")"
R=$(get "$ADMIN_JAR" "/api/admin/site-content"); check "管理端首页内容接口已下线" 1004 "$(code_of "$R")"
R=$(get "$ADMIN_JAR" "/api/admin/site-assets"); check "站点图片接口已下线" 1004 "$(code_of "$R")"
R=$(curl -s "$BASE/api/public/assets/1"); check "站点图片公开访问已下线" 1004 "$(code_of "$R")"

info "== 14. 管理员保护与停用规则（§19.5）=="
R=$(req "$ADMIN_JAR" PUT "/api/admin/hr-users/1" \
      '{"name":"系统管理员","email":"hr.admin@zkteco.com","hrAdmin":false}')
check "取消自己的管理员位被拒绝" 6003 "$(code_of "$R")"

R=$(curl -s -X PUT -b "$ADMIN_JAR" -H "X-XSRF-TOKEN: $(awk '/XSRF-TOKEN/{print $7}' "$ADMIN_JAR" | tail -1)" \
      "$BASE/api/admin/hr-users/1/status?status=DISABLED")
check "停用自己被拒绝" 1001 "$(code_of "$R")"

info "== 15. 权限边界（§19.7）=="
R=$(get "$CAND_JAR" "/api/hr/dashboard")
check "求职者访问 HR 接口返回 1003" 1003 "$(code_of "$R")"
R=$(curl -s "$BASE/api/hr/dashboard")
check "未登录访问 HR 接口返回 1002" 1002 "$(code_of "$R")"
R=$(curl -s "$BASE/api/candidate/profile")
check "未登录访问求职者接口返回 1002" 1002 "$(code_of "$R")"

info "  验证求职者无法查看他人投递"
R=$(get "$CAND_JAR" "/api/candidate/applications/$TARGET_APP")
ACODE=$(code_of "$R")
if [ "$ACODE" = "1003" ] || [ "$ACODE" = "1004" ]; then
  PASS=$((PASS+1)); green "  PASS  越权查看他人投递被拦截 (code=$ACODE)"
else
  FAIL=$((FAIL+1)); red "  FAIL  越权查看未被拦截 (code=$ACODE)"
fi

info "  验证简历文件没有任何公开访问入口"
R=$(curl -s "$BASE/api/candidate/resume/file/download")
check "未登录下载简历返回 1002" 1002 "$(code_of "$R")"
R=$(curl -s "$BASE/storage/resume/1/any.pdf")
check "简历目录无静态映射" 1004 "$(code_of "$R")"

info "== 16. 登录失败锁定（§19.2）=="
init_session "$TMP/lock.txt"
for i in 1 2 3 4 5; do
  req "$TMP/lock.txt" POST /api/auth/login \
    "{\"email\":\"lock${STAMP}@example.com\",\"password\":\"WrongPass1\"}" > /dev/null
done
R=$(req "$TMP/lock.txt" POST /api/auth/login \
      "{\"email\":\"lock${STAMP}@example.com\",\"password\":\"WrongPass1\"}")
check "连续失败 5 次后锁定" 2004 "$(code_of "$R")"

info "== 17. 退出登录（§19.2）=="
R=$(req "$CAND_JAR" POST /api/auth/logout "")
check "退出成功" 0 "$(code_of "$R")"
R=$(get "$CAND_JAR" "/api/auth/me")
check "退出后 me 返回 1002" 1002 "$(code_of "$R")"

echo
echo "==================================================="
green "通过: $PASS"
if [ "$FAIL" -gt 0 ]; then red "失败: $FAIL"; else green "失败: 0"; fi
echo "==================================================="
rm -rf "$TMP"
[ "$FAIL" -eq 0 ]
