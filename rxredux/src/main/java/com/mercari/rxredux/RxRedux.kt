package com.mercari.rxredux

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.annotations.CheckReturnValue
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

interface Action

interface State

interface Reducer<S : State> {

    fun reduce(currentState: S, action: Action): S
}

interface Middleware<S : State> {

    fun performBeforeReducingState(currentState: S, action: Action) {}

    fun performAfterReducingState(action: Action, nextState: S) {}
}

interface StoreType<S : State> {

    val states: Observable<S>

    val indistinctStates: Observable<S>

    var replaceReducer: (S, Action) -> S

    fun dispatch(action: Action)

    fun dispatch(actions: Observable<out Action>): Disposable

    fun dispatch(vararg actions: Observable<out Action>): List<Disposable>

    fun addMiddleware(middleware: Middleware<S>)

    fun removeMiddleware(middleware: Middleware<S>): Boolean
}

class Store<S : State>(
        initialState: S,
        reducer: Reducer<S>,
        defaultScheduler: Scheduler = Schedulers.single()
) : StoreType<S> {

    object NoAction : Action

    private val actionSubject = PublishSubject.create<Action>()

    override val states: Observable<S>
        get() = _states.distinctUntilChanged()

    override val indistinctStates: Observable<S>
        get() = _states

    private val _states: Observable<S>

    private val middlewares = mutableListOf<Middleware<S>>()

    // By default, this is doing nothing, just passing the reduced state
    override var replaceReducer: (S, Action) -> S = { reducedState, _ -> reducedState }

    init {
        _states = actionSubject
                .scan(initialState to NoAction as Action) { (state, _), action ->
                    middlewares.onEach { it.performBeforeReducingState(state, action) }
                    val reducedState = reducer.reduce(state, action)
                    val nextState = replaceReducer(reducedState, action)
                    nextState to action
                }
                .doAfterNext { next ->
                    val (nextState, latestAction) = next
                    middlewares.onEach { it.performAfterReducingState(latestAction, nextState) }
                }
                .map(Pair<S, Action>::first)
                .subscribeOn(defaultScheduler)
                .replay(1)
                .autoConnect()
    }

    override fun dispatch(action: Action) {
        actionSubject.onNext(action)
    }

    @CheckReturnValue
    override fun dispatch(actions: Observable<out Action>): Disposable = actions.subscribe(actionSubject::onNext)

    @CheckReturnValue
    override fun dispatch(vararg actions: Observable<out Action>): List<Disposable> =
            actions.asList()
                    .map { it.subscribe(actionSubject::onNext) }

    override fun addMiddleware(middleware: Middleware<S>) {
        middlewares.add(middleware)
    }

    override fun removeMiddleware(middleware: Middleware<S>) = middlewares.remove(middleware)
}
