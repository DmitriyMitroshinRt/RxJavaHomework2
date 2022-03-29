package iam.thevoid.epic.timeapp

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.Disposable
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

// Дан экран с готовой разметкой
// Реализовать при помощи RxJava
// 1) Отображение часов,начинают работать при старте, показывают время в любом удобном формате.
//    Как пример можно использовать формат из разметки
// 2) Таймер
//    а) Пользователь вводит количество секунд в поле
//    б) По нажатию на "Старт" начинается обратный отсчёт
//    в) (не обязательно) По окончании таймер каким либо образом сигнализирует об окончании,
//       например область таймера вспыхивает ярким цветом
// 3) Секундомер
//    а) Пользователь нажимает на "Старт", начинается отсчёт времени. В соответствующие текстовые
//       поля выводится количество прошедшего времени (отдельно время с точностью до секунд,
//       отдельно миллисекунды)
//    б) По нажатию на паузу отсчёт времени останавливается. Кнопка "Пауза" превращается в кнопку
//       "Сброс".
//    в) По нажатию на "Сброс" отстчёт времени сбрасывается в 0. "Старт" продолжает приостановленный
//       отсчёт
//    г) (не обязательно) Можно сделать изменение состояние кнопки "Старт" на "Продолжить" для
//       состояния паузы

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : AppCompatActivity() {

    // Часы:
    private lateinit var clockText: TextView

    // Обратный отсчёт
    private lateinit var countdownText: TextView
    private lateinit var countdownSecondsEditText: EditText
    private lateinit var countdownStartButton: Button
    private var countdownRunning: Boolean = false
    private var countdownDisposable: Disposable? = null

    // Секундомер
    private lateinit var stopwatchText: TextView
    private lateinit var stopwatchMillisText: TextView
    private lateinit var stopwatchStartButton: Button
    private lateinit var stopwatchEndButton: Button
    private var stopwatchStatus = StopwatchStatus.INIT
    private var stopwatchDisposable: Disposable? = null
    private var stopwatchTime: LocalTime = LocalTime.ofSecondOfDay(0)
    private val stopwatchFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val stopwatchFormatterMillis = DateTimeFormatter.ofPattern(".S")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clockText = findViewById(R.id.clockText)
        initClock()

        countdownText = findViewById(R.id.countdownText)
        countdownSecondsEditText = findViewById(R.id.countdownEditText)
        countdownStartButton = findViewById(R.id.countdownStartButton)
        initCountdown()

        stopwatchText = findViewById(R.id.stopwatchText)
        stopwatchMillisText = findViewById(R.id.stopwatchMillisText)
        stopwatchStartButton = findViewById(R.id.stopwatchStartButton)
        stopwatchEndButton = findViewById(R.id.stopwatchEndButton)
        initStopwatch()
    }

    private fun initClock() {

        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")

        Flowable.interval(1, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { LocalTime.now().format(formatter).also { clockText.text = it } }
    }

    private fun initCountdown() {
        countdownStartButton.setOnClickListener {
            if (!countdownRunning) {
                if (countdownSecondsEditText.text.isNotEmpty()) {
                    val countdownSeconds: Long = countdownSecondsEditText.text.toString().toLong()
                    if (countdownSeconds > 0 && countdownSeconds < (24 * 60 * 60 - 1)) {
                        startCountDown(countdownSeconds)
                        countdownRunning = true
                        countdownRunningState()
                    }
                }
            } else {
                countdownInitState()
                countdownRunning = false
                countdownDisposable?.dispose()
            }
        }
    }

    private fun startCountDown(countdownSeconds: Long) {

        val formatter = DateTimeFormatter.ofPattern("mm:ss")

        var time = LocalTime.ofSecondOfDay(countdownSeconds)
        time.format(formatter).also { countdownText.text = it }

        countdownDisposable = Flowable.intervalRange(0, countdownSeconds, 1L, 1L, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnComplete { countdownFinishedState() }
            .map { time = time.minusSeconds(1) }
            .subscribe { time.format(formatter).also { countdownText.text = it } }
    }

    private fun countdownInitState() {
        countdownStartButton.text = "START"
        countdownText.text = ""
        countdownSecondsEditText.text.clear()
        countdownSecondsEditText.isEnabled = true
    }

    private fun countdownRunningState() {
        countdownStartButton.text = "RESET"
        countdownSecondsEditText.text.clear()
        countdownSecondsEditText.isEnabled = false
        countdownText.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun countdownFinishedState() {
        countdownText.setBackgroundColor(Color.DKGRAY)
    }

    private fun initStopwatch() {

        stopwatchStartButton.setOnClickListener {
            if (stopwatchStatus != StopwatchStatus.RUNNING) {
                stopwatchRunningState()
            }
        }

        stopwatchEndButton.setOnClickListener {
            if (stopwatchStatus == StopwatchStatus.RUNNING) {
                stopwatchPauseState()
            } else if (stopwatchStatus == StopwatchStatus.PAUSE) {
                stopwatchInitState()
            }
        }
    }

    private fun stopwatchInitState() {
        stopwatchStatus = StopwatchStatus.INIT

        stopwatchTime = LocalTime.ofSecondOfDay(0)

        stopwatchText.text = ""
        stopwatchMillisText.text = ""

        stopwatchStartButton.text = "START"
        stopwatchEndButton.text = "PAUSE"
    }

    private fun stopwatchPauseState() {
        stopwatchStatus = StopwatchStatus.PAUSE

        stopwatchDisposable?.dispose()

        stopwatchStartButton.text = "CONTINUE"
        stopwatchEndButton.text = "RESET"
    }

    private fun stopwatchRunningState() {
        stopwatchStatus = StopwatchStatus.RUNNING
        stopwatchTime.format(stopwatchFormatter)
            .also { stopwatchText.text = it }
        stopwatchTime.format(stopwatchFormatterMillis)
            .also { stopwatchMillisText.text = it }

        stopwatchDisposable = Flowable.interval(100, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .map { stopwatchTime = stopwatchTime.plus(100, ChronoUnit.MILLIS) }
            .doOnNext {
                stopwatchTime.format(stopwatchFormatterMillis)
                    .also { stopwatchMillisText.text = it }
            }
            .filter {
                stopwatchTime.get(ChronoField.MILLI_OF_SECOND) == 0
            }
            .doOnNext {
                stopwatchTime.format(stopwatchFormatter)
                    .also { stopwatchText.text = it }
            }
            .subscribe()

        stopwatchStartButton.text = "START"
        stopwatchEndButton.text = "PAUSE"
    }

    private enum class StopwatchStatus {
        INIT,
        RUNNING,
        PAUSE
    }

}