#!/usr/bin/env bash
# Bump the app version before a Play release.
#
# Usage:
#   ./bump-version.sh                 # increment versionCode only
#   ./bump-version.sh 1.1             # increment versionCode AND set versionName to 1.1
#
# versionCode must increase for every Play upload; versionName is the label users see.

set -e
GRADLE="app/build.gradle.kts"

current=$(grep -oE 'versionCode = [0-9]+' "$GRADLE" | grep -oE '[0-9]+')
if [ -z "$current" ]; then
  echo "Could not find versionCode in $GRADLE" >&2
  exit 1
fi
new=$((current + 1))
sed -i "s/versionCode = $current/versionCode = $new/" "$GRADLE"
echo "versionCode: $current -> $new"

if [ -n "$1" ]; then
  sed -i "s/versionName = \"[^\"]*\"/versionName = \"$1\"/" "$GRADLE"
  echo "versionName set to: $1"
fi

echo "Done. Next: ./gradlew bundleRelease"
