package wd.samauto.testshtrixscale

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import wd.pos.slimscale.IScale
import wd.pos.slimscale.SlimScale

class MainActivity : AppCompatActivity() {
    lateinit var slimScale: IScale

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
        startBtn.setOnClickListener { slimScale.start() }
        stopBtn.setOnClickListener { slimScale.stop() }
        clearBtn.setOnClickListener { slimScale.clear() }
        setTareBtn.setOnClickListener { slimScale.setTare() }
        setZeroBtn.setOnClickListener { slimScale.setZero() }
    }

    private fun init() {
        slimScale = SlimScale(applicationContext).apply {
            updateMillis = 200
            onWeightChange {
                runOnUiThread {
                    weightTxt.text = it.toString()
                }
            }
        }
    }

    override fun onStop() {
        slimScale.stop()
        super.onStop()
    }
}
