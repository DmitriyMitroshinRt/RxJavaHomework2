package iam.thevoid.epic.timeapp

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.*
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import java.time.temporal.ChronoUnit.MILLIS

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
    private var counter: Long = 0L

    // Секундомер
    private lateinit var stopwatchText: TextView
    private lateinit var stopwatchMillisText: TextView
    private lateinit var stopwatchStartButton: Button
    private lateinit var stopwatchEndButton: Button
    private lateinit var stopwatchTime: LocalTime
    private lateinit var stopwatchTimeBuf: LocalTime

    private var disposable: Disposable? = null
    private var pause: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clockText = findViewById(R.id.clockText)

        Single.create<String> {
            it.onSuccess(
                LocalTime
                    .now()
                    .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                    .toString()
            )
        }
            .delay(1, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .repeat()
            .subscribe { time ->
                clockText.setText(time)
            }

        countdownText = findViewById(R.id.countdownText)
        countdownSecondsEditText = findViewById(R.id.countdownEditText)
        countdownStartButton = findViewById(R.id.countdownStartButton)

        countdownStartButton.setOnClickListener {
            if (!countdownSecondsEditText.text.isEmpty()) {
                counter = countdownSecondsEditText.text.toString().toLong()
                countdownText.setText(getElapsedCountDownTime(counter))

                Flowable.fromIterable(1L..counter)
                    .concatMapCompletable {
                        Completable
                            .timer(1, TimeUnit.SECONDS)
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnComplete {
                                val elapsedTime = counter - it;
                                countdownText.setText(getElapsedCountDownTime(elapsedTime))
                                if (elapsedTime == 0L) {
                                    countdownText.setBackgroundColor(Color.RED)
                                    Completable.timer(3, TimeUnit.SECONDS)
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe {
                                            countdownText.setBackgroundColor(Color.WHITE)
                                        }
                                }
                            }
                    }
                    .subscribe()
            }
        }

        stopwatchText = findViewById(R.id.stopwatchText)
        stopwatchMillisText = findViewById(R.id.stopwatchMillisText)
        stopwatchStartButton = findViewById(R.id.stopwatchStartButton)
        stopwatchEndButton = findViewById(R.id.stopwatchEndButton)
        clearStopwatchTime()

        stopwatchStartButton.setOnClickListener {
            Log.d(LOG_TAG, "start timer")
            if (!pause)
                clearStopwatchTime()
            stopwatchTimeBuf = LocalTime.now()
            pause = false

            disposable = Flowable.interval(0, 1, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.computation())
                .onBackpressureDrop()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        val now = LocalTime.now()
                        stopwatchTime = stopwatchTime.plus(stopwatchTimeBuf.until(now, MILLIS), MILLIS)
                        stopwatchTimeBuf = now
                        Log.d(
                            LOG_TAG,
                            (stopwatchTime.format(
                                DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
                            ).toString())
                        )
                        Log.d(LOG_TAG, "time was changed")
                        setStopwatchTimeEdit()
                    },
                    {
                        Log.d(LOG_TAG, it.message.orEmpty())
                    }
                )

        }

        stopwatchEndButton.setOnClickListener {
            disposable?.dispose()
            disposable = null
            if (pause) {
                Log.d(LOG_TAG, "clear timer")
                stopwatchStartButton.setText("START")
                stopwatchEndButton.setText("PAUSE")
                clearStopwatchTime()
            } else {
                Log.d(LOG_TAG, "pause timer")
                stopwatchStartButton.setText("CONTINUE")
                stopwatchEndButton.setText("CLEAR")
                pause = true
            }
        }
    }

    private fun getElapsedCountDownTime(counter: Long): String {
        return LocalTime.of(0, 0, 0, 0)
            .plusSeconds(counter)
            .format(DateTimeFormatter.ofPattern("mm:ss"))
            .toString()
    }

    private fun clearStopwatchTime() {
        stopwatchTime = LocalTime.of(0, 0, 0, 0)
        setStopwatchTimeEdit()
    }

    private fun setStopwatchTimeEdit() {
        stopwatchText.setText(
            stopwatchTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                .toString()
        )
        stopwatchMillisText.setText(
            stopwatchTime.format(DateTimeFormatter.ofPattern(".SSS")).toString()
        )
    }

    companion object {
        private val LOG_TAG = MainActivity::class.simpleName
    }
}