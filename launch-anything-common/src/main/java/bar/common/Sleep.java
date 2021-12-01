package bar.common;

import java.util.concurrent.TimeUnit;

public class Sleep {

    public static void milliseconds(int milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (Exception ignored) {
        }
    }

    public static void seconds(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (Exception ignored) {
        }
    }

    public static void minutes(int minutes) {
        try {
            TimeUnit.MINUTES.sleep(minutes);
        } catch (Exception ignored) {
        }
    }

    public static void hours(int hours) {
        try {
            TimeUnit.HOURS.sleep(hours);
        } catch (Exception ignored) {
        }
    }
}
