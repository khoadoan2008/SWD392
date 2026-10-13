@echo off
chcp 65001 >nul
echo ==============================================================
echo  AIVES - Khoi dong he thong tren Apache Tomcat 9 (Port 8089)
echo ==============================================================
set "CATALINA_HOME=C:\apache-tomcat-9.0.95"
set "JAVA_HOME=C:\Program Files\Java\jdk-21"

if not exist "%CATALINA_HOME%\bin\catalina.bat" (
    echo Khong tim thay Tomcat tai %CATALINA_HOME%.
    pause
    exit /b 1
)

echo [1/2] Dong goi WAR bang Maven...
call mvn package -DskipTests
if errorlevel 1 (
    echo Build WAR that bai!
    pause
    exit /b 1
)

echo [2/2] Deploy WAR sang Tomcat webapps...
copy /y "target\aives_system.war" "%CATALINA_HOME%\webapps\aives_system.war"

echo.
echo ==============================================================
echo  Tomcat dang khoi dong...
echo  Truy cap ung dung tai: http://localhost:8089/aives_system
echo  Tai khoan test:
echo    - Admin:     admin / admin123
echo    - Giang vien: lecturer1 / 123456
echo    - Sinh vien:  student1 / 123456
echo ==============================================================
echo.
"%CATALINA_HOME%\bin\catalina.bat" run
