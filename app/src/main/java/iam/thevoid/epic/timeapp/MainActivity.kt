package iam.thevoid.epic.timeapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clockText = findViewById(R.id.clockText)

        //val subject = PublishSubject.create<Long>()
        Observable
            .interval(1000, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                clockText.text = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Calendar.getInstance().time)
            }

        countdownText = findViewById(R.id.countdownText)
        countdownSecondsEditText = findViewById(R.id.countdownEditText)
        countdownStartButton = findViewById(R.id.countdownStartButton)

        stopwatchText = findViewById(R.id.stopwatchText)
        stopwatchMillisText = findViewById(R.id.stopwatchMillisText)
        stopwatchStartButton = findViewById(R.id.stopwatchStartButton)
        stopwatchEndButton = findViewById(R.id.stopwatchEndButton)
    }
}