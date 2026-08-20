package jp.tonbiattack.debuglab.signup;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Scanner;

import org.junit.jupiter.api.Test;

class ScannerNewlineObservationTest {

    @Test
    void nextLineAfterNextIntConsumesTheRemainingLineBeforeTheNameLine() {
        try (Scanner scanner = new Scanner("28\nAda Lovelace\n")) {
            int age = scanner.nextInt();
            String remainingLine = scanner.nextLine();
            String nameLine = scanner.nextLine();

            assertAll(
                    () -> assertEquals(28, age),
                    () -> assertEquals("", remainingLine,
                            "nextIntの直後は現在行の残りである改行だけをnextLineが消費する"),
                    () -> assertEquals("Ada Lovelace", nameLine,
                            "二回目のnextLineで氏名行を取得できる")
            );
        }
    }
}
