package com.example.a3dmausecad.bluetooth

/**
 * Defines HID Descriptors and Report formatting for a 3D Mouse / Multi-Axis Controller.
 */
object HidDescriptors {

    /**
     * HID Report Descriptor for a 6-DOF (Degrees of Freedom) 3D Controller.
     * Reports:
     * - Report ID: 1
     * - Translation axes: X, Y, Z (16-bit signed short)
     * - Rotation axes: Rx, Ry, Rz (16-bit signed short)
     */
    val THREE_D_MOUSE_DESCRIPTOR = byteArrayOf(
        0x05.toByte(), 0x01.toByte(),        // USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x08.toByte(),        // USAGE (Multi-axis Controller)
        0xA1.toByte(), 0x01.toByte(),        // COLLECTION (Application)
        0x85.toByte(), 0x01.toByte(),        //   REPORT_ID (1)

        // Translation axes: X, Y, Z
        0x05.toByte(), 0x01.toByte(),        //   USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x30.toByte(),        //   USAGE (X)
        0x09.toByte(), 0x31.toByte(),        //   USAGE (Y)
        0x09.toByte(), 0x32.toByte(),        //   USAGE (Z)
        0x15.toByte(), 0x81.toByte(), 0x80.toByte(), // LOGICAL_MINIMUM (-32767)
        0x25.toByte(), 0x7F.toByte(), 0x7F.toByte(), // LOGICAL_MAXIMUM (32767)
        0x75.toByte(), 0x10.toByte(),        //   REPORT_SIZE (16 bits)
        0x95.toByte(), 0x03.toByte(),        //   REPORT_COUNT (3)
        0x81.toByte(), 0x02.toByte(),        //   INPUT (Data,Var,Abs)

        // Rotation axes: Rx, Ry, Rz
        0x09.toByte(), 0x33.toByte(),        //   USAGE (Rx)
        0x09.toByte(), 0x34.toByte(),        //   USAGE (Ry)
        0x09.toByte(), 0x35.toByte(),        //   USAGE (Rz)
        0x15.toByte(), 0x81.toByte(), 0x80.toByte(), // LOGICAL_MINIMUM (-32767)
        0x25.toByte(), 0x7F.toByte(), 0x7F.toByte(), // LOGICAL_MAXIMUM (32767)
        0x75.toByte(), 0x10.toByte(),        //   REPORT_SIZE (16 bits)
        0x95.toByte(), 0x03.toByte(),        //   REPORT_COUNT (3)
        0x81.toByte(), 0x02.toByte(),        //   INPUT (Data,Var,Abs)

        0xC0.toByte()                         // END_COLLECTION
    )

    const val REPORT_ID_3D_MOUSE: Byte = 1

    /**
     * Builds a 12-byte HID report array for Report ID 1 (3D Controller).
     * Values are 16-bit signed integers (-32767..32767) encoded in Little Endian.
     */
    fun createReport(
        x: Short = 0,
        y: Short = 0,
        z: Short = 0,
        rx: Short = 0,
        ry: Short = 0,
        rz: Short = 0
    ): ByteArray {
        val report = ByteArray(12)

        // X Axis
        report[0] = (x.toInt() and 0xFF).toByte()
        report[1] = ((x.toInt() shr 8) and 0xFF).toByte()

        // Y Axis
        report[2] = (y.toInt() and 0xFF).toByte()
        report[3] = ((y.toInt() shr 8) and 0xFF).toByte()

        // Z Axis
        report[4] = (z.toInt() and 0xFF).toByte()
        report[5] = ((z.toInt() shr 8) and 0xFF).toByte()

        // Rx Axis (Pitch)
        report[6] = (rx.toInt() and 0xFF).toByte()
        report[7] = ((rx.toInt() shr 8) and 0xFF).toByte()

        // Ry Axis (Roll)
        report[8] = (ry.toInt() and 0xFF).toByte()
        report[9] = ((ry.toInt() shr 8) and 0xFF).toByte()

        // Rz Axis (Yaw)
        report[10] = (rz.toInt() and 0xFF).toByte()
        report[11] = ((rz.toInt() shr 8) and 0xFF).toByte()

        return report
    }
}
