# 3D Mouse CAD Receiver for Windows (Supports Fusion 360, Blender, CAD, 3D Orbiting)

try {
    Write-Host "=======================================================" -ForegroundColor Cyan
    Write-Host "     3D MOUSE CAD - WINDOWS NATIVE RECEIVER SERVER     " -ForegroundColor Yellow
    Write-Host "=======================================================" -ForegroundColor Cyan
    Write-Host "Listening on UDP & TCP port 9876..." -ForegroundColor Green
    Write-Host "Ready for Fusion 360, Blender, SolidWorks & CAD!" -ForegroundColor Green
    Write-Host "=======================================================" -ForegroundColor Cyan

    $code = @"
    using System;
    using System.Runtime.InteropServices;

    public class Win32Input {
        [DllImport("user32.dll")]
        public static extern void mouse_event(uint dwFlags, int dx, int dy, uint dwData, UIntPtr dwExtraInfo);

        [DllImport("user32.dll")]
        public static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, UIntPtr dwExtraInfo);

        public const uint MOUSEEVENTF_MOVE = 0x0001;
        public const uint MOUSEEVENTF_MIDDLEDOWN = 0x0020;
        public const uint MOUSEEVENTF_MIDDLEUP = 0x0040;
        public const uint KEYEVENTF_KEYUP = 0x0002;
        public const byte VK_SHIFT = 0x10;
    }
"@
    Add-Type -TypeDefinition $code -ErrorAction Stop

    $global:isMmbDown = $false

    function Press-MMB {
        if (-not $global:isMmbDown) {
            $global:isMmbDown = $true
            # Press Shift key (Required for Fusion 360 / AutoCAD / Revit 3D Orbiting)
            [Win32Input]::keybd_event([Win32Input]::VK_SHIFT, 0, 0, [UIntPtr]::Zero)
            # Press Middle Mouse Button
            [Win32Input]::mouse_event([Win32Input]::MOUSEEVENTF_MIDDLEDOWN, 0, 0, 0, [UIntPtr]::Zero)
        }
    }

    function Release-MMB {
        if ($global:isMmbDown) {
            $global:isMmbDown = $false
            # Release Middle Mouse Button
            [Win32Input]::mouse_event([Win32Input]::MOUSEEVENTF_MIDDLEUP, 0, 0, 0, [UIntPtr]::Zero)
            # Release Shift key
            [Win32Input]::keybd_event([Win32Input]::VK_SHIFT, 0, [Win32Input]::KEYEVENTF_KEYUP, [UIntPtr]::Zero)
        }
    }

    function Move-MouseRelative($dx, $dy) {
        [Win32Input]::mouse_event([Win32Input]::MOUSEEVENTF_MOVE, $dx, $dy, 0, [UIntPtr]::Zero)
    }

    function Process-TextPayload($text) {
        foreach ($line in $text.Split("`n")) {
            $trimmed = $line.Trim()
            if ($trimmed.Length -eq 0) { continue }
            try {
                $json = ConvertFrom-Json $trimmed
                if ($json.type -eq "3d_mouse_discover") {
                    $hostname = [System.Environment]::MachineName
                    $resp = "{`"type`":`"3d_mouse_pc_beacon`",`"name`":`"$hostname`"}"
                    $respBytes = [System.Text.Encoding]::UTF8.GetBytes($resp)
                    if ($global:udpClient -ne $null -and $global:lastUdpEndpoint -ne $null) {
                        $global:udpClient.Send($respBytes, $respBytes.Length, $global:lastUdpEndpoint) | Out-Null
                    }
                }
                elseif ($json.type -eq "3d_mouse") {
                    $rx = [int]$json.rx
                    $rz = [int]$json.rz
                    $touchDown = [bool]$json.touch_down

                    if (-not $touchDown -or ($rx -eq 0 -and $rz -eq 0)) {
                        Release-MMB
                    }
                    else {
                        Press-MMB
                        $dx = [int]($rz / 220)
                        $dy = [int]($rx / 220)
                        if ($dx -ne 0 -or $dy -ne 0) {
                            Move-MouseRelative $dx $dy
                        }
                    }
                }
            } catch {}
        }
    }

    # Start UDP Receiver Thread
    $udpThread = [System.Threading.Thread]::new([System.Threading.ThreadStart]{
        try {
            $global:udpClient = New-Object System.Net.Sockets.UdpClient(9876)
            $global:lastUdpEndpoint = New-Object System.Net.IPEndPoint([System.Net.IPAddress]::Any, 0)
            while ($true) {
                $bytes = $global:udpClient.Receive([ref]$global:lastUdpEndpoint)
                $text = [System.Text.Encoding]::UTF8.GetString($bytes)
                Process-TextPayload $text
            }
        } catch {}
    })
    $udpThread.IsBackground = $true
    $udpThread.Start()

    # TCP Listener Loop (For USB ADB)
    $tcpListener = New-Object System.Net.Sockets.TcpListener([System.Net.IPAddress]::Any, 9876)
    $tcpListener.Start()

    while ($true) {
        $client = $tcpListener.AcceptTcpClient()
        $stream = $client.GetStream()
        $buffer = New-Object byte[] 4096

        try {
            while ($client.Connected) {
                $read = $stream.Read($buffer, 0, $buffer.Length)
                if ($read -le 0) { break }
                $text = [System.Text.Encoding]::UTF8.GetString($buffer, 0, $read)
                Process-TextPayload $text
            }
        } catch {}
        finally {
            $client.Close()
            Release-MMB
        }
    }
}
catch {
    Write-Host "ERROR: $_" -ForegroundColor Red
    Write-Host $_.ScriptStackTrace -ForegroundColor Yellow
}
finally {
    Release-MMB
}
