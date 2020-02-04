package com.mercari.rxredux

import io.reactivex.Observable
import org.amshove.kluent.shouldBe
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicInteger

object MultiThreadTest : Spek({

    val counterState = CounterState()

    val counterReducer = object : Reducer<CounterState, CounterAction> {
        override fun reduce(currentState: CounterState, action: CounterAction): CounterState {
            val counter = currentState.counter
            return when (action) {
                is Increment -> currentState.copy(counter = counter + action.by)
                is Decrement -> currentState.copy(counter = counter - action.by)
            }
        }
    }

    describe("a redux store with default scheduler") {

        val store = Store(counterState, counterReducer)
        val test = store.states.test()
        val errorCounter = AtomicInteger(0)

        context("increment actions in parallel") {

            MultiThreadTest.incrementCounter(store, errorCounter)
            MultiThreadTest.incrementCounter(store, errorCounter)
            MultiThreadTest.incrementCounter(store, errorCounter)
            MultiThreadTest.incrementCounter(store, errorCounter)
            MultiThreadTest.incrementCounter(store, errorCounter)

            it("should no errors") {
                test.await(5_000L, MILLISECONDS)
                errorCounter.get() shouldBe 0
            }
        }
    }
}) {

    private fun incrementCounter(store: Store<CounterState, CounterAction>, errorCounter: AtomicInteger) {
        Observable.interval(0L, 10L, MILLISECONDS)
            .subscribe({
                store.dispatch(Increment(1))
            }, {
                it.printStackTrace()
                errorCounter.incrementAndGet()
            })
    }
}
