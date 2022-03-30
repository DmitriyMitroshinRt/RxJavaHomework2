package iam.thevoid.epic.timeapp

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.time.Instant
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.time.DurationUnit
import kotlin.time.toDuration

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

    private val TAG = this::class.simpleName

    // Часы:
    private lateinit var clockText: TextView

    // Обратный отсчёт
    private lateinit var countDownText: TextView
    private lateinit var countDownSecondsEditText: EditText
    private lateinit var countDownStartButton: Button

    // Секундомер
    private lateinit var stopWatchText: TextView
    private lateinit var stopWatchMillisText: TextView
    private lateinit var stopWatchStartButton: Button
    private lateinit var stopWatchEndButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clockText = findViewById(R.id.clockText)

        countDownText = findViewById(R.id.countdownText)
        countDownSecondsEditText = findViewById(R.id.countdownEditText)
        countDownStartButton = findViewById(R.id.countdownStartButton)

        stopWatchText = findViewById(R.id.stopwatchText)
        stopWatchMillisText = findViewById(R.id.stopwatchMillisText)
        stopWatchStartButton = findViewById(R.id.stopwatchStartButton)
        stopWatchEndButton = findViewById(R.id.stopwatchEndButton)


        // 1. Часы
        clock()

        // 2. Таймер
        displayCountDown(0) // Начальное значение
        timer()


        // 3. Секундомер
        displayStopWatch() // Начальное значение
        stopWatch()
    }

    private fun clock() {
        Observable.interval(1, TimeUnit.SECONDS, Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())

            .subscribe {
                clockText.text = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            }
    }

    private val delimiter = ":"

    private val twoDigits: (Number) -> String =
        { value -> if (value.toLong() < 10) "0$value" else "$value" }

    private val formatTime: (Long, Int, Int, Int) -> java.lang.StringBuilder =
        { hours, minutes, seconds, nano ->
            StringBuilder()
                .append(twoDigits(hours))
                .append(delimiter)
                .append(twoDigits(minutes))
                .append(delimiter)
                .append(twoDigits(seconds))
        }

    private val displayCountDown: (Long) -> Unit =
        { seconds ->
            countDownText.text =
                seconds.toDuration(DurationUnit.SECONDS).toComponents(formatTime)
        }

    private fun timer() {
        val countDownValue: () -> Long =
            { countDownSecondsEditText.text.toString().toLongOrNull() ?: 0L }

        countDownStartButton.setOnClickListener {
            val seconds = countDownValue()
            Log.i(TAG, "seconds: $seconds")

            if (seconds > 0) {
                // Время окончания
                val end = Instant.now().plusSeconds(seconds)

                Observable.interval(1, TimeUnit.SECONDS, Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())

                    .takeWhile { Instant.now() <= end }

                    .subscribe {
                        val remained = end.minusMillis(Instant.now().toEpochMilli())
                        val duration =
                            remained.toEpochMilli().toDuration(DurationUnit.MILLISECONDS)
                        countDownText.text = duration.toComponents(formatTime)
                    }
            }
        }
    }

    private enum class EStopWatchStatus {
        STOP,
        RUN,
        PAUSE
    }

    private object stopWatchStatus {
        var status: EStopWatchStatus = EStopWatchStatus.STOP
        var value: Long = 0
    }

    private val displayStopWatch: () -> Unit = {
        stopWatchText.text =
            stopWatchStatus.value.toDuration(DurationUnit.MILLISECONDS)
                .toComponents(formatTime)
        stopWatchMillisText.text =
            "${stopWatchStatus.value % 1000 / 100}" // 10-е мс
    }

    private fun stopWatch() {
        stopWatchStartButton.setOnClickListener {
            Log.i(TAG, "${stopWatchStatus.status}: ${stopWatchStatus.value}")

            if (stopWatchStatus.status == EStopWatchStatus.STOP || stopWatchStatus.status == EStopWatchStatus.PAUSE) {
                stopWatchStatus.status = EStopWatchStatus.RUN // Начинаем работу

                stopWatchEndButton.text = "Пауза"

                val start = Instant.now()
                    .toEpochMilli() - stopWatchStatus.value // При состоянии паузы "отходим назад"

                Observable.interval(100, TimeUnit.MILLISECONDS, Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())

                    .takeWhile { stopWatchStatus.status == EStopWatchStatus.RUN }

                    .subscribe {
                        stopWatchStatus.value = Instant.now().toEpochMilli() - start
                        displayStopWatch()
                    }
            }
        }

        stopWatchEndButton.setOnClickListener()
        {
            Log.i(TAG, "${stopWatchStatus.status}: ${stopWatchStatus.value}")
            when (stopWatchStatus.status) {
                EStopWatchStatus.RUN -> {
                    // Приостанавливаем
                    stopWatchStatus.status = EStopWatchStatus.PAUSE
                    stopWatchEndButton.text = "Сброс"
                }
                EStopWatchStatus.PAUSE -> {
                    // Сброс
                    stopWatchStatus.status = EStopWatchStatus.STOP
                    stopWatchStatus.value = 0

                    stopWatchEndButton.text = "Пауза"

                    displayStopWatch()
                }
                EStopWatchStatus.STOP -> {
                    stopWatchEndButton.text = "Пауза"
                }
            }
        }
    }
}
