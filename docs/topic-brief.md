# 題材企画: `Scanner.nextInt()`の直後に`nextLine()`が空行を返す

## 対象

| 項目 | 内容 |
| --- | --- |
| 対象言語 | Java 21 |
| 対象読者 | `Scanner`で数値入力と行入力を混在させ、次の文字列が空になる原因を切り分けたい中級者 |
| 難易度プロファイル | 実践・上級 |
| 選定理由 | `Scanner`の`nextInt()`は区切りをまたいで次のトークンを読み取るのではなく、数値トークンの末尾で止まる。直後の`nextLine()`は残った行末を消費して空文字列を返すため、氏名などの次行入力が失われる。登録結果、最後の登録者名、登録件数を分けて観測し、区切り・トークン読み取り・行読み取りを比較できる。 |
| 実行基盤 | Maven、Java 21、JUnit Jupiter 5.11.4 |
| フレームワーク非依存性 | 原因は`java.util.Scanner`の標準ライブラリ契約である。コンソール、HTTP、DI、DB、外部ネットワークには依存しない。 |

## 学習する契約

> 入力が`"28\nAda Lovelace\n"`のとき、年齢を28、氏名を`Ada Lovelace`として登録し、最後の登録者名を`Ada Lovelace`、登録件数を一件にすべきだが、バグ状態では`nextLine()`が残った行末だけを返して空文字列になり、登録は拒否され、氏名と件数が更新されない。

### 対象の直接原因

`scanner.nextInt()`の直後に、残った改行を消費せずそのまま`scanner.nextLine()`を呼んでいる。`nextLine()`は現在行の残りを返すため、空文字列を返す。

### 対象外

このラボは実コンソールI/O、入力エラー再試行、ロケール、年齢バリデーションの網羅、永続化、姓名の正規化、個人情報を扱わない。固定された`String`入力から年齢と次行の氏名を一回だけ読む狭い規則だけを扱う。

## 再現設計

| 要素 | 決定 |
| --- | --- |
| 公開境界 | `SignupConsoleParser#register(String)`、`lastRegisteredName()`、`registeredCount()`。 |
| 入力・初期状態 | `"28\nAda Lovelace\n"`を一回だけ登録する。 |
| Redの観測 | `SignupOutcome.REGISTERED`を期待するが、バグ状態では`SignupOutcome.REJECTED_EMPTY_NAME`となる。 |
| 最終観測 | `lastRegisteredName()`が`"Ada Lovelace"`となり、`registeredCount()`が`1`であることを別々に検証する。 |
| 決定性 | `System.in`を使わず、固定`String`をScanner入力にし、時刻、乱数、並行実行、`sleep`、外部I/Oを使わない。 |
| 固定状態の検証コマンド | `mvn --batch-mode clean test` |
| バグ状態の確認コマンド | `git checkout <bug-commit>`後に`mvn --batch-mode test -Dtest=SignupConsoleParserTest` |

## 仮説

| 仮説 | どう検証または除外するか |
| --- | --- |
| A: 氏名行が入力にない | 固定文字列に`Ada Lovelace`と改行が含まれることを確認する。 |
| B: 空白を含む氏名の検証が拒否している | Scannerから得た氏名文字列を直接観測する。 |
| C: `nextInt()`後の改行が残り、`nextLine()`が空文字列を返す | 同じ固定入力で`nextInt()`、連続する二回の`nextLine()`を比較する。 |

## 予定する履歴

| 順序 | コミットの目的 | 期待する状態 |
| --- | --- | --- |
| 1 | 数値入力後の氏名が空になる失敗を再現する | 対象テストが`REGISTERED`期待・`REJECTED_EMPTY_NAME`実際のアサーション差分で失敗する。 |
| 2 | 数値トークンの後に残る行末を消費する | 同じ検証が成功し、全体も成功する。 |
