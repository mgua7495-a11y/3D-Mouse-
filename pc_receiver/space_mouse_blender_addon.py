bl_info = {
    "name": "3D Mouse CAD Viewport Controller",
    "author": "3D Mouse CAD",
    "version": (1, 0),
    "blender": (3, 0, 0),
    "location": "View3D > Sidebar > 3D Mouse",
    "description": "Receive 3D Mouse touch inputs from Android app to orbit Blender 3D Viewport",
    "category": "3D View",
}

import bpy
import socket
import json
import threading
import mathutils

# Blender Native 3D Viewport Addon for 3D Mouse CAD
# Direct Viewport Matrix Rotation with 0 Lag

class Mouse3DReceiverThread(threading.Thread):
    def __init__(self):
        super().__init__()
        self.daemon = True
        self.running = True
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.sock.bind(('0.0.0.0', 9876))
        self.sock.settimeout(0.5)

    def run(self):
        while self.running:
            try:
                data, addr = self.sock.recvfrom(2048)
                if data:
                    text = data.decode('utf-8', errors='ignore').strip()
                    for line in text.split('\n'):
                        if not line:
                            continue
                        try:
                            payload = json.loads(line)
                            if payload.get("type") == "3d_mouse":
                                rx = payload.get("rx", 0)
                                rz = payload.get("rz", 0)
                                if rx != 0 or rz != 0:
                                    bpy.app.timers.register(lambda: rotate_blender_viewport(rx, rz))
                        except Exception:
                            pass
            except socket.timeout:
                pass
            except Exception:
                pass

    def stop(self):
        self.running = False
        try:
            self.sock.close()
        except Exception:
            pass

receiver_thread = None

def rotate_blender_viewport(rx, rz):
    for area in bpy.context.screen.areas:
        if area.type == 'VIEW_3D':
            for space in area.spaces:
                if space.type == 'VIEW_3D':
                    rv3d = space.region_3d
                    if rv3d:
                        # Convert rx, rz into pitch and yaw Euler rotations
                        pitch = (rx / 500.0) * (3.14159 / 180.0)
                        yaw = (rz / 500.0) * (3.14159 / 180.0)

                        rot_pitch = mathutils.Quaternion((1, 0, 0), pitch)
                        rot_yaw = mathutils.Quaternion((0, 0, 1), yaw)

                        rv3d.view_rotation = rot_yaw @ rv3d.view_rotation @ rot_pitch
                        area.tag_redraw()
                        break
    return None

class MOUSE3D_OT_start_server(bpy.types.Operator):
    bl_idname = "mouse3d.start_server"
    bl_label = "Start 3D Mouse Server"

    def execute(self, context):
        global receiver_thread
        if receiver_thread is None or not receiver_thread.is_alive():
            receiver_thread = Mouse3DReceiverThread()
            receiver_thread.start()
            self.report({'INFO'}, "3D Mouse Server Started on Port 9876")
        return {'FINISHED'}

class MOUSE3D_OT_stop_server(bpy.types.Operator):
    bl_idname = "mouse3d.stop_server"
    bl_label = "Stop 3D Mouse Server"

    def execute(self, context):
        global receiver_thread
        if receiver_thread:
            receiver_thread.stop()
            receiver_thread = None
            self.report({'INFO'}, "3D Mouse Server Stopped")
        return {'FINISHED'}

class MOUSE3D_PT_panel(bpy.types.Panel):
    bl_label = "3D Mouse CAD"
    bl_space_type = 'VIEW_3D'
    bl_region_type = 'UI'
    bl_category = "3D Mouse"

    def draw(self, context):
        layout = self.layout
        layout.operator("mouse3d.start_server", text="Start Receiver")
        layout.operator("mouse3d.stop_server", text="Stop Receiver")

def register():
    bpy.utils.register_class(MOUSE3D_OT_start_server)
    bpy.utils.register_class(MOUSE3D_OT_stop_server)
    bpy.utils.register_class(MOUSE3D_PT_panel)

def unregister():
    global receiver_thread
    if receiver_thread:
        receiver_thread.stop()
        receiver_thread = None
    bpy.utils.unregister_class(MOUSE3D_OT_start_server)
    bpy.utils.unregister_class(MOUSE3D_OT_stop_server)
    bpy.utils.unregister_class(MOUSE3D_PT_panel)

if __name__ == "__main__":
    register()
