@echo off
set SCRIPT_DIR=%~dp0

start "sbt-%CD%" java -Xms256M -Xmx768M -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=384M -Xss2M -Drebel.lift_plugin=true -javaagent:jrebel.jar -jar "%SCRIPT_DIR%sbt-launch-0.12.4.jar" %*


