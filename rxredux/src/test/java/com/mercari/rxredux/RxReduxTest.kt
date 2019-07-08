package com.mercari.rxredux

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MINUTES

data class CounterState(val counter: Int = 0) : State

sealed class CounterAction : Action

class Increment(val by: Int) : CounterAction()
class Decrement(val by: Int) : CounterAction()

class ReduxTest : Spek({

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

        context("increment action") {

            it("should increase counter") {
                store.dispatch(Increment(2))
                test.assertValuesOnly(CounterState(0), CounterState(2))
            }

            it("should continue increasing counter") {
                store.dispatch(Increment(10))
                test.assertValuesOnly(CounterState(0), CounterState(2), CounterState(12))
            }
        }
    }

    describe("a redux store with default scheduler") {

        val store = Store(counterState, counterReducer)
        val test = store.states.test()

        context("decrement action") {

            it("should decrease counter") {
                store.dispatch(Decrement(9))
                test.assertValuesOnly(CounterState(0), CounterState(-9))
            }

            it("should continue decreasing counter") {
                store.dispatch(Decrement(1))
                test.assertValuesOnly(CounterState(0), CounterState(-9), CounterState(-10))
            }
        }
    }

    describe("a redux store with trampoline scheduler") {

        context("store behavior") {
            val store = Store(counterState, counterReducer, Schedulers.trampoline())
            val test = store.states.test()

            it("should not publish change to subscribers, if state doesn't change") {
                store.dispatch(Increment(10))
                store.dispatch(Decrement(9))
                test.assertValuesOnly(CounterState(0), CounterState(10), CounterState(1))

                store.dispatch(Increment(0))
                test.assertValuesOnly(CounterState(0), CounterState(10), CounterState(1))
            }

            it("should receive the latest state first for new subscriber") {
                val localSubscriber = store.states.test()
                localSubscriber.assertValueCount(1)
                localSubscriber.assertValuesOnly(CounterState(1))
            }

            it("should support dispatch of Observable<Action>") {
                val obs = Observable.just(Decrement(23))
                store.dispatch(obs)

                val lastIndex = test.valueCount() - 1
                test.assertValueAt(lastIndex) { (it.counter == -22).shouldBeTrue() }
            }

            it("should support dispatch of List of Observable<Action>") {
                val ob1 = Observable.just(Decrement(2))
                val ob2 = Observable.just(Decrement(10))
                val ob3 = Observable.just(Increment(15))

                store.dispatch(ob1, ob2, ob3)

                val lastIndex = test.valueCount() - 1
                test.values()[lastIndex].counter shouldEqual -19
            }

            it("should not dispatch an action if the Observable gets disposed") {
                val localSubscriber = store.states.test()

                val obs = Observable.just(Increment(100)).delay(3000, TimeUnit.MILLISECONDS)
                val disposable = store.dispatch(obs)

                disposable.dispose()

                localSubscriber.assertValueCount(1)
            }

            it("should not dispatch an action if the some of the Observable gets disposed") {
                val localTest = store.states.test()

                val ob1 = Observable.just(Increment(10)).delay(1, MINUTES)
                val ob2 = Observable.just(Decrement(10)).delay(3, MINUTES)
                val ob3 = Observable.just(Increment(100))

                val disposables = store.dispatch(ob1, ob2, ob3)

                disposables[0].dispose()
                disposables[1].dispose()

                // original and the 100
                localTest.assertValueCount(2)
            }

            it("should not receive state changes after it gets disposed") {
                val localSubscriber = store.states.test()

                store.dispatch(Increment(3))
                store.dispatch(Decrement(20))
                store.dispatch(Increment(13))

                localSubscriber.assertValueCount(4)

                localSubscriber.cancel()

                store.dispatch(Increment(2))
                store.dispatch(Decrement(3))

                localSubscriber.assertValueCount(4)
            }

            it("should not invoke subscriber if the state hasn't changed") {
                val localSubscriber = store.states.test()

                store.dispatch(Increment(0))
                localSubscriber.await(500, TimeUnit.MILLISECONDS)
                localSubscriber.assertValueCount(1)

                store.dispatch(Decrement(0))
                localSubscriber.await(500, TimeUnit.MILLISECONDS)
                localSubscriber.assertValueCount(1)
            }

            it("should invoke subscriber if the state hasn't changed, but indistinctStates is used") {
                val localSubscriber = store.indistinctStates.test()

                store.dispatch(Increment(0))
                localSubscriber.await(500, TimeUnit.MILLISECONDS)
                localSubscriber.assertValueCount(2)

                store.dispatch(Decrement(0))
                localSubscriber.await(500, TimeUnit.MILLISECONDS)
                localSubscriber.assertValueCount(3)
            }
        }

        describe("a redux store with middleware") {

            context("middleware behavior") {
                data class SideEffectData(var value: Int)
                val sideEffectBothData = SideEffectData(0)
                val initialState = CounterState(0)
                val action = Increment(1)

                val overrideBothMiddleware = object : Middleware<CounterState, CounterAction> {

                    override fun performBeforeReducingState(currentState: CounterState, action: CounterAction) {
                        sideEffectBothData.value = currentState.counter - 1
                    }

                    override fun performAfterReducingState(action: CounterAction, nextState: CounterState) {
                        sideEffectBothData.value = nextState.counter + 1
                    }
                }

                it("should perform both side effects with overrideBothMiddleware") {
                    overrideBothMiddleware.performBeforeReducingState(initialState, action)
                    sideEffectBothData.value shouldEqual initialState.counter - 1

                    overrideBothMiddleware.performAfterReducingState(action, initialState)
                    sideEffectBothData.value shouldEqual initialState.counter + 1
                }

                val sideEffectBeforeOnlyData = SideEffectData(0)

                val overrideBeforeOnlyMiddleware = object : Middleware<CounterState, CounterAction> {

                    override fun performBeforeReducingState(currentState: CounterState, action: CounterAction) {
                        sideEffectBeforeOnlyData.value = currentState.counter - 1
                    }
                }

                it("should do nothing after reducing the state, with overrideBeforeOnlyMiddleware") {
                    overrideBeforeOnlyMiddleware.performBeforeReducingState(initialState, action)
                    sideEffectBeforeOnlyData.value shouldEqual initialState.counter - 1

                    overrideBeforeOnlyMiddleware.performAfterReducingState(action, initialState)
                    sideEffectBeforeOnlyData.value shouldEqual initialState.counter - 1
                }

                val sideEffectAfterOnlyData = SideEffectData(0)

                val overrideAfterOnlyMiddleware = object : Middleware<CounterState, CounterAction> {

                    override fun performAfterReducingState(action: CounterAction, nextState: CounterState) {
                        sideEffectAfterOnlyData.value = nextState.counter + 1
                    }
                }

                it("should do nothing before reducing the state, with overrideAfterOnlyMiddleware") {
                    overrideAfterOnlyMiddleware.performBeforeReducingState(initialState, action)
                    sideEffectAfterOnlyData.value shouldEqual 0

                    overrideAfterOnlyMiddleware.performAfterReducingState(action, initialState)
                    sideEffectAfterOnlyData.value shouldEqual initialState.counter + 1
                }
            }

            context("store behavior") {
                val store = Store(counterState, counterReducer, Schedulers.trampoline())
                val test = store.states.test()

                data class SideEffectData(var value: Int)

                val sideEffectData = SideEffectData(0)
                val updateSideEffectDataMiddleware = object : Middleware<CounterState, CounterAction> {

                    override fun performAfterReducingState(action: CounterAction, nextState: CounterState) {
                        sideEffectData.value = nextState.counter
                    }
                }

                it("should invoke side effect as state gets updated") {
                    store.addMiddleware(updateSideEffectDataMiddleware)

                    val localSubscriber = store.states.test()
                    val counter = localSubscriber.values().first().counter

                    store.dispatch(Increment(38))
                    store.dispatch(Decrement(12))

                    store.removeMiddleware(updateSideEffectDataMiddleware)

                    sideEffectData.value shouldEqual (counter + 38 - 12)
                }

                it("should be able to support multiple side effects as state gets updated") {
                    var latestAction: Action? = null

                    val middleware = object : Middleware<CounterState, CounterAction> {

                        override fun performAfterReducingState(action: CounterAction, nextState: CounterState) {
                            latestAction = action
                        }
                    }

                    store.addMiddleware(updateSideEffectDataMiddleware)
                    store.addMiddleware(middleware)

                    val localSubscriber = store.states.test()
                    val counter = localSubscriber.values().first().counter

                    store.dispatch(Increment(67))
                    store.dispatch(Decrement(30))

                    sideEffectData.value shouldEqual (counter + 67 - 30)
                    latestAction shouldBeInstanceOf Decrement::class

                    store.removeMiddleware(updateSideEffectDataMiddleware)
                    store.removeMiddleware(middleware)
                }

                it("should invoke side effect up until side effect got removed") {
                    store.addMiddleware(updateSideEffectDataMiddleware)

                    val localSubscriber = store.states.test()
                    val counter = localSubscriber.values().first().counter

                    store.dispatch(Increment(98))
                    store.removeMiddleware(updateSideEffectDataMiddleware)
                    store.dispatch(Decrement(49))

                    sideEffectData.value shouldEqual (counter + 98)
                }

                it("should invoke side effect methods in correct order") {
                    var before: Int? = null
                    var after: Int? = null

                    val middleware = object : Middleware<CounterState, CounterAction> {

                        override fun performBeforeReducingState(currentState: CounterState, action: CounterAction) {
                            before = currentState.counter
                        }

                        override fun performAfterReducingState(action: CounterAction, nextState: CounterState) {
                            after = nextState.counter
                        }
                    }

                    store.addMiddleware(middleware)

                    val localSubscriber = store.states.test()
                    val counter = localSubscriber.values().first().counter

                    store.dispatch(Increment(183))
                    before shouldEqual counter
                    after shouldEqual (counter + 183)

                    store.dispatch(Decrement(21))
                    before shouldEqual (counter + 183)
                    after shouldEqual (counter + 183 - 21)

                    store.removeMiddleware(middleware)
                }
            }
        }
    }
})
