package iam.thevoid.epic.timeapp

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.format.DateUtils
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
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
    private lateinit var countdownFrameLayout: FrameLayout

    // Секундомер
    private lateinit var stopwatchText: TextView
    private lateinit var stopwatchMillisText: TextView
    private lateinit var stopwatchStartButton: Button
    private lateinit var stopwatchEndButton: Button

    private var disposableTimer: Disposable? = null
    private var disposableStopwatch: Disposable? = null

    enum class StopwatchState {
        IS_RUNNING, IS_PAUSED, IS_STOPPED
    }

    private var stopwatchState: StopwatchState = StopwatchState.IS_STOPPED
    private var stopwatchTime = 0L

    //@SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clockText = findViewById(R.id.clockText)

        countdownText = findViewById(R.id.countdownText)
        countdownSecondsEditText = findViewById(R.id.countdownEditText)
        countdownStartButton = findViewById(R.id.countdownStartButton)
        countdownFrameLayout = findViewById(R.id.countdownFrameLayout)

        stopwatchText = findViewById(R.id.stopwatchText)
        stopwatchMillisText = findViewById(R.id.stopwatchMillisText)
        stopwatchStartButton = findViewById(R.id.stopwatchStartButton)
        stopwatchEndButton = findViewById(R.id.stopwatchEndButton)

        // Часы
        getTime()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { item ->
                clockText.text = item
            }

        //Таймер
        countdownStartButton.setOnClickListener {
            disposableTimer?.dispose()
            val inputSeconds = countdownSecondsEditText.text.toString().toLongOrNull() ?: 0L
            disposableTimer = timer(inputSeconds)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { item ->
                        countdownText.text = item
                    },
                    {

                    },
                    {
                        frameBlink()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe { item -> countdownFrameLayout.setBackgroundColor(item) }
                    }
                )

        }

        //Секундомер
        stopwatchStartButton.setOnClickListener {
            when (stopwatchState) {
                StopwatchState.IS_STOPPED, StopwatchState.IS_PAUSED  -> {
                    stopwatchState = StopwatchState.IS_RUNNING
                    stopwatchStartButton.text = "START"
                    stopwatchEndButton.text = "PAUSE"
                }
                StopwatchState.IS_RUNNING -> {
                    stopwatchTime = 0
                }
            }
            disposableStopwatch?.dispose()
            disposableStopwatch = getStopwatch(stopwatchTime)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    stopwatchText.text = it

                }

        }

        stopwatchEndButton.setOnClickListener {
            disposableStopwatch?.dispose()
            when (stopwatchState) {
                StopwatchState.IS_RUNNING -> {
                    stopwatchState = StopwatchState.IS_PAUSED
                    stopwatchStartButton.text = "RESUME"
                    stopwatchEndButton.text = "STOP"

                }
                StopwatchState.IS_PAUSED -> {
                    stopwatchState = StopwatchState.IS_STOPPED
                    stopwatchTime = 0L
                    stopwatchText.text = ""
                    stopwatchStartButton.text = "START"

                }
                else -> {}
            }
        }
    }

    private fun getTime(): Observable<String> {
        return Observable.interval(1000, TimeUnit.MILLISECONDS)
            .map { getTimeStr() }
    }

    private fun getTimeStr(): String {
        val date = Calendar.getInstance().time
        val timeFormat = SimpleDateFormat.getTimeInstance()
        return timeFormat.format(date)
    }

    private fun timer(time: Long): Observable<String> {
        return Observable.interval(1000, TimeUnit.MILLISECONDS)
            .takeWhile { it <= time }
            .map { timerCountdown(time, it) }
    }

    private fun timerCountdown(time: Long, tick: Long): String {
        return DateUtils.formatElapsedTime(time - tick)
    }

    private fun frameBlink(): Observable<Int> {
        return Observable.interval(200, TimeUnit.MILLISECONDS)
            .takeWhile { it < 5 }
            .map { t -> getColorByNum(t.toInt()) }

    }

    private fun getColorByNum(num: Int): Int {
        return if (num % 2 == 0) Color.GRAY else Color.GREEN
    }

    private fun getStopwatch(curTime: Long): Observable<String> {
        return Observable.interval(1L, TimeUnit.MILLISECONDS)
            .map { t -> getStopwatchTime(t + curTime) }
    }

    private fun getStopwatchTime(time: Long): String {
        stopwatchTime = time
        val sdf = SimpleDateFormat("HH:mm:ss:SS", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("3")
        val timeMain = sdf.format(time)
        return timeMain.toString()
    }

}