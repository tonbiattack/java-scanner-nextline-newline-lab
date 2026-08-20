# E010: `Scanner.nextInt()`の直後に`nextLine()`が空行を返す

## 目的

入力が`"28\nAda Lovelace\n"`のとき、年齢を読み取った後に氏名`Ada Lovelace`を登録し、最後の登録者名と登録件数を更新する必要があります。しかし`nextInt()`の直後に`nextLine()`を呼ぶと、残った行末だけを消費して空文字列を返します。

## 実行環境と再現境界

このラボはJava 21、Maven、JUnit Jupiter 5.11.4だけを使います。`System.in`、実コンソール、ネットワーク、データベース、ファイルは使いません。公開境界は`SignupConsoleParser#register(String)`であり、直接の`SignupOutcome`に加えて、`lastRegisteredName()`と`registeredCount()`の最終状態を別々に読みます。

テストは固定入力`"28\nAda Lovelace\n"`を一度だけ登録します。年齢の後に改行があり、氏名が次行にあるため、`nextInt()`後に残る入力を決定的に確認できます。時刻、乱数、並行実行、外部I/Oには依存しません。

## 最初に観測した事実

バグ状態はコミット[`c1cc932`](../commit/c1cc932)です。次のコマンドで、意図したアサーション差分を確認しました。

```bash
git checkout c1cc932
mvn --batch-mode test -Dtest=SignupConsoleParserTest
```

| 観測項目 | 期待 | 実際 | 根拠 |
| --- | --- | --- | --- |
| 直接の登録結果 | `REGISTERED` | `REJECTED_EMPTY_NAME` | `SignupConsoleParserTest` |
| 最後の登録者名 | `Ada Lovelace` | `null` | `SignupConsoleParser#lastRegisteredName()` |
| 登録件数 | `1` | `0` | `SignupConsoleParser#registeredCount()` |
| `nextInt()`後の最初の`nextLine()` | 空文字列 | 空文字列 | `ScannerNewlineObservationTest` |
| 二回目の`nextLine()` | `Ada Lovelace` | `Ada Lovelace` | `ScannerNewlineObservationTest` |

```text
年齢の次の行にある氏名を登録する
==> expected: <REGISTERED> but was: <REJECTED_EMPTY_NAME>

最後の登録者名に次行の氏名を保存する
==> expected: <Ada Lovelace> but was: <null>

登録件数を一件に更新する
==> expected: <1> but was: <0>
```

完全な失敗出力は[`evidence/01-bug-service-test-output.txt`](../evidence/01-bug-service-test-output.txt)に保存しています。結果コードだけでなく、氏名と登録件数を最終状態として分けて確認したため、空文字列がバリデーションで拒否され、状態更新に到達していないことを確定できます。

## 競合仮説と検証

| 仮説 | 確認方法 | 結果 |
| --- | --- | --- |
| 氏名行が入力にない | 固定文字列を確認する | `Ada Lovelace`と改行を含むため棄却。 |
| 氏名の空白がバリデーションで拒否される | Scannerから得た文字列を直接観測する | 最初の`nextLine()`は名前でなく空文字列のため棄却。 |
| `nextInt()`後の改行が残る | 連続する二回の`nextLine()`を比較する | 一回目は空、二回目は氏名となるため採用。 |

## 確定した原因

バグ状態では、数値トークンの直後に氏名を読もうとしていました。

```java
int age = scanner.nextInt();
String name = scanner.nextLine();
```

`Scanner`は既定で空白を区切りとしてトークンを読み、`nextInt()`は次の整数トークンを返します。[1] `nextLine()`は現在行を越えて進み、スキップした入力を返します。[1] 数値直後には改行だけが残っているため、最初の`nextLine()`は空文字列を返し、次行にある氏名にはまだ到達していません。

問題は氏名値や年齢値ではなく、**トークン読み取りと行読み取りのカーソル位置の不一致**です。`Scanner`が空白を区切りとしてトークンを読むことと、`nextLine()`が現在行の残りを読むことを混同していました。

## 最小修正

修正コミットは[`d2ced47`](../commit/d2ced47)です。数値後に残った行末を一度消費してから氏名行を読みました。

```java
int age = scanner.nextInt();
scanner.nextLine();
String name = scanner.nextLine();
```

一回目の`nextLine()`は数値の後ろに残る行末を消費します。二回目の`nextLine()`が次行にある`Ada Lovelace`を返します。修正は入力カーソルを進める一行の追加だけで、テストデータ、空氏名の拒否規則、状態更新を変更していません。

`nextInt()`を使い続けたまま空文字列を氏名として許容する、テスト期待値を空氏名へ下げる、氏名の入力をトークンとして`next()`で読む修正は採用していません。公開契約は空白を含み得る**次行の氏名全体**を登録することだからです。

## 回帰保証

### 再発防止テスト

最初に失敗した`numberLineThenNameLine_registersTheFullName`はそのまま残しています。このテストは、結果、最後の氏名、登録件数を別々に検証します。

| テスト | 回帰として守る契約 |
| --- | --- |
| `numberLineThenNameLine_registersTheFullName` | 数値の次行の氏名を全文のまま登録し、状態を更新する。 |
| `explicitlyEmptyName_isRejectedAndPreservesState` | 明示的な空氏名は拒否し、状態を変更しない。 |
| `nextLineAfterNextIntConsumesTheRemainingLineBeforeTheNameLine` | 最初の`nextLine()`が行末を、二回目が氏名行を返すことを直接示す。 |

修正後の`mvn --batch-mode clean test`では、3テストがすべて成功しました。完全な出力は[`evidence/03-fixed-full-test-output.txt`](../evidence/03-fixed-full-test-output.txt)に保存しています。

## 再現手順

```bash
git checkout c1cc932
mvn --batch-mode test -Dtest=SignupConsoleParserTest
# expected: <REGISTERED> but was: <REJECTED_EMPTY_NAME>

git checkout main
mvn --batch-mode clean test
# Tests run: 3, Failures: 0, Errors: 0
```

## スコープと注意点

この修正は、年齢のトークンを読み、次行の氏名を一度だけ読む入力形式に限定します。実コンソールI/O、入力エラー再試行、ロケール、姓名の正規化、永続化は扱いません。

トークン単位の入力では`nextInt()`や`next()`は正しい選択です。行単位で読む設計なら、はじめから`nextLine()`で取得して数値へ変換する方法もあります。入力項目をトークンか行かで分類し、読み取りAPIを混在させる箇所を明示してください。

## References

[1] [Oracle: `Scanner` — token scanning, delimiters, and `nextLine`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Scanner.html)
