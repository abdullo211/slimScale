package wd.pos.slimscale

interface IScale {
    var updateMillis: Long
    fun onWeightChange(scaleChanged: (weight: Double) -> Unit)
    fun start()
    fun stop()
    fun clear()
}