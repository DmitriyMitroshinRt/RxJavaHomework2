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
import java.text.SimpleDateFormat
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
    private var timerDisposable: Disposable? = null
    private var pausedStopWatchVal: Long = 0
    private var currentStopWatchVal: Long = 0
    private val stopWatchDisposables: MutableList<Disposable> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        clockText = findViewById(R.id.clockText)
        subscribeClock()

        countdownText = findViewById(R.id.countdownText)
        countdownSecondsEditText = findViewById(R.id.countdownEditText)
        countdownStartButton = findViewById(R.id.countdownStartButton)

        countdownStartButton.setOnClickListener {
            startTimerDown()
        }

        stopwatchText = findViewById(R.id.stopwatchText)
        stopwatchMillisText = findViewById(R.id.stopwatchMillisText)
        stopwatchStartButton = findViewById(R.id.stopwatchStartButton)
        stopwatchEndButton = findViewById(R.id.stopwatchEndButton)

        stopwatchStartButton.setOnClickListener {
            startTimerUp()
        }

        stopwatchEndButton.setOnClickListener {
            pauseTimerUp()
        }

    }

    private fun subscribeClock() {
        Observable.interval(1000, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { clockText.text = getCurrentTime() },
                {}
            )

    }

    private fun getCurrentTime(): String {
        val calendar: Calendar = Calendar.getInstance()
        val simpleDateFormat = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
        return simpleDateFormat.format(calendar.time)
    }

    private fun startTimerDown() {
        timerDisposable?.dispose()
        val text = countdownSecondsEditText.text.toString()
        if (text == "") {
            countdownText.text = "Введите количество"
        } else {
            val amountSec = text.toLong()
            var currentAmountSec = amountSec
            countdownText.setBackgroundColor(Color.WHITE)
            timerDisposable =
                Observable.intervalRange(amountSec, amountSec + 1, 0, 1000, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnComplete { countdownText.setBackgroundColor(Color.BLUE) }
                    .subscribe(
                        {
                            countdownText.text = secondsToTimeString(currentAmountSec)
                            currentAmountSec--
                        },
                        {}
                    )
        }
    }

    private fun startTimerUp() {
        if (stopWatchDisposables.isEmpty()) {
            val observable: Observable<Long> = Observable.interval(1, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .doOnDispose { pausedStopWatchVal = currentStopWatchVal }
                .map { it + pausedStopWatchVal }
                .doOnNext { currentStopWatchVal = it }

            stopWatchDisposables.add(observable.observeOn(AndroidSchedulers.mainThread())
                .subscribe { stopwatchMillisText.text = "${it % 1000}" })

            stopWatchDisposables.add(observable
                .filter { it % 1000 == 0L }
                .map { it / 1000 }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    stopwatchText.text = secondsToTimeString(it)
                })
            stopWatchStartedPerformance()
        }
    }

    private fun secondsToTimeString(seconds: Long): String {
        return "${seconds / 3600}:${(seconds % 3600) / 60}:${(seconds % 3600) % 60}"
    }

    private fun pauseTimerUp() {
        stopWatchDispose()
        if (currentStopWatchVal == 0L) {
            pausedStopWatchVal = 0
            stopWatchStoppedPerformance()
        } else {
            stopWatchPausedPerformance()
        }
        currentStopWatchVal = 0
    }

    private fun stopWatchDispose() {
        stopWatchDisposables.forEach {
            it.dispose()
        }
        stopWatchDisposables.clear()
    }

    private fun stopWatchStartedPerformance() {
        stopwatchEndButton.text = "Pause"
        stopwatchStartButton.text = "Started"
    }

    private fun stopWatchPausedPerformance() {
        stopwatchEndButton.text = "Stop"
        stopwatchStartButton.text = "Continue"
    }

    private fun stopWatchStoppedPerformance() {
        stopwatchText.text = secondsToTimeString(0)
        stopwatchMillisText.text = "0"
        stopwatchEndButton.text = "Stop"
        stopwatchStartButton.text = "Start"
    }
}