@echo off
chcp 65001 >nul <nul
setlocal
rem ==============================================================
rem  AIVES - Tạo database "aives" + dữ liệu demo (Windows)
rem  Cách dùng: bấm đúp file này, nhập mật khẩu user postgres.
rem  Tuỳ chọn (đặt trước khi chạy): PGHOST, PGPORT, PGUSER, DBNAME, PGBIN
rem ==============================================================
set "DIR=%~dp0"
if not defined PGHOST set "PGHOST=localhost"
if not defined PGPORT set "PGPORT=5432"
if not defined PGUSER set "PGUSER=postgres"
if not defined DBNAME set "DBNAME=aives"

rem ---- Tìm thư mục bin của PostgreSQL ----
if defined PGBIN goto :found
for /f "delims=" %%P in ('where psql.exe 2^>nul') do (
    set "PGBIN=%%~dpP"
    goto :found
)
for /d %%V in ("C:\Program Files\PostgreSQL\*") do (
    if exist "%%~V\bin\psql.exe" set "PGBIN=%%~V\bin"
)
if not defined PGBIN (
    echo Không tìm thấy PostgreSQL. Hãy cài PostgreSQL hoặc đặt biến PGBIN trỏ tới thư mục bin.
    goto :fail
)
:found
echo Dùng PostgreSQL tại: %PGBIN%
echo Server: %PGHOST%:%PGPORT%   User: %PGUSER%   Database: %DBNAME%
echo.

set /p "PGPASSWORD=Nhập mật khẩu của user %PGUSER%: "
echo.
echo LƯU Ý: database "%DBNAME%" (nếu đã có) sẽ bị XOÁ và tạo lại. Hãy tắt Tomcat trước.
set /p "OK=Tiếp tục? (y/n): "
if /i not "%OK:~0,1%"=="y" (
    echo Đã huỷ.
    goto :end
)

echo.
echo [1/4] Xoá database cũ (nếu có)...
"%PGBIN%\dropdb.exe" --if-exists "%DBNAME%" || goto :fail
echo [2/4] Tạo database mới (UTF-8)...
"%PGBIN%\createdb.exe" -E UTF8 -T template0 "%DBNAME%" || goto :fail
echo [3/4] Tạo bảng...
"%PGBIN%\psql.exe" -d "%DBNAME%" -q -v ON_ERROR_STOP=1 -f "%DIR%aives_schema.sql" 2>&1 | findstr /v /c:"NOTICE"
"%PGBIN%\psql.exe" -d "%DBNAME%" -tAc "SELECT 1 FROM users LIMIT 1" >nul 2>&1 || goto :fail
echo [4/4] Nạp dữ liệu demo...
"%PGBIN%\psql.exe" -d "%DBNAME%" -q -v ON_ERROR_STOP=1 -f "%DIR%aives_demo_data.sql" || goto :fail

echo.
echo ==============================================================
echo  XONG! Database "%DBNAME%" đã sẵn sàng.
echo  Nhớ sửa mật khẩu trong src\main\resources\db.properties
echo  Tài khoản demo: admin/admin123 - lecturer1, student1... /123456
echo ==============================================================
goto :end

:fail
echo.
echo *** CÓ LỖI - xem thông báo ở trên. Thường gặp: sai mật khẩu, PostgreSQL chưa chạy,
echo     hoặc database đang được Tomcat/pgAdmin dùng (hãy tắt rồi chạy lại).
:end
echo.
pause
endlocal
