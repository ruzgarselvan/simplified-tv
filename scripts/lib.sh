#!/usr/bin/env bash
# Shared helpers for the Android TV / Google TV scripts. Sourced, not run.
# Must stay bash 3.2 compatible: that is what /usr/bin/env bash is on macOS.

ADB=${ADB:-adb}
DRY_RUN=${DRY_RUN:-0}
ASSUME_YES=${ASSUME_YES:-0}
ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
DATA="$ROOT/data/androidtv"
REPORT="$ROOT/report"
CONF=${CONF:-$ROOT/dumbtv.conf}
SERIAL=${SERIAL:-${ADB_SERIAL:-}}

# dumbtv.conf, if there is one: KEEP_APPS="pkg pkg" are never touched.
load_conf() {
  KEEP_APPS=${KEEP_APPS:-}
  [[ -f $CONF ]] && source "$CONF"
  KEEP_APPS=${KEEP_APPS:-}
}

keep_pkg() {
  local k
  for k in $KEEP_APPS; do [[ $k == "$1" ]] && return 0; done
  return 1
}

step() { printf '\n\033[1m%s\033[0m\n' "$*"; }
note() { printf '  %s\n' "$*" >&2; }
die()  { printf 'error: %s\n' "$*" >&2; exit 1; }

dev()  { "$ADB" -s "$SERIAL" shell "$@" 2>&1 </dev/null | tr -d '\r'; }
dev1() { dev "$@" | head -1; }

# A dropped connection must not look like "package absent".
dev_ok() {
  local out
  out=$(dev "$@") || return 1
  printf '%s\n' "$out" | grep -qiE "^error: (device|closed|no devices)|device '[^']*' not found" &&
    die "device $SERIAL stopped responding: reconnect and re-run"
  printf '%s\n' "$out"
}

# Device command that honours --dry-run; the dry-run line goes to stderr.
acmd() {
  if [[ $DRY_RUN == 1 ]]; then note "[dry-run] $*"; return 0; fi
  dev "$@"
}

confirm() {
  [[ $ASSUME_YES == 1 ]] && return 0
  printf '%s [y/N] ' "$1" >&2
  read -r reply
  case $reply in y|Y|yes|YES) return 0 ;; *) return 1 ;; esac
}

# Flags every script takes; a script that wants more handles them before calling this.
parse_flags() {
  for a in "$@"; do
    case $a in
      --dry-run) DRY_RUN=1 ;;
      -y|--yes)  ASSUME_YES=1 ;;
      *) die "unknown flag: $a" ;;
    esac
  done
}

need_device() {
  command -v "$ADB" >/dev/null || die "adb not found in PATH"
  "$ADB" start-server >/dev/null 2>&1
  if [[ -z $SERIAL ]]; then
    local found count
    found=$("$ADB" devices | awk 'NR>1 && $2=="device" {print $1}')
    [[ -z $found ]] && die "no adb device. Turn on USB/network debugging on the TV, then: adb connect <tv-ip>:5555"
    count=$(printf '%s\n' "$found" | wc -l | tr -d ' ')
    [[ $count -gt 1 ]] && die "several devices attached; set ADB_SERIAL to the one you mean: $(printf '%s' "$found" | tr '\n' ' ')"
    SERIAL=$(printf '%s\n' "$found" | head -1)
  fi
  dev getprop ro.build.version.sdk >/dev/null || die "device $SERIAL is not responding"
  dev 'pm list features' | grep -q 'android.software.leanback' ||
    die "$SERIAL is not an Android TV / Google TV (no leanback feature)"
}

list_of() { sed 's/#.*//; s/[[:space:]]*$//' "$1" | awk 'NF'; }
present() { dev_ok "pm path $1" | grep -q '^package:'; }

# Names that are never switched off, whatever a list says: the framework, the TV input
# and tuner stack, remote and Bluetooth, the home screens, setup and the installers.
# A list entry that matches is skipped with a note, so a wrong entry cannot brick a TV.
ESSENTIAL='^(android|com\.android\.(systemui|settings|tv|tv\.settings|packageinstaller|permissioncontroller|providers\..*|inputmethod\..*|bluetooth|nfc|shell|networkstack.*|captiveportallogin|externalstorage|documentsui|location.*|keychain|certinstaller|se|cellbroadcast.*|carrierconfig|phone|server\..*|mtp|wifi.*|webview|vending)|com\.google\.android\.(gms|gsf|vending|tv\..*|tvlauncher|apps\.tv\.launcherx|youtube\.tv|webview|packageinstaller|permissioncontroller|inputmethod\..*|ext\..*|tungsten\.setupwraith))$|(tvinput|tvservice|tv\.service|tvcenter|inputservice|hdmi|\.cec|bluetooth|keyoverlay|dolby|\.audio|setupwizard|overlay|\.rro)'

essential_pkg() {
  printf '%s\n' "$1" | grep -Eq "$ESSENTIAL" && return 0
  list_of "$DATA/never-disable.txt" | awk '{print $1}' | grep -qx "$1"
}

# Packages that own saved Wi-Fi networks: Android deletes a package's networks when it is
# turned off, and nothing brings them back. Printed one per line.
wifi_owners() { dev 'dumpsys wifi' | sed -n 's/.*cname=\([A-Za-z0-9_.]*\).*/\1/p' | sort -u; }

# Packages that hold something live: the home screen, the keyboard, accessibility services.
live_packages() {
  dev 'cmd role get-role-holders --user 0 android.app.role.HOME'
  dev 'settings get secure default_input_method' | sed 's#/.*##'
  dev 'settings get secure enabled_accessibility_services' | tr ':' '\n' | sed 's#/.*##'
}
