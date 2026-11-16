@echo off
title 3D Mouse CAD Receiver Server
mode con: cols=85 lines=32
cls

echo  ========================================================
echo      3D MOUSE CAD -- PC RECEIVER SETUP AND LAUNCHER
echo  ========================================================
echo.

:: Smart Python Discovery (Bypasses Windows Store fake stubs)
set "PYTHON_CMD="

:: 1. Search User AppData Python Installations
for /d %%D in ("%LocalAppData%\Programs\Python\Python*") do (
    if exist "%%D\python.exe" set "PYTHON_CMD=%%D\python.exe"
)

:: 2. Search Program Files Python Installations
if "%PYTHON_CMD%"=="" (
    for /d %%D in ("%ProgramFiles%\Python*") do (
        if exist "%%D\python.exe" set "PYTHON_CMD=%%D\python.exe"
    )
)

:: 3. Check System PATH
if "%PYTHON_CMD%"=="" (
    where python >nul 2>nul
    if %errorlevel% equ 0 set "PYTHON_CMD=python"
)

:: 4. Check Python Launcher
if "%PYTHON_CMD%"=="" (
    where py >nul 2>nul
    if %errorlevel% equ 0 set "PYTHON_CMD=py"
)

:: 5. Fallback: Search Blender Bundled Python
if "%PYTHON_CMD%"=="" (
    for /d %%D in ("%ProgramFiles%\Blender Foundation\Blender*") do (
        for /d %%V in ("%%D\*") do (
            if exist "%%V\python\bin\python.exe" set "PYTHON_CMD=%%V\python\bin\python.exe"
        )
    )
)

if "%PYTHON_CMD%"=="" (
    echo  [ERROR] Python executable not found!
    echo.
    echo  Please install Python from https://www.python.org
    echo  IMPORTANT: Check the box "Add Python to PATH" during installation.
    echo.
    pause
    exit /b
)

:: Resolve Python receiver script path (Supports Downloads folder and project subfolder)
set "PY_SCRIPT=%~dp0space_mouse_receiver.py"
if not exist "%PY_SCRIPT%" set "PY_SCRIPT=%~dp0pc_receiver\space_mouse_receiver.py"

echo  [1/3] Configuring Windows Firewall (Port 9876)...
netsh advfirewall firewall add rule name="3D_Mouse_CAD_UDP" dir=in action=allow protocol=UDP localport=9876 >nul 2>nul
netsh advfirewall firewall add rule name="3D_Mouse_CAD_TCP" dir=in action=allow protocol=TCP localport=9876 >nul 2>nul
echo        [OK] Firewall configured.
echo.

echo  [2/3] Checking required libraries (pywin32, pynput)...
"%PYTHON_CMD%" -m pip install pywin32 pynput --quiet >nul 2>nul
echo        [OK] Libraries ready.
echo.

echo  [3/3] Launching 3D Mouse Receiver Server...
echo  --------------------------------------------------------
echo.
"%PYTHON_CMD%" "%PY_SCRIPT%"

if %errorlevel% neq 0 (
    echo.
    echo  [Notice] Receiver process finished.
)
echo.
pause
