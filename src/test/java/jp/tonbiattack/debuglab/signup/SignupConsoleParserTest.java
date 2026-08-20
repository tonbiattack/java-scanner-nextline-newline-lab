package jp.tonbiattack.debuglab.signup;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SignupConsoleParserTest {

    @Test
    void numberLineThenNameLine_registersTheFullName() {
        SignupConsoleParser parser = new SignupConsoleParser();

        SignupOutcome outcome = parser.register("28\nAda Lovelace\n");

        assertAll(
                () -> assertEquals(SignupOutcome.REGISTERED, outcome,
                        "年齢の次の行にある氏名を登録する"),
                () -> assertEquals("Ada Lovelace", parser.lastRegisteredName(),
                        "最後の登録者名に次行の氏名を保存する"),
                () -> assertEquals(1, parser.registeredCount(),
                        "登録件数を一件に更新する")
        );
    }

    @Test
    void explicitlyEmptyName_isRejectedAndPreservesState() {
        SignupConsoleParser parser = new SignupConsoleParser();

        SignupOutcome outcome = parser.register("28\n\n");

        assertAll(
                () -> assertEquals(SignupOutcome.REJECTED_EMPTY_NAME, outcome),
                () -> assertEquals(null, parser.lastRegisteredName()),
                () -> assertEquals(0, parser.registeredCount())
        );
    }
}
