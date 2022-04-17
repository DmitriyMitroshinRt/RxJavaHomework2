package iam.thevoid.epic.timeapp

//import io.reactivex.rxjava3.core.Observable
//
//
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jakewharton.rxbinding3.view.clicks
import io.reactivex.Observable
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxkotlin.merge
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Flow

//import kotlinx.android.synthetic.main.activity_main.*
//import java.util.concurrent.TimeUnit
//


//import io.reactivex.android.schedulers.AndroidSchedulers
//import kotlinx.android.synthetic.main.activity_main.*
//import java.util.concurrent.TimeUnit

// Дан экран с готовой разметкой
// Реализовать при помощи RxJava
// 1) Отображение часов,начинают работать при старте, показывают время в любом удобном формате.
//    Как пример можно использовать формат из разметки
// 2) Таймер - ^?
//    а) Пользователь вводит количество секунд в поле
//    б) По нажатию на "Старт" начинается обратный отсчёт
//    в) (не обязательно) По окончании таймер каким либо образом сигнализирует об окончании,
//       например область таймера вспыхивает ярким цветом
// 3) Секундомер ^? - Не до конца доделал
//    а) Пользователь нажимает на "Старт", начинается отсчёт времени. В соответствующие текстовые
//       поля выводится количество прошедшего времени (отдельно время с точностью до секунд,
//       отдельно миллисекунды)
//    б) По нажатию на паузу отсчёт времени останавливается. Кнопка "Пауза" превращается в кнопку
//       "Сброс".
//    в) По нажатию на "Сброс" отстчёт времени сбрасывается в 0. "Старт" продолжает приостановленный
//       отсчёт
//    г) (не обязательно) Можно сделать изменение состояние кнопки "Старт" на "Продолжить" для
//       состояния паузы
private const val MAXIMUM_STOP_WATCH_LIMIT = 36000000L
private const val NUMBER_OF_SECONDS_IN_ONE_MINUTE = 60

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
    private lateinit var  SecName: TextView
    private lateinit var stopwatchStartButton: Button /*EditText*/
    private lateinit var stopwatchEndButton:   Button /*EditText*/


    private val disposable =
        io.reactivex.disposables.CompositeDisposable()
    private val disposableMS = io.reactivex.disposables.CompositeDisposable()
    //var disposable: Disposable? = null;
    private val displayInitialState by lazy { resources.getString(R.string._0_0) }
    private val displayInitialStateMS by lazy { resources.getString(R.string._0_0_0) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clockText = findViewById(R.id.clockText)
        clock()

        countdownText = findViewById(R.id.countdownText)
        countdownSecondsEditText = findViewById(R.id.countdownEditText)
        countdownStartButton = findViewById(R.id.countdownStartButton)

        countdownStartButton.setOnClickListener {
          // countdownText.text = "Hello Kitty!"
            //backCount(countdownSecondsEditText.toString().toLong())
            timer(countdownSecondsEditText.text.toString().toLong())
        }


        stopwatchText = findViewById(R.id.stopwatchText)

        stopwatchMillisText = findViewById(R.id.stopwatchMillisText)
        //
        SecName =  findViewById(R.id.SecName)
        //

        stopwatchStartButton =  findViewById(R.id.stopwatchStartButton)
        stopwatchEndButton   =   findViewById(R.id.stopwatchEndButton)






        mergeClicks().switchMap {
            if (it) timerObservable()
            else Observable.just(displayInitialState)
        }.subscribe(stopwatchText::setText)
        .let(disposable::add)

      /*     mergeClicks().switchMap {
                if (it) timerObservableMS()
                else Observable.just(displayInitialStateMS)
            }.subscribe(/*stopwatchMillisText*/SecName::setText)
                .let(disposableMS::add)
       */




    }

    override fun onDestroy() {
        disposable.clear()
        disposableMS.clear()
        super.onDestroy()
    }

    private fun mergeClicks(): Observable<Boolean> =
        listOf( stopwatchStartButton.clicks().map { true }, stopwatchEndButton.clicks().map { false })
            .merge()
            .doOnNext(::buttonStateManager)

    private fun buttonStateManager(boolean: Boolean) {
        stopwatchStartButton.isEnabled = !boolean
        stopwatchEndButton.isEnabled = boolean
    }
    private fun timerObservable(): Observable<String> =
        Observable.interval(0, 1, java.util.concurrent.TimeUnit.SECONDS)
            .takeWhile { it <= MAXIMUM_STOP_WATCH_LIMIT }
            .map(timeFormatter)
            .observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
            .doOnComplete { buttonStateManager(false) }

    private fun timerObservableMS(): Observable<String> =
        Observable.interval(0, 1, java.util.concurrent.TimeUnit.MILLISECONDS)
            .takeWhile { it <= MAXIMUM_STOP_WATCH_LIMIT }
            .map(timeFormatterMS)
            .observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
            .doOnComplete { buttonStateManager(false) }

    private val timeFormatter: (Long) -> String =
        { secs ->
            if (secs == MAXIMUM_STOP_WATCH_LIMIT) displayInitialState
            else "${secs / NUMBER_OF_SECONDS_IN_ONE_MINUTE} : ${secs % NUMBER_OF_SECONDS_IN_ONE_MINUTE}"
        }


    private val timeFormatterMS: (Long) -> String =
        { secs ->

            if (secs == MAXIMUM_STOP_WATCH_LIMIT) displayInitialStateMS
            else "${secs} "
        }

    override fun onStop() {
        super.onStop()
        disposable?.dispose()
       // disposable = null
    }

    override fun onStart() {
        super.onStart()
     //   disposable = null; // subscribeInput()
    }

    val mSubscription: Flow.Subscription? = null
    private fun timer(count: Long) {
        if (count.toString().isEmpty())
        {  countdownText.text = "err:" //+ count.toString()

            return}
        Flowable.interval (0, 1,  java.util.concurrent.TimeUnit.SECONDS)
            .onBackpressureBuffer ()
            .take (count + 1)
            .map{ aLong ->
                count - aLong //
            }
            .observeOn (AndroidSchedulers.mainThread ())
            .subscribe({
                countdownText.text = it.toString()
                if (it == 0L) {
                    countdownText.setTextColor(Color.RED)
                }
            })



    }


    private fun clock() {
        val count: Long = 10000000000000000L
        Flowable.interval (0, 1,  java.util.concurrent.TimeUnit.SECONDS)
            .onBackpressureBuffer ()
            .take (count + 1)
            .map{ aLong ->
                count - aLong //
            }
            .observeOn (AndroidSchedulers.mainThread ())
            .subscribe({
                //val curTime: Long = Date().getTime()
                val currentTime = System.currentTimeMillis()
                val simpleDateFormat = SimpleDateFormat(/*"hh:mm:ss.S"*/"HH:mm:ss.SSS")
                val date = Date(currentTime)
                val time: String = simpleDateFormat.format(date)
                clockText.text = time

            })



    }

   }

