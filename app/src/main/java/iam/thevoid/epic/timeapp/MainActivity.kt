package iam.thevoid.epic.timeapp

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
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
    private lateinit var timerLayoutEdit: FrameLayout

    // Секундомер
    private lateinit var stopwatchText: TextView
    private lateinit var stopwatchMillisText: TextView
    private lateinit var stopwatchStartButton: Button
    private lateinit var stopwatchEndButton: Button

    @SuppressLint("SimpleDateFormat")
    val dateFormat = SimpleDateFormat("HH:mm:ss")
    val formatTimer = DateTimeFormatter.ofPattern("mm:ss")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clockText = findViewById(R.id.clockText)
        getNowTime()

        countdownText = findViewById(R.id.countdownText)
        countdownSecondsEditText = findViewById(R.id.countdownEditText)
        countdownStartButton = findViewById(R.id.countdownStartButton)
        timerLayoutEdit = findViewById(R.id.timerLayout)

        countdownStartButton.setOnClickListener {
            var secondText = countdownSecondsEditText.text.toString()
            if (!secondText.isEmpty()) {
                countdownStartButton.isEnabled = false
                countdownSecondsEditText.isEnabled = false
                val secondEdit = secondText.toLong()
                var ofSecondOfDay = LocalTime.ofSecondOfDay(secondEdit)
                countdownText.setText(
                    ofSecondOfDay.format(formatTimer)
                )
                timer(ofSecondOfDay)
            }
        }

        stopwatchText = findViewById(R.id.stopwatchText)
        stopwatchMillisText = findViewById(R.id.stopwatchMillisText)
        stopwatchStartButton = findViewById(R.id.stopwatchStartButton)
        stopwatchEndButton = findViewById(R.id.stopwatchEndButton)
        stopwatchText.setText("00:00:00")
        stopwatchMillisText.setText("00")
        var subscribe: Disposable? = null

        var stopwatchState = StopwatchState.STOP
        var millisecondsPassed = 0L
        stopwatchStartButton.setOnClickListener {
            when (stopwatchState) {
                StopwatchState.STOP, StopwatchState.PAUSE -> {
                    stopwatchState = StopwatchState.RUN
                    stopwatchEndButton.text = "PAUSE"
                    stopwatchStartButton.text = "START"
                    stopwatchStartButton.isEnabled = false
                }
                StopwatchState.RUN -> {
                    millisecondsPassed = 0L
                    subscribe?.dispose()
                }
            }

            subscribe = Observable.interval(1, TimeUnit.MILLISECONDS, Schedulers.computation())
                .takeWhile { stopwatchState == StopwatchState.RUN }
                .map { millisecondsPassed + it }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    var secondNow = LocalTime.ofSecondOfDay(it / 1000)
                    stopwatchText.setText(secondNow.format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                    stopwatchMillisText.setText((it % 1000).toString())
                }
        }

        stopwatchEndButton.setOnClickListener {
            when (stopwatchState) {
                StopwatchState.RUN -> {
                    stopwatchState = StopwatchState.PAUSE
                    stopwatchStartButton.isEnabled = true
                    stopwatchStartButton.text = "CONTINUE"
                    stopwatchEndButton.text = "RESET"
                    millisecondsPassed = LocalTime.parse(stopwatchText.text, DateTimeFormatter.ofPattern("HH:mm:ss")).toSecondOfDay() * 1000L
                }
                StopwatchState.PAUSE -> {
                    stopwatchState = StopwatchState.STOP
                    millisecondsPassed = 0L
                    stopwatchText.setText("00:00:00")
                    stopwatchMillisText.setText("00")
                    stopwatchStartButton.text = "START"
                    subscribe?.dispose()
                }
            }
        }

    }

    fun getNowTime() {
        Observable.interval(1, TimeUnit.SECONDS, Schedulers.computation())
            .map {
                dateFormat.format(Date())
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { clockText.setText(it) }
    }

    private fun timer(ofSecondOfDay: LocalTime) {
        var ofSecondOfDay1 = ofSecondOfDay
        Observable.interval(1, TimeUnit.SECONDS, Schedulers.computation())
            .takeWhile { ofSecondOfDay1.toSecondOfDay() > 0 }
            .map { ofSecondOfDay1 = ofSecondOfDay1.minusSeconds(1) }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnDispose { }
            .subscribe(
                { countdownText.setText(ofSecondOfDay1.format(formatTimer)) },
                { Log.d(LOG_TAG, it.message.orEmpty()) },
                {
                    Observable.intervalRange(
                        0L,
                        3,
                        100L,
                        200L,
                        TimeUnit.MILLISECONDS,
                        Schedulers.computation()
                    )
                        .map {
                            if (it % 2L != 0L)
                                Color.GREEN
                            else Color.YELLOW
                        }
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnComplete {
                            countdownStartButton.isEnabled = true
                            countdownSecondsEditText.isEnabled = true
                            timerLayoutEdit.setBackgroundColor(Color.WHITE)
                        }
                        .subscribe {
                            timerLayoutEdit.setBackgroundColor(it)
                        }
                })
    }

    companion object {
        private val LOG_TAG = MainActivity::class.simpleName
    }

    enum class StopwatchState { RUN, PAUSE, STOP }
}
