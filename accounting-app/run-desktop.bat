@echo off
echo 启动记账软件桌面版...
cd /d %~dp0
call mvn clean compile
call mvn exec:java -Dexec.mainClass="com.accounting.ui.MainApplication" -Dexec.args=""
pause

