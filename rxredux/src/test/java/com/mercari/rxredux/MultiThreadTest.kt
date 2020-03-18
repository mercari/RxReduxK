package com.mercari.rxredux

import io.reactivex.Observable
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.security.SecureRandom
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

    describe("a redux store with default PublishSubject") {

        val store = Store(
            initialState = counterState,
            reducer = counterReducer
        )
        val test = store.states.test()
        val errorCounter = AtomicInteger(0)

        context("increment actions in parallel") {

            repeat(10) {
                MultiThreadTest.incrementCounter(store, errorCounter)
            }

            it("should errors") {
                test.await(5_000L, MILLISECONDS)
                errorCounter.get() shouldBeGreaterThan 0
            }
        }
    }

    describe("a redux store with serialized PublishSubject") {

        val store = Store(
            initialState = counterState,
            reducer = counterReducer,
            serializeActions = true
        )
        val test = store.states.test()
        val errorCounter = AtomicInteger(0)

        context("increment actions in parallel") {

            repeat(10) {
                MultiThreadTest.incrementCounter(store, errorCounter)
            }

            it("should no errors") {
                test.await(5_000L, MILLISECONDS)
                errorCounter.get() shouldBeEqualTo 0
            }
        }
    }
}) {

    private fun incrementCounter(store: Store<CounterState, CounterAction>, errorCounter: AtomicInteger) {
        Observable.interval(0L, SecureRandom().nextLong(), MILLISECONDS)
            .subscribe({
                store.dispatch(Increment(1))
            }, {
                errorCounter.incrementAndGet()
            })
    }
}
