package iam.thevoid.epic.timeapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.format.DateUtils
import androidx.appcompat.app.AppCompatActivity
import iam.thevoid.epic.timeapp.databinding.ActivityMainBinding
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
    private lateinit var binding: ActivityMainBinding
    private var disposableCountdownTimer: Disposable? = null
    private var disposableStopWatchClock: Disposable? = null
    private var stopwatchClockState = StopwatchClockState.STOP
    private var milSecShift = 5L
    private var lastMilSec = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setStopwatchClockBegin()
        binding.countdownText.text = getTimeForCountdown(0L, 0L)
    }

    @SuppressLint("SetTextI18n")
    override fun onStart() {
        super.onStart()
        // *************** Current Time Clock ***************
        observableInterval().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe { binding.clockText.text = it }
        // *************** CountDown Timer ***************
        binding.countdownStartButton.setOnClickListener {
            disposeCountdown()
            val inputCountDownSeconds: Long = binding.countdownEditText
                .text.toString().toLongOrNull() ?: 0L
            disposableCountdownTimer = getTimerOfCountDown(inputCountDownSeconds)
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe({item -> binding.countdownText.text = "[ $item ]"}, {})
        }
        // *************** StopWatch Clock ***************
        binding.stopwatchStartButton.setOnClickListener {
            when (stopwatchClockState) {
                StopwatchClockState.STOP -> {
                    stopwatchClockState = StopwatchClockState.RUN
                    binding.stopwatchStartButton.text = "Start"
                    binding.stopwatchEndButton.text = "Pause"
                }
                StopwatchClockState.PAUSE -> {
                    stopwatchClockState = StopwatchClockState.RUN
                    binding.stopwatchStartButton.text = "Start"
                    binding.stopwatchEndButton.text = "Pause"
                }
                StopwatchClockState.RUN -> {
                    lastMilSec = 0
                }
            }
            disposeStopWatchClock()
            disposableStopWatchClock = getStopwatchTimer(lastMilSec)
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe {item ->
                    binding.stopwatchText.text = item.first
                    binding.stopwatchMillisText.text = item.second
                    lastMilSec = item.third
                }
        }
        binding.stopwatchEndButton.setOnClickListener {
            disposeStopWatchClock()
            when (stopwatchClockState) {
                StopwatchClockState.RUN -> {
                    stopwatchClockState = StopwatchClockState.PAUSE
                    binding.stopwatchStartButton.text = "Resume"
                    binding.stopwatchEndButton.text = "Reset"
                }
                StopwatchClockState.PAUSE -> {
                    stopwatchClockState = StopwatchClockState.STOP
                    lastMilSec = 0
                    binding.stopwatchStartButton.text = "Start"
                    binding.stopwatchEndButton.text = "Pause"
                    setStopwatchClockBegin()
                } else -> {setStopwatchClockBegin()}
            }
        }
    }

    // *************** get current time from observable and format it ***************
    private fun observableInterval(): Observable<String> {
        return Observable.interval(1000, TimeUnit.MILLISECONDS).map {
            SimpleDateFormat("[ HH:mm:ss ]", Locale.GERMAN).format(Calendar.getInstance().time)
        }
    }

    // *************** Time format for Timer Of CountDown ***************
    private fun getTimeForCountdown(tmr: Long, v: Long): String {
        return DateUtils.formatElapsedTime(tmr - v)
    }

    // *************** Timer Of CountDown ***************
    private fun getTimerOfCountDown(timeBeforeStop: Long): Observable<String> {
        return Observable.interval(1000, TimeUnit.MILLISECONDS)
            .takeWhile { it <= timeBeforeStop }
            .map { time -> getTimeForCountdown(timeBeforeStop, time) }
    }

    private fun disposeCountdown() {
        disposableCountdownTimer?.dispose(); disposableCountdownTimer = null
    }

    private fun disposeStopWatchClock() {
        disposableStopWatchClock?.dispose(); disposableStopWatchClock = null
    }

    // *************** StopWatch beginning state ***************
    private fun setStopwatchClockBegin() {
        val info = getStopwatchClockTime(0L)
        binding.stopwatchText.text = info.first
        binding.stopwatchMillisText.text = info.second
    }

    // *************** Format Time for StopWatch Clock ***************
    private fun getStopwatchClockTime(time: Long): Triple<String, String, Long> {
        val part1 = String.format("[ %02d:%02d:%02d ]", TimeUnit.MILLISECONDS.toHours(time),
            TimeUnit.MILLISECONDS.toMinutes(time) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(time)),
            TimeUnit.MILLISECONDS.toSeconds(time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time)))
        val part2 = String.format("[ %03d ]", time - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(time)))
        return Triple(part1, part2, time)
    }

    private fun getStopwatchTimer(lastMilSec: Long): Observable<Triple<String, String, Long>> {
        return Observable.interval(milSecShift, TimeUnit.MILLISECONDS)
            .map { t -> getStopwatchClockTime(t * milSecShift + lastMilSec) }
    }

    enum class StopwatchClockState { RUN, PAUSE, STOP }
}