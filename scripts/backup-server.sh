#!/usr/bin/env bash
set -euo pipefail
umask 077

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_dir"

if [[ ! -f .env ]]; then
  echo "缺少 $project_dir/.env" >&2
  exit 1
fi

mkdir -p backups
stamp="$(date +%Y%m%d_%H%M%S)"
output_dir="backups/zk_recruit_${stamp}"
mkdir -p "$output_dir"

docker compose exec -T db sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump -uroot --single-transaction --routines --triggers zk_recruit' \
  | gzip -9 > "$output_dir/database.sql.gz.partial"

docker compose exec -T backend tar -czf - -C /data/storage . \
  > "$output_dir/resumes.tar.gz.partial"

gzip -t "$output_dir/database.sql.gz.partial" "$output_dir/resumes.tar.gz.partial"
mv "$output_dir/database.sql.gz.partial" "$output_dir/database.sql.gz"
mv "$output_dir/resumes.tar.gz.partial" "$output_dir/resumes.tar.gz"

echo "备份完成: $output_dir（数据库 + 简历附件）"
