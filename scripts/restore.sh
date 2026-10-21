#!/usr/bin/env bash
# Turns back on every package debloat.sh switched off (from its log), or with --all every package
# in data/androidtv/packages.txt.
# Usage: ./scripts/restore.sh [--all] [--dry-run] [-y]
set -uo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
ALL=0
args=(); for a in "$@"; do [[ $a == --all ]] && ALL=1 || args+=("$a"); done
parse_flags ${args[@]+"${args[@]}"}
need_device

LOG="$REPORT/debloat-${SERIAL//[:\/]/_}.log"
if [[ $ALL == 1 ]]; then pkgs=$(list_of "$DATA/packages.txt" | awk '{print $1}')
elif [[ -f $LOG ]]; then pkgs=$(sort -u "$LOG")
else die "no log for $SERIAL; use --all to re-enable everything in data/androidtv/packages.txt"; fi

step "restoring"
n=0
for p in $pkgs; do
  present "$p" || { printf '  %-48s absent\n' "$p"; continue; }
  acmd pm enable --user 0 "$p" >/dev/null
  printf '  %-48s on\n' "$p"; n=$((n+1))
done
[[ $DRY_RUN == 0 && $ALL == 0 ]] && rm -f "$LOG"
note "$n packages turned back on"
