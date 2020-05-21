# RxRedux for Kotlin

[![jcenter](https://api.bintray.com/packages/mercari-inc/maven/rxreduxk/images/download.svg)](https://bintray.com/mercari-inc/maven/rxreduxk/_latestVersion)
[![Build Status](https://circleci.com/gh/mercari/RxReduxK.svg?style=svg)](https://circleci.com/gh/mercari/RxReduxK)
[![codecov](https://codecov.io/gh/mercari/rxreduxk/branch/master/graph/badge.svg)](https://codecov.io/gh/mercari/RxReduxK)

Micro-framework for Redux implemented in Kotlin

## Installation

```
dependencies {
  repositories {
    jcenter()
  }
}

implementation("com.mercari.rxredux:rxredux:<latest-version>")
```

## Usage

This framework is composed of several types/abstractions inspired by [Redux](https://redux.js.org/) that help to implement the reactive behavior of application components.
It is based on [RxJava](https://github.com/ReactiveX/RxJava) for reactivity and works well with [RemoteDataK](https://github.com/mercari/RemoteDataK).

### State

State represents the model or state of your component or UI.
A State is recommended to be immutable, however it can be allowed to be mutable.

This can typically be implemented by a [data class](https://kotlinlang.org/docs/reference/data-classes.html)

For example:

```kotlin
data class CounterState(
    val counter: Int
) : State
```

### Action

An Action represents the desired modifications on a State, for example

```kotlin
class Increment : Action
class Decrement : Action
```

Although not required, it is recommended to model Actions as class hierarchy with a [sealed class](https://kotlinlang.org/docs/reference/sealed-classes.html).

```kotlin
sealed class CounterAction : Action

class Increment : CounterAction()
class Decrement : CounterAction()
```

An Action can contain parameters that make them more useful depending on the desired behaviour.
For example:

```kotlin
class Increment(val by: Int) : CounterAction
```

Actions are to be dispatched through the Store's _dispatch_ method to perform State mutations.

For example:

```kotlin
store.dispatch(Increment(2))
```

### Reducer

A Reducer is where the State is mutated or modified, depending on which Action is applied.
It is basically a map of the desired modifications and their effects.

For example:

```kotlin
class CounterReducer: Reducer<CounterState, CounterAction> {
    override fun reduce(currentState: CounterState, action: CounterAction) : CounterState =
        when(action) {
          is Increment -> CounterState(counter: currenState.counter + action.by)
          is Decrement -> CounterState(counter: currenState.counter - action.by)
        }
}
```

### Middleware

Middleware allows for a variety of behaviours that are not directly related to the component's State.
This is useful for the implementation of so called cross-cutting concerns such as Logging, by hooking into the sequence of Action events.

Middleware can run before reducing the state or after depending on the need, this can be achieved by overriding the provided methods.

### Store

The Store "stores" the State, and exposes it for observation as an [Observable](http://reactivex.io/documentation/observable.html). It also connects all the other abstractions together.

To create a Store, simply instantiate it with an initial State and its related Reducer:

```kotlin
val counterStore = Store(initialState, reducer)
```

Several middleware can also be added to the Store through the Store's _addMiddleware_ method.

#### Examples

Examples of usage can be seen in the [tests](https://github.com/mercari/RxReduxK/tree/master/rxredux/src/test/java/com/mercari/rxredux)
