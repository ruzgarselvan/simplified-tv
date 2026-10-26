#!/usr/bin/env bash
# Installs Simplified TV on an Android TV or Google TV and makes it the home screen.
#   ./launcher/install.sh              build from source, install, take HOME
#   ./launcher/install.sh app.apk      install a ready APK instead of building
#   ./launcher/install.sh --airplay    also install an AirPlay receiver (adds an AirPlay button to the home screen)
#   ./launcher/install.sh --undo       give HOME back to the TV's own launcher
#   TMDB_TOKEN=<read access token> ./launcher/install.sh
#                                      also stores the token on the TV for the movies row
# Pick the TV with ADB_SERIAL when more than one is connected.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"

PKG=tv.dumbtv.home
CONF=/sdcard/Android/data/$PKG/files/home.conf
ADB=${ADB:-adb}

adb_() { "$ADB" ${ADB_SERIAL:+-s "$ADB_SERIAL"} "$@"; }
sh_() { adb_ shell "$@" </dev/null; }

command -v "$ADB" >/dev/null || { echo "error: adb not found: install Android platform-tools" >&2; exit 1; }
[[ $(adb_ get-state 2>/dev/null || true) == device ]] \
  || { echo "error: no TV connected (adb connect <tv-ip>:5555; set ADB_SERIAL if several)" >&2; exit 1; }

holder() { sh_ cmd role get-role-holders --user 0 android.app.role.HOME 2>/dev/null | tr -d '\r' || true; }

case ${1:-} in
  --undo)
    # a TV set up with another home screen may have its own launcher switched off: switch it back on
    for stock in com.google.android.apps.tv.launcherx com.google.android.tvlauncher com.google.android.leanbacklauncher; do
      if sh_ pm list packages -d --user 0 | grep -qx "package:$stock"; then
        sh_ pm enable --user 0 $stock >/dev/null && echo "re-enabled the TV's own launcher: $stock"
      fi
    done
    sh_ cmd role remove-role-holder --user 0 android.app.role.HOME $PKG >/dev/null 2>&1 || true
    sh_ am start -a android.intent.action.MAIN -c android.intent.category.HOME >/dev/null
    echo "HOME handed back (now: $(holder)). Simplified TV stays installed: adb uninstall $PKG"
    exit 0 ;;
  --smarttube)
    # the latest release from github.com/yuliskov/SmartTube, the build that matches this TV's processor
    abis=$(sh_ getprop ro.product.cpu.abilist | tr -d '\r')
    case ,$abis, in *,arm64-v8a,*) abi=arm64-v8a ;; *,armeabi-v7a,*) abi=armeabi-v7a ;; *,x86,*) abi=x86 ;; *) abi=universal ;; esac
    mkdir -p build
    info=$(curl -fsSL https://api.github.com/repos/yuliskov/SmartTube/releases/latest \
      | python3 -c 'import json,sys
r=json.load(sys.stdin)
for a in r["assets"]:
    if a["name"].endswith("_'$abi'.apk"): print(a["browser_download_url"], a["digest"].split(":")[-1], r["tag_name"])') \
      || { echo "error: could not read the SmartTube release list" >&2; exit 1; }
    set -- $info
    [[ -n ${1:-} ]] || { echo "error: no SmartTube build for $abi in the latest release" >&2; exit 1; }
    echo "SmartTube $3 ($abi)"
    curl -fL --progress-bar -o build/smarttube.apk "$1" || { echo "error: download failed" >&2; exit 1; }
    got=$(shasum -a 256 build/smarttube.apk | awk '{print $1}')
    [[ $got == "$2" ]] || { echo "error: checksum mismatch (got $got): not installing" >&2; rm -f build/smarttube.apk; exit 1; }
    adb_ install -r build/smarttube.apk 2>&1 | tail -2
    echo "installed. Options > YouTube app > SmartTube makes the YouTube tile open it."
    exit 0 ;;
  --airplay)
    # the latest release from github.com/jqssun/android-airplay-server: an AirPlay receiver for the TV
    mkdir -p build
    info=$(curl -fsSL https://api.github.com/repos/jqssun/android-airplay-server/releases/latest \
      | python3 -c 'import json,sys
r=json.load(sys.stdin)
for a in r["assets"]:
    if a["name"] == "app-release.apk": print(a["browser_download_url"], a["digest"].split(":")[-1], r["tag_name"])') \
      || { echo "error: could not read the AirPlay receiver release list" >&2; exit 1; }
    set -- $info
    [[ -n ${1:-} ]] || { echo "error: no APK in the latest AirPlay receiver release" >&2; exit 1; }
    echo "AirPlay receiver $3"
    curl -fL --progress-bar -o build/airplay.apk "$1" || { echo "error: download failed" >&2; exit 1; }
    got=$(shasum -a 256 build/airplay.apk | awk '{print $1}')
    [[ $got == "$2" ]] || { echo "error: checksum mismatch (got $got): not installing" >&2; rm -f build/airplay.apk; exit 1; }
    adb_ install -r build/airplay.apk 2>&1 | tail -2
    echo "installed. Open the AirPlay button on the home screen once on the TV and allow what it asks."
    exit 0 ;;
  ""|*.apk) ;;
  *) echo "usage: $0 [app.apk | --airplay | --undo]" >&2; exit 2 ;;
esac

if [[ -n ${1:-} ]]; then
  APK=$1
else
  ./build.sh
  APK=build/dumbtv-home.apk
fi

out=$(adb_ install -r "$APK" 2>&1 || true)
case $out in
  *Success*) ;;
  *INSTALL_FAILED_UPDATE_INCOMPATIBLE*)
    echo "error: the installed Simplified TV was signed with a different key." >&2
    echo "  back up its settings:  adb pull /sdcard/Android/data/$PKG/files/home.conf" >&2
    echo "  then remove it:        adb uninstall $PKG    and run this again" >&2
    exit 1 ;;
  *) echo "error: adb install failed: $(echo "$out" | tail -2)" >&2; exit 1 ;;
esac
echo "installed $PKG"

# keep the TV from freezing the home screen's process in the background
sh_ dumpsys deviceidle whitelist +$PKG >/dev/null 2>&1 || true

if [[ -n ${TMDB_TOKEN:-} ]]; then
  # the token goes only into the TV's settings file, never into the APK or this repo
  sh_ "mkdir -p $(dirname $CONF); touch $CONF; grep -v '^tmdb_token=' $CONF > $CONF.new; echo 'tmdb_token=$TMDB_TOKEN' >> $CONF.new; mv $CONF.new $CONF"
  echo "tmdb token stored on the TV"
fi

sh_ cmd package set-home-activity $PKG/.Home >/dev/null 2>&1 || true
sh_ cmd role add-role-holder --user 0 android.app.role.HOME $PKG >/dev/null 2>&1 || true
sh_ am start -n $PKG/.Home >/dev/null
# the role manager answers a moment late
for _ in 1 2 3 4 5; do [[ $(holder) == *$PKG* ]] && break; sleep 1; done
if [[ $(holder) == *$PKG* ]]; then
  echo "home: Simplified TV ($(sh_ getprop ro.product.model | tr -d '\r'), Android $(sh_ getprop ro.build.version.release | tr -d '\r'))"
else
  echo "warning: Simplified TV is installed and open, but the TV did not make it the default home (holder: $(holder))." >&2
  echo "  press HOME and choose Simplified TV if the TV asks; ./launcher/install.sh --undo reverts." >&2
fi
