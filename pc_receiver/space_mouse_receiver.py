import socket
import json
import threading
import time
import sys
import ctypes
import random

# PC Receiver & Auto-Discovery Server for 3D Mouse CAD App
# Supports Blender, Fusion 360, SolidWorks, FreeCAD, AutoCAD, Inventor, etc.

PORT = 9876
HOSTNAME = socket.gethostname()

# Enable ANSI escape codes on Windows Console
if sys.platform == "win32":
    try:
        kernel32 = ctypes.windll.kernel32
        kernel32.SetConsoleMode(kernel32.GetStdHandle(-11), 7)
    except Exception:
        pass

# Cyber Neon Color Constants
C_RESET = "\033[0m"
C_BOLD = "\033[1m"
C_CYAN = "\033[96m"
C_ORANGE = "\033[38;2;255;109;0m"
C_GREEN = "\033[92m"
C_YELLOW = "\033[93m"
C_PURPLE = "\033[95m"
C_RED = "\033[91m"
C_GRAY = "\033[90m"
C_WHITE = "\033[97m"

try:
    import win32api
    import win32con
    USE_WIN32 = True
    print(f" {C_GREEN}[✔] Mouse Engine: Native Windows Win32 API Active!{C_RESET}")
except ImportError:
    USE_WIN32 = False
    try:
        import pynput
        mouse_controller = pynput.mouse.Controller()
        mouse_button = pynput.mouse.Button
        keyboard_controller = pynput.keyboard.Controller()
        keyboard_key = pynput.keyboard.Key
        USE_PYNPUT = True
        print(f" {C_GREEN}[✔] Mouse Engine: Pynput Engine Active!{C_RESET}")
    except ImportError:
        USE_PYNPUT = False
        print(f" {C_YELLOW}[!] Install 'pywin32' or 'pynput' (pip install pywin32 pynput){C_RESET}")

NON_CAD_KEYWORDS = [
    "explorer", "program manager", "chrome", "msedge", "firefox",
    "opera", "brave", "android studio", "studio64", "telegram",
    "discord", "taskbar", "task manager", "cmd.exe", "powershell"
]

def get_active_window_title():
    try:
        if sys.platform == "win32":
            hwnd = ctypes.windll.user32.GetForegroundWindow()
            if not hwnd:
                return ""
            length = ctypes.windll.user32.GetWindowTextLengthW(hwnd)
            if length == 0:
                return ""
            buf = ctypes.create_unicode_buffer(length + 1)
            ctypes.windll.user32.GetWindowTextW(hwnd, buf, length + 1)
            return buf.value.lower()
    except Exception:
        pass
    return ""

cached_cad_active = True
last_check_time = 0

def is_cad_window_active():
    global cached_cad_active, last_check_time
    now = time.time()
    if now - last_check_time > 0.25:
        last_check_time = now
        title = get_active_window_title()
        if not title:
            cached_cad_active = True
        else:
            cached_cad_active = not any(kw in title for kw in NON_CAD_KEYWORDS)

    return cached_cad_active

def animate_cyber_banner():
    banner_lines = [
        "  ██████╗ ██████╗     ███╗   ███╗██████╗ ██╗   ██╗███████╗███████╗",
        "  ╚════██╗██╔══██╗    ████╗ ████║██╔═══██╗██║   ██║██╔════╝██╔════╝",
        "   █████╔╝██║  ██║    ██╔████╔██║██║   ██║██║   ██║███████╗█████╗  ",
        "   ╚═══██╗██║  ██║    ██║╚██╔╝██║██║   ██║██║   ██║╚════██║██╔══╝  ",
        "  ██████╔╝██████╔╝    ██║ ╚═╝ ██║╚██████╔╝╚██████╔╝███████║███████╗",
        "  ╚═════╝ ╚═════╝     ╚═╝     ╚═╝ ╚═════╝  ╚═════╝ ╚══════╝╚══════╝"
    ]

    print("\n")

    # Phase 1: Binary Zeros ('0')
    lines1 = []
    for line in banner_lines:
        lines1.append("".join(f"{C_GRAY}0{C_RESET}" if ch != ' ' else ' ' for ch in line))

    sys.stdout.write(f"\033[{len(banner_lines)}A")
    for l in lines1:
        sys.stdout.write(l + "\n")
    sys.stdout.flush()
    time.sleep(0.20)

    # Phase 2: Glitch Matrix Digits ('0', '1', '2', '7', '8', '9')
    for _ in range(4):
        lines2 = []
        for line in banner_lines:
            l_chars = []
            for ch in line:
                if ch == ' ':
                    l_chars.append(' ')
                else:
                    digit = str(random.choice([0, 1, 2, 7, 8, 9]))
                    color = random.choice([C_CYAN, C_GREEN, C_YELLOW, C_GRAY])
                    l_chars.append(f"{color}{digit}{C_RESET}")
            lines2.append("".join(l_chars))

        sys.stdout.write(f"\033[{len(banner_lines)}A")
        for l in lines2:
            sys.stdout.write(l + "\n")
        sys.stdout.flush()
        time.sleep(0.08)

    # Phase 3: RGB Block Glitch ('█')
    for _ in range(3):
        lines3 = []
        for line in banner_lines:
            l_chars = []
            for ch in line:
                if ch == ' ':
                    l_chars.append(' ')
                else:
                    color = random.choice([C_GREEN, C_RED, C_CYAN, C_PURPLE, C_YELLOW])
                    l_chars.append(f"{color}█{C_RESET}")
            lines3.append("".join(l_chars))

        sys.stdout.write(f"\033[{len(banner_lines)}A")
        for l in lines3:
            sys.stdout.write(l + "\n")
        sys.stdout.flush()
        time.sleep(0.08)

    # Phase 4: Final Solid Neon Orange Reveal with Instagram Credit
    lines4 = []
    for line in banner_lines:
        lines4.append(f"{C_ORANGE}{C_BOLD}{line}{C_RESET}")

    sys.stdout.write(f"\033[{len(banner_lines)}A")
    for l in lines4:
        sys.stdout.write(l + "\n")
    sys.stdout.write(f"{C_CYAN}         --- Created by: instagram {C_YELLOW}m4ximys{C_CYAN} ---{C_RESET}\n\n")
    sys.stdout.flush()

def print_ip_addresses():
    try:
        host_name = socket.gethostname()
        print(f" {C_WHITE}PC Name:{C_RESET} {C_YELLOW}{host_name}{C_RESET}")
        print(f" {C_CYAN}[🌐 AVAILABLE NETWORK IP ADDRESSES]{C_RESET}")
        addrs = set()
        try:
            for item in socket.gethostbyname_ex(host_name)[2]:
                if not item.startswith("127."):
                    addrs.add(item)
        except Exception:
            pass
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            s.connect(("8.8.8.8", 80))
            addrs.add(s.getsockname()[0])
            s.close()
        except Exception:
            pass
        for ip in addrs:
            tag = "(PC Hotspot IP)" if "137." in ip else "(Wi-Fi Router IP)"
            print(f"   {C_GREEN}➜ {ip}{C_RESET}  {C_GRAY}{tag}{C_RESET}")
    except Exception:
        pass

animate_cyber_banner()
print_ip_addresses()
print(f"\n {C_CYAN}[⚡ UDP & TCP LISTENING ON PORT {PORT}]{C_RESET}")
print(f" {C_GREEN}[✔] Auto-Discovery Active: Open the Android app to connect!{C_RESET}")
print(f" {C_GRAY}-------------------------------------------------------------------{C_RESET}\n")

# State Machine Variables
mmb_is_down = False
shift_is_down = False
ctrl_is_down = False
last_wheel_time = 0
orig_pos = None
current_preset = "blender"

def set_mouse_states(desired_mmb, desired_shift, desired_ctrl=False, preset="blender"):
    global mmb_is_down, shift_is_down, ctrl_is_down, orig_pos

    if not desired_mmb:
        if mmb_is_down:
            mmb_is_down = False
            if USE_WIN32:
                win32api.mouse_event(win32con.MOUSEEVENTF_MIDDLEUP, 0, 0, 0, 0)
                if orig_pos:
                    win32api.SetCursorPos(orig_pos)
                    orig_pos = None
                win32api.ShowCursor(True)
            elif USE_PYNPUT:
                mouse_controller.release(mouse_button.middle)
                if orig_pos:
                    mouse_controller.position = orig_pos
                    orig_pos = None

        if shift_is_down:
            shift_is_down = False
            if USE_WIN32:
                win32api.keybd_event(0x10, 0, win32con.KEYEVENTF_KEYUP, 0)
            elif USE_PYNPUT:
                keyboard_controller.release(keyboard_key.shift)

        if ctrl_is_down:
            ctrl_is_down = False
            if USE_WIN32:
                win32api.keybd_event(0x11, 0, win32con.KEYEVENTF_KEYUP, 0)
            elif USE_PYNPUT:
                keyboard_controller.release(keyboard_key.ctrl)
        return

    if desired_shift != shift_is_down:
        shift_is_down = desired_shift
        if USE_WIN32:
            flags = 0 if shift_is_down else win32con.KEYEVENTF_KEYUP
            win32api.keybd_event(0x10, 0, flags, 0)
        elif USE_PYNPUT:
            if shift_is_down:
                keyboard_controller.press(keyboard_key.shift)
            else:
                keyboard_controller.release(keyboard_key.shift)

    if desired_ctrl != ctrl_is_down:
        ctrl_is_down = desired_ctrl
        if USE_WIN32:
            flags = 0 if desired_ctrl else win32con.KEYEVENTF_KEYUP
            win32api.keybd_event(0x11, 0, flags, 0)
        elif USE_PYNPUT:
            if desired_ctrl:
                keyboard_controller.press(keyboard_key.ctrl)
            else:
                keyboard_controller.release(keyboard_key.ctrl)

    if not mmb_is_down:
        mmb_is_down = True
        if USE_WIN32:
            orig_pos = win32api.GetCursorPos()
            win32api.ShowCursor(False)
            win32api.mouse_event(win32con.MOUSEEVENTF_MIDDLEDOWN, 0, 0, 0, 0)
        elif USE_PYNPUT:
            orig_pos = mouse_controller.position
            mouse_controller.press(mouse_button.middle)

def move_mouse_relative(dx, dy):
    if USE_WIN32:
        win32api.mouse_event(win32con.MOUSEEVENTF_MOVE, dx, dy, 0, 0)
    elif USE_PYNPUT:
        mouse_controller.move(dx, dy)

def send_key_combo_win32(vk_codes):
    if USE_WIN32:
        for vk in vk_codes:
            win32api.keybd_event(vk, 0, 0, 0)
        time.sleep(0.015)
        for vk in reversed(vk_codes):
            win32api.keybd_event(vk, 0, win32con.KEYEVENTF_KEYUP, 0)
        time.sleep(0.01)
    elif USE_PYNPUT:
        try:
            for vk in vk_codes:
                keyboard_controller.press(pynput.keyboard.KeyCode.from_vk(vk))
            time.sleep(0.015)
            for vk in reversed(vk_codes):
                keyboard_controller.release(pynput.keyboard.KeyCode.from_vk(vk))
        except Exception:
            pass

def handle_home_command(preset):
    print(f"\n {C_GREEN}>>> [COMMAND] HOME / FIT TO SCREEN (Preset: {preset.upper()}) <<<{C_RESET}")
    set_mouse_states(False, False, False, preset)
    if not is_cad_window_active():
        print(f"   {C_GRAY}[Notice] Command skipped: Active window is not 3D CAD{C_RESET}")
        return
    if preset == "fusion360":
        send_key_combo_win32([0x75]) # F6 key
    elif preset == "solidworks":
        send_key_combo_win32([0x46]) # 'F' key (Zoom to Fit)
    elif preset == "blender":
        send_key_combo_win32([0x24]) # Home key
        send_key_combo_win32([0x6E]) # Numpad Period '.'
    elif preset == "freecad":
        send_key_combo_win32([0x75]) # F6 key

def handle_view_plane_command(plane, preset):
    print(f"\n {C_CYAN}>>> [COMMAND] VIEW PLANE: {plane.upper()} (Preset: {preset.upper()}) <<<{C_RESET}")
    set_mouse_states(False, False, False, preset)
    if not is_cad_window_active():
        print(f"   {C_GRAY}[Notice] Command skipped: Active window is not 3D CAD{C_RESET}")
        return

    if preset == "solidworks":
        vk_map = {"x": 0x34, "y": 0x35, "z": 0x31}
        vk = vk_map.get(plane, 0x34)
        send_key_combo_win32([0x11, vk])
    elif preset == "blender":
        np_map = {"x": 0x63, "y": 0x67, "z": 0x61}
        np_vk = np_map.get(plane, 0x63)
        send_key_combo_win32([np_vk])
    elif preset == "fusion360":
        vk_map = {"x": 0x33, "y": 0x31, "z": 0x32}
        vk = vk_map.get(plane, 0x33)
        send_key_combo_win32([0x10, vk])

def handle_undo_command(preset):
    print(f"\n {C_YELLOW}>>> [COMMAND] UNDO (Ctrl+Z) (Preset: {preset.upper()}) <<<{C_RESET}")
    set_mouse_states(False, False, False, preset)
    if not is_cad_window_active():
        print(f"   {C_GRAY}[Notice] Command skipped: Active window is not 3D CAD{C_RESET}")
        return
    send_key_combo_win32([0x11, 0x5A]) # Ctrl + Z

def handle_redo_command(preset):
    print(f"\n {C_PURPLE}>>> [COMMAND] REDO (Preset: {preset.upper()}) <<<{C_RESET}")
    set_mouse_states(False, False, False, preset)
    if not is_cad_window_active():
        print(f"   {C_GRAY}[Notice] Command skipped: Active window is not 3D CAD{C_RESET}")
        return
    if preset == "blender":
        send_key_combo_win32([0x11, 0x10, 0x5A]) # Ctrl + Shift + Z for Blender!
    else:
        send_key_combo_win32([0x11, 0x59]) # Ctrl + Y for Fusion 360 & SolidWorks

z_accumulator = 0
pan_x_accum = 0.0
pan_y_accum = 0.0
orbit_x_accum = 0.0
orbit_y_accum = 0.0

def handle_3d_event(payload):
    global current_preset, last_wheel_time, z_accumulator, pan_x_accum, pan_y_accum, orbit_x_accum, orbit_y_accum
    x = payload.get("x", 0)
    y = payload.get("y", 0)
    z = payload.get("z", 0)
    rx = payload.get("rx", 0)
    rz = payload.get("rz", 0)
    touch_down = payload.get("touch_down", True)
    preset = payload.get("preset", "blender")
    current_preset = preset

    if not is_cad_window_active():
        z_accumulator = 0
        pan_x_accum = 0.0
        pan_y_accum = 0.0
        orbit_x_accum = 0.0
        orbit_y_accum = 0.0
        set_mouse_states(False, False, False, preset)
        return

    if not touch_down or (x == 0 and y == 0 and z == 0 and rx == 0 and rz == 0):
        z_accumulator = 0
        pan_x_accum = 0.0
        pan_y_accum = 0.0
        orbit_x_accum = 0.0
        orbit_y_accum = 0.0
        set_mouse_states(False, False, False, preset)
        return

    if z != 0:
        pan_x_accum = 0.0
        pan_y_accum = 0.0
        orbit_x_accum = 0.0
        orbit_y_accum = 0.0
        set_mouse_states(False, False, False, preset)
        z_accumulator += z

        THRESHOLD = 60
        if abs(z_accumulator) >= THRESHOLD:
            direction = 1 if z_accumulator > 0 else -1
            notch_count = min(2, max(1, int(abs(z_accumulator) / THRESHOLD)))
            z_accumulator -= direction * notch_count * THRESHOLD
            wheel_delta = direction * notch_count * 120

            if USE_WIN32:
                win32api.mouse_event(win32con.MOUSEEVENTF_WHEEL, 0, 0, wheel_delta, 0)
            elif USE_PYNPUT:
                mouse_controller.scroll(0, direction * notch_count)

    elif x != 0 or y != 0:
        z_accumulator = 0
        orbit_x_accum = 0.0
        orbit_y_accum = 0.0

        if preset == "blender":
            set_mouse_states(True, True, False, preset)
        elif preset == "solidworks":
            set_mouse_states(True, False, True, preset)
        else:
            set_mouse_states(True, False, False, preset)

        pan_x_accum += (x / 20.0)
        pan_y_accum += (y / 20.0)

        step_x = int(pan_x_accum)
        step_y = int(pan_y_accum)

        if step_x != 0 or step_y != 0:
            pan_x_accum -= step_x
            pan_y_accum -= step_y
            move_mouse_relative(step_x, step_y)

    elif rx != 0 or rz != 0:
        z_accumulator = 0
        pan_x_accum = 0.0
        pan_y_accum = 0.0

        if preset == "blender" or preset == "solidworks":
            set_mouse_states(True, False, False, preset)
        else:
            set_mouse_states(True, True, False, preset)

        orbit_x_accum += (rz / 20.0)
        orbit_y_accum += (rx / 20.0)

        step_x = int(orbit_x_accum)
        step_y = int(orbit_y_accum)

        if step_x != 0 or step_y != 0:
            orbit_x_accum -= step_x
            orbit_y_accum -= step_y
            move_mouse_relative(step_x, step_y)

def start_udp_server():
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.bind(('0.0.0.0', PORT))
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)

    while True:
        try:
            data, addr = sock.recvfrom(2048)
            if not data:
                continue

            text = data.decode('utf-8', errors='ignore').strip()
            for line in text.split('\n'):
                if not line:
                    continue
                try:
                    payload = json.loads(line)
                    msg_type = payload.get("type")

                    if msg_type == "3d_mouse_discover":
                        beacon = json.dumps({
                            "type": "3d_mouse_pc_beacon",
                            "name": HOSTNAME
                        }).encode('utf-8')
                        sock.sendto(beacon, addr)

                    elif msg_type == "3d_mouse":
                        handle_3d_event(payload)
                    elif msg_type == "3d_mouse_home":
                        preset = payload.get("preset", "fusion360")
                        handle_home_command(preset)
                    elif msg_type == "3d_mouse_view_plane":
                        plane = payload.get("plane", "x")
                        preset = payload.get("preset", "fusion360")
                        handle_view_plane_command(plane, preset)
                    elif msg_type == "3d_mouse_undo":
                        preset = payload.get("preset", "fusion360")
                        handle_undo_command(preset)
                    elif msg_type == "3d_mouse_redo":
                        preset = payload.get("preset", "fusion360")
                        handle_redo_command(preset)

                except json.JSONDecodeError:
                    pass
        except Exception as e:
            time.sleep(0.01)

def handle_tcp_client(client_sock):
    buffer = ""
    while True:
        try:
            data = client_sock.recv(2048)
            if not data:
                break
            buffer += data.decode('utf-8', errors='ignore')
            while '\n' in buffer:
                line, buffer = buffer.split('\n', 1)
                line = line.strip()
                if line:
                    try:
                        payload = json.loads(line)
                        msg_type = payload.get("type")
                        if msg_type == "3d_mouse":
                            handle_3d_event(payload)
                        elif msg_type == "3d_mouse_home":
                            preset = payload.get("preset", "fusion360")
                            handle_home_command(preset)
                        elif msg_type == "3d_mouse_view_plane":
                            plane = payload.get("plane", "x")
                            preset = payload.get("preset", "fusion360")
                            handle_view_plane_command(plane, preset)
                        elif msg_type == "3d_mouse_undo":
                            preset = payload.get("preset", "fusion360")
                            handle_undo_command(preset)
                        elif msg_type == "3d_mouse_redo":
                            preset = payload.get("preset", "fusion360")
                            handle_redo_command(preset)
                    except json.JSONDecodeError:
                        pass
        except Exception:
            break
    client_sock.close()

def start_tcp_server():
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind(('0.0.0.0', PORT))
    server.listen(5)
    while True:
        try:
            client_sock, addr = server.accept()
            client_sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
            threading.Thread(target=handle_tcp_client, args=(client_sock,), daemon=True).start()
        except Exception:
            time.sleep(0.1)

if __name__ == "__main__":
    udp_temp_thread = threading.Thread(target=start_udp_server, daemon=True)
    udp_temp_thread.start()

    tcp_temp_thread = threading.Thread(target=start_tcp_server, daemon=True)
    tcp_temp_thread.start()

    print(f" {C_GREEN}[SERVER RUNNING] Move your finger on phone screen to orbit 3D viewports!{C_RESET}\n")
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print(f"\n {C_YELLOW}Stopping PC Receiver. Released MMB.{C_RESET}")
        set_mouse_states(False, False, False, current_preset)
