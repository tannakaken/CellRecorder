# CellRecorder

北見工業大学内でのプロジェクトで、
基地局の情報と位置情報を取得し、保存する必要に迫られたが、
ちょうどよい既存のアプリがなかったので、自分で作ることにした。

## 方針

- UIを最低限にして、必要な情報を単純に保存するものにする。
- アプリを開いてなくても、画面をスリープにしていても、情報収集を続けるものにする。
- 場合によっては、google play storeへの公開が難しいものになってもよい。

## 機能とTODO

- 位置情報はまだ緯度と経度しかとっていない。TODO 速度や高度などもっと細かいデータも拾う。
- 基地局情報はまだ非常に粗い情報しかとっていない。TODO より細かい情報を取得する。
- TODO 情報取得前も情報取得中も、現在の位置情報や基地局情報が見えるくらいのUIはさすがにあった方がよい。
- TODO 情報の取得頻度などを設定できた方がよい。設定メニューはあるが、現在は何の機能もない。

## 担当者

田中健策 <tannakaken@gmail.com>
