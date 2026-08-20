# `Scanner.nextInt()`の直後に`nextLine()`が空行を返す

Java標準ライブラリの`Scanner`を題材に、**数値トークンの後に残った行末を氏名として読み、次行の氏名を失う**問題を、失敗するテスト、原因の直接観測、最小修正、回帰テストの順に追うデバッグ教材です。既定ブランチの`main`は成功状態に保ち、意図的に失敗する状態はGit履歴に独立して残します。

## この題材で守る契約

> 入力`"28\nAda Lovelace\n"`を読むとき、年齢28の後にある氏名`Ada Lovelace`を登録し、最後の登録者名を`Ada Lovelace`、登録件数を`1`にする。

| 段階 | 実施内容 | 確認すること |
| --- | --- | --- |
| 再現 | `nextInt()`の直後に一度だけ`nextLine()`を呼ぶ | 残った行末を消費して空文字列となり、氏名登録が拒否される |
| 観測 | 連続する二回の`nextLine()`を比較する | 一回目は空文字列、二回目は`Ada Lovelace`を返す |
| 修正 | 数値トークン後の行末を先に消費する | 次の`nextLine()`で氏名行を取得できる |
| 回帰防止 | 同じ登録テストを再実行する | 結果、最後の氏名、登録件数がすべて更新される |

## 必要な環境

| 項目 | バージョン |
| --- | --- |
| JDK | 21 |
| Maven | 3.8以上 |
| テストランナー | JUnit Jupiter 5.11.4 |
| アプリケーションフレームワーク | 不使用 |

## 最短の開始手順

```bash
mvn --batch-mode clean test
```

検証済みの`main`では、3テストがすべて成功します。

## バグを再現する

```bash
git checkout c1cc932
mvn --batch-mode test -Dtest=SignupConsoleParserTest
# expected: <REGISTERED> but was: <REJECTED_EMPTY_NAME>
# expected: <Ada Lovelace> but was: <null>
# expected: <1> but was: <0>

git checkout main
mvn --batch-mode clean test
# Tests run: 3, Failures: 0, Errors: 0
```

バグコミットでは、年齢の次行の氏名を取得する契約だけが失敗します。完全な出力は[`evidence/01-bug-service-test-output.txt`](evidence/01-bug-service-test-output.txt)に保存しています。

## 原因の要点

`Scanner`は既定で空白を区切りとしてトークンを読みます。[1] `nextInt()`は数値トークンを読み取りますが、行末を越えて次行の氏名へは進みません。`nextLine()`は現在行の残りを返すため、直後の最初の呼出しは改行だけを消費し、空文字列を返します。[1]

したがって、トークン読み取りと行読み取りを混在させる場合は、`nextInt()`の直後に`nextLine()`を一度呼んで残った行末を消費してから、次の`nextLine()`で氏名行を読みます。

## プロジェクト構成

```text
.
├── docs/
│   ├── debugging-record.md      # 観測・仮説・原因・修正・回帰保証
│   ├── novelty-report.md        # 既存Java記事との四軸比較
│   └── topic-brief.md           # 実装前に固定した契約と再現境界
├── evidence/
│   ├── 01-bug-service-test-output.txt
│   ├── 02-scanner-newline-observation-output.txt
│   └── 03-fixed-full-test-output.txt
├── src/main/java/.../signup/
│   ├── SignupConsoleParser.java
│   └── SignupOutcome.java
└── src/test/java/.../signup/
    ├── ScannerNewlineObservationTest.java
    └── SignupConsoleParserTest.java
```

詳細な調査手順は[デバッグ記録](docs/debugging-record.md)、既存コンテンツとの差分は[題材重複調査レポート](docs/novelty-report.md)を参照してください。

## スコープ

この教材は固定の`String`から年齢と氏名を一回だけ読むケースに限定します。実コンソールI/O、入力エラー再試行、ロケール、年齢バリデーションの網羅、永続化、姓名の正規化は対象外です。

トークン単位で読む入力なら`nextInt()`や`next()`は正しい選択です。行単位で読む入力なら、はじめから`nextLine()`を使って数値を明示的に変換する設計も有効です。入力の意味がトークンか行かを先に決め、APIを混在させる境界を明確にしてください。

## References

[1] [Oracle: `Scanner` — token scanning, delimiters, and `nextLine`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Scanner.html)
