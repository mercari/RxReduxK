package com.mercari.rxredux

import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.gherkin.Feature

object NoOpReducerTest : Spek({

  Feature("NoOpReducer") {

    Scenario("Reducing the state with NoOpReducer") {

      lateinit var reducer: NoOpReducer<NoOpReducerTestState, NoOpReducerTestAction>
      lateinit var state: NoOpReducerTestState
      lateinit var newState: NoOpReducerTestState

      Given("Initialize NoOpReducer") {
        reducer = NoOpReducer()
        state = NoOpReducerTestState()
      }

      When("Reduce the action") {
        newState = reducer.reduce(state, NoOpReducerTestAction)
      }

      Then("No state changes will happen") {
        newState shouldEqual state
      }
    }
  }
})

data class NoOpReducerTestState(val meaningless: String = "meaningless") : State

object NoOpReducerTestAction : Action
