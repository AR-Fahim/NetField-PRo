# BAT-MANUAL

Manual verification commands for battery alert and charging simulation on emulator/device:

1. Unplug battery simulation:
   ```bash
   adb shell dumpsys battery unplug
   ```

2. Simulate low battery (WARN state, 18%):
   ```bash
   adb shell dumpsys battery set level 18
   ```

3. Simulate critical battery (CRITICAL state, 9%):
   ```bash
   adb shell dumpsys battery set level 9
   ```

4. Simulate charging (cleared to NONE state):
   ```bash
   adb shell dumpsys battery set ac 1
   ```

5. Reset battery simulation:
   ```bash
   adb shell dumpsys battery reset
   ```
