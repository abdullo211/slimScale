package wd.pos.slimscale

interface IScale {
    var updateMillis: Long
    fun onWeightChange(scaleChanged: (weight: Double) -> Unit)
    val isUsbConnected:Boolean
    fun setTare()
    fun setZero()
    fun start()
    fun stop()
    fun clear()
}