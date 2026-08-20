package jp.tonbiattack.debuglab.signup;

import java.util.Scanner;

/**
 * 年齢と氏名が別行で届く登録入力を読み取ります。
 */
public class SignupConsoleParser {

    private String lastRegisteredName;
    private int registeredCount;

    public SignupOutcome register(String input) {
        try (Scanner scanner = new Scanner(input)) {
            int age = scanner.nextInt();
            scanner.nextLine();
            String name = scanner.nextLine();
            if (name.isEmpty()) {
                return SignupOutcome.REJECTED_EMPTY_NAME;
            }
            lastRegisteredName = name;
            registeredCount++;
            return SignupOutcome.REGISTERED;
        }
    }

    public String lastRegisteredName() {
        return lastRegisteredName;
    }

    public int registeredCount() {
        return registeredCount;
    }
}
