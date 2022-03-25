package iam.thevoid.epic.timeapp

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
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

    private var disposableCountdownTimer: Disposable? = null
    private var disposableStopwatch: Disposable? = null
    private var disposebleColorBlink: Disposable? = null

    private var millsShift: Long = 5L
    private var lastMillis: Long = 0L

    // состояние обратного отсчета
    enum class StopwatchState {
        STOPPED, PAUSED, RUNNING
    }

    private var stopwatchState: StopwatchState = StopwatchState.STOPPED

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

        setStopwatchFirstTime()
        countdownText.text = getCountdownTime(0L, 0L)
    }

    @SuppressLint("SetTextI18n")
    override fun onStop() {
        super.onStop()
        if (stopwatchState == StopwatchState.RUNNING) {
            disposeStopwatch()
            stopwatchState = StopwatchState.PAUSED
            stopwatchStartButton.text = "Resume"
            stopwatchEndButton.text = "Stop"
        }
    }

    private fun disposeCountdown() {
        disposableCountdownTimer?.dispose()
        disposableCountdownTimer = null
    }

    private fun disposeStopwatch() {
        disposableStopwatch?.dispose()
        disposableStopwatch = null
    }

    private fun disposeColorBlink() {
        disposebleColorBlink?.dispose()
        disposebleColorBlink = null
    }

    private fun setStopwatchFirstTime() {
        val texts = getStopwatchTime(0L)
        stopwatchText.text = texts.first
        stopwatchMillisText.text = texts.second
    }

    @SuppressLint("SetTextI18n")
    override fun onStart() {
        super.onStart()

        // часы
        getTime()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { item ->
                clockText.text = item
            }

        // обратный отсчет
        countdownStartButton.setOnClickListener {
            disposeCountdown()
            disposeColorBlink()
            val input: Long = countdownSecondsEditText.text.toString().toLongOrNull() ?: 0L
            countdownText.setTextColor(Color.BLACK)
            disposableCountdownTimer = getCountdownTimer(input)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({ item ->
                    countdownText.text = item
                }, {}) {
                    disposebleColorBlink = getColourBlink()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { itemColor -> countdownText.setTextColor(itemColor) }
                }
        }

        //секундомер
        stopwatchStartButton.setOnClickListener {
            when (stopwatchState) {
                StopwatchState.PAUSED, StopwatchState.STOPPED -> {
                    stopwatchState = StopwatchState.RUNNING
                    stopwatchStartButton.text = "Start"
                    stopwatchEndButton.text = "Pause"
                }
                StopwatchState.RUNNING -> {
                    lastMillis = 0L
                }
            }

            disposeStopwatch()
            disposableStopwatch = getStopwatchTimer(lastMillis)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    item ->
                    stopwatchText.text = item.first
                    stopwatchMillisText.text = item.second
                    lastMillis = item.third
                }
        }

        stopwatchEndButton.setOnClickListener {
            disposeStopwatch()
            when (stopwatchState) {
                StopwatchState.RUNNING -> {
                    stopwatchState = StopwatchState.PAUSED
                    stopwatchStartButton.text = "Resume"
                    stopwatchEndButton.text = "Stop"
                }
                StopwatchState.PAUSED -> {
                    stopwatchState = StopwatchState.STOPPED
                    lastMillis = 0L
                    stopwatchStartButton.text = "Start"
                    stopwatchEndButton.text = "Pause"

                    setStopwatchFirstTime()
                }
                else -> {
                    Log.d(LOG_TAG, stopwatchState.toString())
                }
            }
        }

    }

    // Форматирование времени
    private fun getTimeStr(): String {
        val date = Calendar.getInstance().time
        val formatter = SimpleDateFormat.getTimeInstance()
        return formatter.format(date)
    }

    // Получение времени
    private fun getTime(): Observable<String> {
        return Observable.interval(1, TimeUnit.SECONDS).map { getTimeStr()}
    }

    // Форматирование времени для обратного отсчета
    private fun getCountdownTime(tmr: Long, v: Long): String {
        return DateUtils.formatElapsedTime(tmr - v)
    }

    // Обратный отчет таймер
    private fun getCountdownTimer(timeUnilStop: Long): Observable<String> {
        return Observable.interval(1, TimeUnit.SECONDS)
            .takeWhile { it <= timeUnilStop}
            .map { t -> getCountdownTime(timeUnilStop, t) }
    }

    // Цвет для мигания
    private fun getColorByNum(i: Int): Int {
        Thread.sleep(100)
        return if (i % 2 == 0) Color.WHITE else Color.RED
    }

    // Мигание
    private fun getColourBlink(): Observable<Int> {
        return Observable.range(1, 5)
            .map { t -> getColorByNum(t) }
    }

    // Секундомер - форматирование
    private fun getStopwatchTime(tmr: Long): Triple<String, String, Long> {
        val hours = TimeUnit.MILLISECONDS.toHours(tmr)
        val mins = TimeUnit.MILLISECONDS.toMinutes(tmr) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(tmr))
        val sec = TimeUnit.MILLISECONDS.toSeconds(tmr) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(tmr))
        val millisecend = tmr - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(tmr))

        val s1 = String.format("%02d:%02d:%02d", hours, mins, sec)
        val s2 = String.format("%03d", millisecend)
        return Triple(s1, s2, tmr)
    }

    // Секундомер - счетчик
    private fun getStopwatchTimer(lastMillis: Long): Observable<Triple<String, String, Long>> {
        return Observable.interval(millsShift, TimeUnit.MILLISECONDS)
            .map { t -> getStopwatchTime(t * millsShift + lastMillis) }
    }

    companion object {
        private val LOG_TAG = MainActivity::class.simpleName
    }

}