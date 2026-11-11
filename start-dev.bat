@echo off
chcp 65001 >nul
where java >nul 2>nul || (echo [ERROR] Can cai Java 21. & pause & exit /b 1)
where mvn >nul 2>nul || (echo [ERROR] Can cai Apache Maven 3.9+ va them vao PATH. & pause & exit /b 1)
where npm >nul 2>nul || (echo [ERROR] Can cai Node.js 22+ va npm. & pause & exit /b 1)
echo Khoi dong EasyHome (dev, XAMPP MariaDB, mock payment)...
start "EasyHome Backend 8081" /D "%~dp0backend" cmd /k "mvn spring-boot:run"
start "EasyHome Frontend 5173" /D "%~dp0frontend" cmd /k "npm install && npm run dev"
echo Frontend: http://localhost:5173
echo Backend:  http://localhost:8081/api/listings
echo Database: MariaDB cua XAMPP (127.0.0.1:3306/easyhome)
echo De tat: nhan Ctrl+C trong moi cua so terminal.
pause
