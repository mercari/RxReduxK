package com.mercari.rxredux

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.annotations.CheckReturnValue
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

interface Action

interface State

interface Reducer<S : State, A : Action> {

    fun reduce(currentState: S, action: A): S
}

interface Middleware<S : State, A : Action> {

    fun performBeforeReducingState(currentState: S, action: A) {}

    fun performAfterReducingState(action: A, nextState: S) {}
}

interface StoreType<S : State, A : Action> {

    val states: Observable<S>

    val indistinctStates: Observable<S>

    var replaceReducer: (S, A) -> S

    fun dispatch(action: A)

    fun dispatch(actions: Observable<out A>): Disposable

    fun dispatch(vararg actions: Observable<out A>): List<Disposable>

    fun addMiddleware(middleware: Middleware<S, A>)

    fun removeMiddleware(middleware: Middleware<S, A>): Boolean
}

class Store<S : State, A : Action>(
        initialState: S,
        reducer: Reducer<S, A>,
        defaultScheduler: Scheduler = Schedulers.single()
) : StoreType<S, A> {

    // seed action
    private object NoAction : Action

    private val actionSubject = PublishSubject.create<A>()

    override val states: Observable<S>
        get() = _states.distinctUntilChanged()

    override val indistinctStates: Observable<S>
        get() = _states

    private val _states: Observable<S>

    private val middlewares = mutableListOf<Middleware<S, A>>()

    // By default, this is doing nothing, just passing the reduced state
    override var replaceReducer: (S, A) -> S = { reducedState, _ -> reducedState }

    init {
        _states = actionSubject
                .scan(initialState to NoAction as Action) { (state, _), action ->
                    middlewares.onEach { it.performBeforeReducingState(state, action) }
                    val reducedState = reducer.reduce(state, action)
                    val nextState = replaceReducer(reducedState, action)
                    nextState to action
                }
                .doAfterNext { (nextState, latestAction) ->
                    if (latestAction !== NoAction) {
                        middlewares.onEach {
                            @Suppress("UNCHECKED_CAST")
                            it.performAfterReducingState(latestAction as A, nextState)
                        }
                    }
                }
                .map(Pair<S, Action>::first)
                .subscribeOn(defaultScheduler)
                .replay(1)
                .autoConnect()
    }

    override fun dispatch(action: A) {
        actionSubject.onNext(action)
    }

    @CheckReturnValue
    override fun dispatch(actions: Observable<out A>): Disposable = actions.subscribe(actionSubject::onNext)

    @CheckReturnValue
    override fun dispatch(vararg actions: Observable<out A>): List<Disposable> =
            actions.asList()
                    .map { it.subscribe(actionSubject::onNext) }

    override fun addMiddleware(middleware: Middleware<S, A>) {
        middlewares.add(middleware)
    }

    override fun removeMiddleware(middleware: Middleware<S, A>) = middlewares.remove(middleware)
}
