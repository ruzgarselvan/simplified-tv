#!/usr/bin/env bash
# Read-only inventory of an Android TV / Google TV: what is installed, what is already off, and
# which vendor packages could be switched off. It changes nothing on the TV.
#
# Names that merely look like telemetry are not evidence, so the report only suggests. Check each
# one with `./scripts/debloat.sh --dry-run`, then switch it off on your own TV and see that it works
# before it goes into data/androidtv/packages.txt.
# Usage: ./scripts/discover.sh
set -uo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
parse_flags "$@"
need_device

brand=$(dev1 getprop ro.product.brand); model=$(dev1 getprop ro.product.model)
OUT="$REPORT/discover-$(date +%Y%m%d-%H%M).txt"
mkdir -p "$REPORT"

system=$(dev 'pm list packages -s --user 0' | sed 's/^package://' | sort)
off=$(dev 'pm list packages -d --user 0' | sed 's/^package://' | sort)
third=$(dev 'pm list packages -3 --user 0' | sed 's/^package://' | sort)
[[ -z $system ]] && die "no packages read from the TV"

# Ads, measurement, feedback and promo by name.
LOOKS='(^|\.)(ads?|adservice|advert|advertising|analytics|metrics?|telemetry|tracking|tracker|crash|logger|logging|promo|weblab|experiment|abtest|attribution|referrer|feedback|demo|retail|store(demo)?|tvcare|easy_?installation|companion|rctest)(\.|$|_)'
# System packages that are neither the platform nor Google: the vendor's own.
VENDOR_SKIP='^(android|com\.android\.|com\.google\.|com\.qualcomm|com\.mediatek\.(tv|wwtv)|com\.amlogic|com\.droidlogic|com\.realtek)'

looks=""; vendor=""
for p in $system; do
  printf '%s\n' "$off" | grep -qx "$p" && continue
  essential_pkg "$p" && continue
  if printf '%s' "$p" | grep -Eqi "$LOOKS"; then looks="$looks$p"$'\n'
  elif ! printf '%s' "$p" | grep -Eq "$VENDOR_SKIP"; then vendor="$vendor$p"$'\n'; fi
done

{
  printf '# Simplified TV discover, %s\n# %s %s, Android %s\n' "$(date '+%Y-%m-%d %H:%M')" "$brand" "$model" "$(dev1 getprop ro.build.version.release)"
  printf '# %s system packages, %s already off, %s installed by the maker or the user\n#\n' \
    "$(printf '%s\n' "$system" | grep -c .)" "$(printf '%s\n' "$off" | grep -c .)" "$(printf '%s\n' "$third" | grep -c .)"
  printf '# Nothing here has been changed, and a name is a hint, not a verdict.\n\n'
  printf 'look like ads, feedback, demo or promo:\n%s\n' "$looks"
  printf 'the maker'"'"'s own system packages (review by hand):\n%s\n' "$vendor"
  printf 'already switched off on this TV:\n%s\n' "$off"
} > "$OUT"

step "result"
note "$(printf '%s' "$looks" | grep -c .) look like ads or promo, $(printf '%s' "$vendor" | grep -c .) are the maker's own"
note "report: ${OUT#$ROOT/}"
