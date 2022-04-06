package iam.thevoid.epic.timeapp

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.text.DateFormat
import java.util.*
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

class MainActivity : AppCompatActivity() {

    // Часы:
    private lateinit var clockText: TextView

    // Обратный отсчёт
    private lateinit var countdownText: TextView
    private lateinit var countdownSecondsEditText: EditText
    private lateinit var countdownStartButton: Button

    // Секундомер
    private lateinit var stopwatchText: TextView
    private lateinit var stopwatchMillisText: TextView
    private lateinit var stopwatchStartButton: Button
    private lateinit var stopwatchEndButton: Button

    // Счётчики
    private var subscriptionClock : Disposable? = null
    private var subscriptionCountdown : Disposable? = null
    private var subscriptionStopwatch : Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clockText = findViewById(R.id.clockText)

        subscriptionClock?.dispose()
        subscriptionClock = Observable
            .interval(1, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                clockText.text = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Calendar.getInstance().time).replace(" ", "\n")
            }

        countdownText = findViewById(R.id.countdownText)
        countdownSecondsEditText = findViewById(R.id.countdownEditText)
        countdownStartButton = findViewById(R.id.countdownStartButton)

        countdownStartButton.setOnClickListener{
            var seconds : Int? = countdownSecondsEditText.text.toString().toIntOrNull()
            if (seconds != null && seconds > 0) {
                countdownText.setTextColor(Color.BLACK)
                countdownText.text = seconds.toString()
                subscriptionCountdown?.dispose()
                subscriptionCountdown = Observable
                    .interval(1, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .takeWhile {
                        seconds-- > 0
                    }
                    .subscribe {
                        countdownText.text = seconds.toString()
                        if (seconds == 0) countdownText.setTextColor(Color.RED)
                    }

            }
        }

        stopwatchText = findViewById(R.id.stopwatchText)
        stopwatchMillisText = findViewById(R.id.stopwatchMillisText)
        stopwatchStartButton = findViewById(R.id.stopwatchStartButton)
        stopwatchEndButton = findViewById(R.id.stopwatchEndButton)
        var ticks = 0
        var ticksBeforePause = 0
        var stopped = false

        fun startStopwatch() {
            subscriptionStopwatch?.dispose()
            stopped = false
            stopwatchEndButton.text = getString(R.string.button_Pause)
            subscriptionStopwatch = Observable
                .interval(1, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .takeWhile {
                    !stopped
                }
                .subscribe{
                    ticks = ticksBeforePause + it.toInt()
                    stopwatchText.text = (ticks / 1000).toString()
                    stopwatchMillisText.text = (ticks % 1000).toString()
                }
        }

        stopwatchStartButton.setOnClickListener {
            if (!stopped) {
                ticksBeforePause = 0
                startStopwatch()
            } else {
                stopped = false
                stopwatchEndButton.text = getString(R.string.button_Pause)
                stopwatchStartButton.text = getString(R.string.button_Start)
                startStopwatch()
            }
        }

        stopwatchEndButton.setOnClickListener {
            if (ticks == 0) return@setOnClickListener
            if (!stopped) {
                ticksBeforePause = ticks
                stopped = true
                stopwatchEndButton.text = getString(R.string.button_Reset)
                stopwatchStartButton.text = getString(R.string.button_Resume)
            } else {
                ticksBeforePause = 0
                stopped = false
                stopwatchEndButton.text = getString(R.string.button_Pause)
                stopwatchStartButton.text = getString(R.string.button_Start)
                startStopwatch()
            }
        }

    }

    override fun onDestroy() {
        subscriptionClock?.dispose()
        subscriptionCountdown?.dispose()
        subscriptionStopwatch?.dispose()
        super.onDestroy()
    }
}