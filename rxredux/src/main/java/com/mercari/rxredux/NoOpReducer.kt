package com.mercari.rxredux

/**
 * This reducer always returns the same state.
 */
class NoOpReducer<S : State, A : Action> : Reducer<S, A> {
  override fun reduce(currentState: S, action: A): S = currentState
}