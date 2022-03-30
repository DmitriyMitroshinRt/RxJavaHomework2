package iam.thevoid.epic.timeapp

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
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
    private lateinit var timerLayout: FrameLayout

    // Секундомер
    private lateinit var stopwatchText: TextView
    private lateinit var stopwatchMillisText: TextView
    private lateinit var stopwatchStartButton: Button
    private lateinit var stopwatchEndButton: Button
    private val compositeDisposable = CompositeDisposable()


    companion object {
        const val BASE_MAIN_TIME = "00:00:00"
        const val BASE_MS_TIME = "000"
    }

    /** Именования кнопок */
    enum class EButton(val title: String) {
        PAUSE("PAUSE"),
        RESET("RESET"),
        START("START"),
        CONTINUE("CONTINUE")
    }

    /** Состояния секундомера */
    enum class EState() {
        RUNNING, STOPPED, PAUSE
    }

    /* Текущее состояние секундомера */
    private var stopWatchState = EState.STOPPED

    /* Последнее время сенкудомера */
    private var lastMilliseconds = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /* Часы */
        clockText = findViewById(R.id.clockText)

        currentTimeObserver()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                clockText.text = it
            }

        /* Обратный отсчёт */
        countdownText = findViewById(R.id.countdownText)
        countdownSecondsEditText = findViewById(R.id.countdownEditText)
        countdownStartButton = findViewById(R.id.countdownStartButton)
        timerLayout = findViewById(R.id.timer_layout)

        countdownStartButton.setOnClickListener {
            val interval = countdownSecondsEditText.text.toString()
            if (interval.isNotEmpty()) {
                countdownStartButton.isEnabled = false
                downWatchObserver(interval.toLong())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { countdownText.text = it.toString() }, {},
                        {
                            /* Вопрос: как можно избавиться от вложенного Observable и избежать роста цепочки? */
                            handleTimerLayout()
                        }
                    )
            }
        }

        // Секундомер
        stopwatchText = findViewById(R.id.stopwatchText)
        stopwatchMillisText = findViewById(R.id.stopwatchMillisText)
        stopwatchStartButton = findViewById(R.id.stopwatchStartButton)
        stopwatchEndButton = findViewById(R.id.stopwatchEndButton)

        stopwatchEndButton.setOnClickListener {
            if (stopWatchState == EState.RUNNING) {
                stopwatchEndButton.text = EButton.RESET.title
                stopwatchStartButton.text = EButton.CONTINUE.title

                stopWatchState = EState.PAUSE
                compositeDisposable.clear()
            } else if (stopWatchState == EState.PAUSE) {
                lastMilliseconds = 0L

                stopwatchEndButton.text = EButton.PAUSE.title
                stopwatchStartButton.text = EButton.START.title

                stopwatchText.text = BASE_MAIN_TIME
                stopwatchMillisText.text = BASE_MS_TIME

                stopWatchState = EState.STOPPED
            }
        }

        stopwatchStartButton.setOnClickListener {
            if (stopWatchState == EState.STOPPED) {
                stopWatchState = EState.RUNNING
            } else if (stopWatchState == EState.PAUSE) {
                stopWatchState = EState.RUNNING
                stopwatchEndButton.text = EButton.PAUSE.title
            }

            compositeDisposable.add(
                stopWatchObserver(lastMilliseconds)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        stopwatchText.text = it.first
                        stopwatchMillisText.text = it.second
                    })
        }
    }

    private fun handleTimerLayout() {
        timerLayoutBgColorObserver()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                timerLayout.setBackgroundColor(it)
            }, {}, {
                countdownStartButton.isEnabled = true
                timerLayout.setBackgroundColor(Color.WHITE)
            })
    }

    /** Возвращает время секундомера начиная с заданного времени в мс [lastMs] */
    private fun stopWatchObserver(lastMs: Long) = Observable
        .interval(1, TimeUnit.MILLISECONDS, Schedulers.computation())
        .map { formatElapsedTime(it + lastMs) }

    /** Возврващет время в формате HH:mm:ss:mls для заданного кол-ва миллисекунд [elapsed] */
    private fun formatElapsedTime(elapsed: Long): Pair<String, String> {
        val milliseconds = elapsed % 1000
        val seconds = elapsed / 1000
        val secondsFormatted = (seconds % 60)
        val minutes = seconds / 60
        val minutesFormatted = (minutes % 60)
        val hoursFormatted = minutes / 60
        lastMilliseconds = elapsed
        return Pair(
            String.format(
                "%02d:%02d:%02d",
                hoursFormatted,
                minutesFormatted,
                secondsFormatted
            ),
            String.format("%03d", milliseconds)
        )
    }

    /** Поочередно возвращает цвет для фона (10 раз) каждые 0.5 секунд */
    private fun timerLayoutBgColorObserver() = Observable
        .intervalRange(0, 10, 0, 500, TimeUnit.MILLISECONDS, Schedulers.io())
        .map {
            if (it.toInt() % 2 == 0)
                Color.GREEN
            else Color.WHITE
        }

    /** Возвращает текущее время */
    private fun currentTimeObserver() = Observable
        .interval(1, TimeUnit.SECONDS, Schedulers.io())
        .map { getCurrentTime() }

    /** Возвращает текущее время в формате HH:mm:ss */
    private fun getCurrentTime() =
        SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(Date())

    /** Возвращает секунды в обратном порядке начиная с [interval] */
    private fun downWatchObserver(interval: Long) = Observable
        .interval(1, TimeUnit.SECONDS, Schedulers.io())
        .takeWhile { it <= interval }
        .map { interval - it }
}