package com.mercari.rxredux

/**
 * A reducer container that can have multiple other reducers.
 * This will apply all [Reducer.reduce] operations associated with this sequentially.
 */
class CompositeReducer<S : State, A : Action>(
  private val reducers: List<Reducer<S, A>>
) : Reducer<S, A> {
  override fun reduce(currentState: S, action: A): S = reducers.fold(currentState) { state, reducer ->
    reducer.reduce(state, action)
  }
}