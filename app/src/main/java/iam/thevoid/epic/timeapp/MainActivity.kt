package iam.thevoid.epic.timeapp

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.format.DateUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
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

    enum class StopwatchState { RUN, STOP, PAUSE }

    private var disposableTimer: Disposable? = null
    private var disposableStopwatch: Disposable? = null

    private fun disposeTimer() {
        disposableTimer?.dispose()
        disposableTimer = null
    }

    private fun disposeStopwatch() {
        disposableStopwatch?.dispose()
        disposableStopwatch = null
    }


    @SuppressLint("pop")
    override fun onStart() {
        super.onStart()

        /**
         * Таймер
         */
        countdownStartButton.setOnClickListener {
            disposeTimer()
            val input = countdownSecondsEditText.text.toString()
                .toLongOrNull() ?: 0L
            disposableTimer = Observable.interval(1, TimeUnit.SECONDS, Schedulers.io())
                .takeUntil { it >= input }
                .map { s ->
                    DateUtils
                        .formatElapsedTime(input - s)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { i ->
                        countdownText.text = i
                        countdownText.setTextColor(Color.BLACK)
                    },
                    {}
                )
                {
                    disposeTimer()
                    countdownText.setTextColor(Color.RED)
                    countdownSecondsEditText.text = null
                }
        }

        /**
         * Секундомер
         */
        var stopwatchState = StopwatchState.STOP
        var milliseconds = 0L
        stopwatchStartButton.setOnClickListener {
            when (stopwatchState) {
                StopwatchState.PAUSE, StopwatchState.STOP -> {
                    stopwatchState = StopwatchState.RUN
                    stopwatchStartButton.text = "START"
                    stopwatchEndButton.text = "PAUSE"
                }
                StopwatchState.RUN -> {
                    milliseconds = 0L
                    disposeStopwatch()
                }
            }
            //disposeStopwatch()
            disposableStopwatch = Observable.interval(1, TimeUnit.MILLISECONDS, Schedulers.io())
                .takeWhile { stopwatchState == StopwatchState.RUN }
                .map { milliseconds + it }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    stopwatchText.text = LocalTime.ofSecondOfDay(it / 1000)
                        .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                    stopwatchMillisText.text = (it % 1000).toString()
                }
        }

        stopwatchEndButton.setOnClickListener {
            disposeStopwatch()
            when (stopwatchState) {
                StopwatchState.RUN -> {
                    stopwatchState = StopwatchState.PAUSE
                    stopwatchStartButton.text = "RUN"
                    stopwatchEndButton.text = "STOP"
                    milliseconds = LocalTime.parse(
                        stopwatchText.text,
                        DateTimeFormatter.ofPattern("HH:mm:ss")
                    ).toSecondOfDay() * 1000L
                }
                StopwatchState.PAUSE -> {
                    stopwatchState = StopwatchState.STOP
                    milliseconds = 0L
                    stopwatchText.setText("00:00:00")
                    stopwatchMillisText.setText("00")
                    stopwatchStartButton.text = "START"
                    stopwatchEndButton.text = "PAUSE"
                    disposeStopwatch()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        disposeTimer()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        clockText = findViewById(R.id.clockText)
        countdownText = findViewById(R.id.countdownText)
        countdownSecondsEditText = findViewById(R.id.countdownEditText)
        countdownStartButton = findViewById(R.id.countdownStartButton)
        stopwatchText = findViewById(R.id.stopwatchText)
        stopwatchMillisText = findViewById(R.id.stopwatchMillisText)
        stopwatchStartButton = findViewById(R.id.stopwatchStartButton)
        stopwatchEndButton = findViewById(R.id.stopwatchEndButton)

        /**
         * Часы
         */
        Observable.interval(1, TimeUnit.SECONDS)
            .map {
                SimpleDateFormat.getTimeInstance()
                    .format(Calendar.getInstance().time)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { i -> clockText.text = i }
    }
}