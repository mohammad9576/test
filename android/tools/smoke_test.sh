#!/usr/bin/env bash
# Emulator smoke test: opens every main screen through the debug «demo mode», saves a screenshot of each and
# records any crash. Usage: tools/smoke_test.sh <apk> <output dir>
set -u
APK="$1"; OUT="$2"
PKG=com.kodomo.app
ACT=$PKG/.kiosk.KidsLauncherActivity
mkdir -p "$OUT"
adb install -r -g "$APK" || { echo "INSTALL FAILED" > "$OUT/report.txt"; exit 0; }
adb shell pm grant $PKG android.permission.CAMERA 2>/dev/null
adb shell pm grant $PKG android.permission.RECORD_AUDIO 2>/dev/null
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS 2>/dev/null
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0

# name|age|route|wait seconds
SCREENS="
home_age3|3|kid/{child}/home|8
home_age7|7|kid/{child}/home|6
home_age12|12|kid/{child}/home|6
videos|7|kid/{child}/videos|5
games|7|kid/{child}/games|5
quest_forest|7|kid/{child}/game/FOREST_QUEST|5
quest_hero|9|kid/{child}/game/HERO_QUEST|5
body_quiz|7|kid/{child}/game/BODY_QUIZ|7
rhythm|7|kid/{child}/game/RHYTHM|7
boxing|7|kid/{child}/game/BOXING|7
tap_race|7|kid/{child}/game/TAP_RACE|4
car_race|7|kid/{child}/game/CAR_RACE|5
bakery|7|kid/{child}/game/BAKERY|4
memory|7|kid/{child}/game/MEMORY_CARDS|5
ping_pong|7|kid/{child}/game/PING_PONG|5
books|7|kid/{child}/books|6
book_real|5|kid/{child}/book/gs0004/SELF|6
music|7|kid/{child}/music|4
learning|7|kid/{child}/learning|4
study|8|kid/{child}/study|5
study_unit|8|kid/{child}/study/g3_times|5
english|7|kid/{child}/english|5
english_unit|7|kid/{child}/english/animals|5
explore|7|kid/{child}/explore|5
explore_item|7|kid/{child}/explore/ex01|5
browser|9|kid/{child}/browser|6
projects|7|kid/{child}/projects|4
chores|7|kid/{child}/chores|4
interests|7|kid/{child}/interests|5
rewards|7|kid/{child}/rewards|4
apps|7|kid/{child}/apps|4
school|9|kid/{child}/school|4
parent_hub|7|parent|6
parent_sites|7|parent/sites|4
parent_wellbeing|7|parent/wellbeing|4
profiles|7|profiles|4
end_jacks|7|kid/{child}/game/JUMPING_JACKS|13|true
end_body_quiz|7|kid/{child}/game/BODY_QUIZ|13|true
end_forest|7|kid/{child}/game/FOREST_QUEST|13|true
end_boxing|7|kid/{child}/game/BOXING|13|true
end_memory|7|kid/{child}/game/MEMORY_CARDS|13|true
"
: > "$OUT/report.txt"
i=0
while IFS='|' read -r name age route wait finish <&3; do
  [ -z "$name" ] && continue
  i=$((i+1))
  adb logcat -c
  adb shell am force-stop $PKG
  adb shell am start -n $ACT --es demo_route "$route" --ei demo_age "$age" --ez demo_finish "${finish:-false}" >/dev/null
  sleep "$wait"
  f=$(printf "%02d_%s.png" "$i" "$name")
  adb exec-out screencap -p > "$OUT/$f"
  if adb logcat -d | grep -q "FATAL EXCEPTION"; then
    echo "CRASH $name ($route)" >> "$OUT/report.txt"
    adb logcat -d | grep -A 40 "FATAL EXCEPTION" | head -60 > "$OUT/crash_$name.txt"
  elif ! adb shell pidof $PKG >/dev/null; then
    echo "NOT RUNNING $name ($route)" >> "$OUT/report.txt"
  else
    echo "OK $name" >> "$OUT/report.txt"
  fi
done 3<<< "$SCREENS"
# smaller screenshots for review
command -v mogrify >/dev/null && mogrify -resize 540x "$OUT"/*.png
cat "$OUT/report.txt"
