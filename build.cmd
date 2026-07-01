@echo off
set "JAVA_HOME=C:\tools\jdk"
set "NODE_HOME=C:\tools\node"
set "PATH=%JAVA_HOME%\bin;%NODE_HOME%;%PATH%"
echo 🛠️ 1巻の表紙を並べるマンガサーバーを組み立てています...
call gradlew.bat clean shadowJar createPortableZip
echo 🎉 完了しました！
echo build\distributions\ フォルダの中に完成品（ZIP）が入っています。
pause
