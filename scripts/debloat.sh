#!/usr/bin/env bash
# Switches off the vendor and promo packages in data/androidtv/packages.txt on an Android TV or
# Google TV. Nothing is uninstalled: every package is only disabled for user 0, and
# scripts/restore.sh turns them all back on. Packages that look essential, that hold the
# home screen or the keyboard, or that own a saved Wi-Fi network are skipped.
# Usage: ./scripts/debloat.sh [--dry-run] [-y]
set -uo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
parse_flags "$@"
need_device
load_conf

LOG="$REPORT/debloat-${SERIAL//[:\/]/_}.log"
brand=$(dev1 getprop ro.product.brand); model=$(dev1 getprop ro.product.model)
step "plan"
note "tv         $brand $model, Android $(dev1 getprop ro.build.version.release)"
note "candidates $(list_of "$DATA/packages.txt" | wc -l | tr -d ' ') in data/androidtv/packages.txt (only those installed here are touched)"
[[ -n $KEEP_APPS ]] && note "keeping    $KEEP_APPS"

wifi=$(wifi_owners); live=$(live_packages); off=$(dev "pm list packages -d --user 0")
todo=""
step "checking"
for p in $(list_of "$DATA/packages.txt" | awk '{print $1}'); do
  if ! present "$p"; then continue; fi
  if printf '%s\n' "$off" | grep -qx "package:$p"; then printf '  %-48s already off\n' "$p"; continue; fi
  if essential_pkg "$p"; then printf '  %-48s SKIP (essential)\n' "$p"; continue; fi
  if keep_pkg "$p"; then printf '  %-48s SKIP (keep list)\n' "$p"; continue; fi
  if printf '%s\n' "$live" | grep -qx "$p"; then printf '  %-48s SKIP (holds home, keyboard or an accessibility service)\n' "$p"; continue; fi
  if printf '%s\n' "$wifi" | grep -qx "$p"; then printf '  %-48s SKIP (owns saved Wi-Fi networks)\n' "$p"; continue; fi
  printf '  %-48s will be switched off\n' "$p"
  todo="$todo $p"
done

count=$(printf '%s\n' $todo | grep -c . || true)
[[ $count -eq 0 ]] && { note "nothing to do"; exit 0; }
[[ $DRY_RUN == 1 ]] && { note "dry-run: nothing was changed ($count would be switched off)"; exit 0; }
confirm "Switch off these $count packages on $SERIAL? ./scripts/restore.sh turns them back on." || die "cancelled"

mkdir -p "$REPORT"
step "disabling"
done_n=0
for p in $todo; do
  out=$(dev "pm disable-user --user 0 $p" | head -1)
  case $out in
    *disabled-user*) printf '  %-48s off\n' "$p"; printf '%s\n' "$p" >> "$LOG"; done_n=$((done_n+1)) ;;
    *)               printf '  %-48s kept: %s\n' "$p" "$out" ;;
  esac
done

# If the TV no longer has a home screen, undo at once.
step "check"
if [[ -z $(dev 'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME' | grep '/') ]]; then
  note "no home screen resolves any more: restoring everything"
  "$(dirname "${BASH_SOURCE[0]}")/restore.sh" -y
  die "reverted; nothing is switched off"
fi
note "switched off $done_n of $count; log: ${LOG#$ROOT/}"
note "a reboot lets the TV drop the freed memory: adb reboot"
