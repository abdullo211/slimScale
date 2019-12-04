package wd.pos.slimscale

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import java.util.*
import kotlin.concurrent.timer
import kotlin.math.abs

class SlimScale(private val context: Context) : IScale {

    private var timer: Timer? = null
    private var serial: UsbSerialDevice? = null
    override var updateMillis: Long = 3000
    private var onWeightChange: ((Double) -> Unit)? = null
    private var lastWeight: Double = -1.0

    companion object {
        private const val ACTION_USB_PERMISSION = "wd.pos.slimScale.USB_PERMISSION"
        private const val SHTRIH_M_DEVICE_ID = 8137
        private const val TIMER_NAME = "weight_timer"
        private val GET_WEIGHT_CODE = byteArrayOf(0X02, 0X05, 0X3A, 0X30, 0X30, 0X33, 0X30, 0X3C)
    }

    val isUsbConnected get() = serial?.isOpen ?: false

    override fun onWeightChange(scaleChanged: (weight: Double) -> Unit) {
        this.onWeightChange = scaleChanged
    }

    override fun setTare() {

    }

    override fun clear() {
        stop()
        this.onWeightChange = null
    }

    override fun start() {
        if (!isUsbConnected)
            connectUsb()
        startTimer()
    }

    override fun stop() {
        timer?.cancel()
        onWeightChange?.invoke(-1.0)
    }

    private fun startTimer() {
        timer = timer(TIMER_NAME, period = updateMillis) {
            serial?.write(GET_WEIGHT_CODE)
        }
    }

    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            if (UsbManager.ACTION_USB_DEVICE_DETACHED == intent.action) {
                val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                device?.apply {

                }
            }
        }
    }

    private fun connectUsb() {
        try {
            val manager = context.getSystemService(AppCompatActivity.USB_SERVICE) as UsbManager?
            val mDevice =
                manager?.deviceList?.values?.firstOrNull { it.vendorId == SHTRIH_M_DEVICE_ID }
                    ?: return
            if (!checkDevicePermission(manager, mDevice)) return
            val connection = manager.openDevice(mDevice)
            serial = UsbSerialDevice.createUsbSerialDevice(mDevice, connection)
            serial?.let { !openSerialPort(it) } ?: return
            serial?.read { it?.let { bytes -> onDataReceived(bytes) } }
        } catch (ex: Exception) {
            Log.e("Error connect scale", "Cannot connect scale", ex.cause)
        }
    }

    private fun onDataReceived(bytes: ByteArray) {
        if (bytes.size < 12) return

        when (abs(bytes[1].toInt())) {
            11 -> onWeight(bytes)
        }
        Log.d(
            "content",
            bytes.map { arr -> (arr * 255).toByte() }.toByteArray().contentToString()
        )
    }

    private fun openSerialPort(serial: UsbSerialDevice): Boolean {
        if (serial.isOpen) return true
        if (!serial.isOpen && serial.open()) {
            serial.setBaudRate(9600)
            serial.setDataBits(UsbSerialInterface.DATA_BITS_8)
            serial.setStopBits(UsbSerialInterface.STOP_BITS_1)
            serial.setParity(UsbSerialInterface.PARITY_NONE)
            return true
        }
        return false
    }

    private fun onWeight(bytes: ByteArray) {
        val first = bytes[6].toInt()
        val second = bytes[7].toInt()
        val pow = when {
            first == 0 -> second
            first > 0 -> second
            else -> second + 1
        }
        val weight: Double =
            (first + 256 * pow).toDouble() / 1000
        if (lastWeight != weight) {
            lastWeight = weight
            onWeightChange?.invoke(lastWeight)
        }
    }

    private fun checkDevicePermission(manager: UsbManager, usbDevice: UsbDevice): Boolean {
        if (manager.hasPermission(usbDevice)) return true
        val permissionIntent =
            PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), 0)
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        context.registerReceiver(usbReceiver, filter)
        manager.requestPermission(usbDevice, permissionIntent)
        return false
    }
}