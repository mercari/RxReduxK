package com.mercari.rxredux

import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.gherkin.Feature

object NoOpReducerTest : Spek({

  Feature("NoOpReducer") {

    Scenario("Reducing the state with NoOpReducer") {

      lateinit var reducer: NoOpReducer<TestState, TestAction>
      lateinit var state: TestState
      lateinit var newState: TestState

      Given("Initialize NoOpReducer") {
        reducer = NoOpReducer()
        state = TestState()
      }

      When("Reduce the action") {
        newState = reducer.reduce(state, TestAction)
      }

      Then("No state changes will happen") {
        newState shouldEqual state
      }
    }
  }
})

data class TestState(val meaningless: String = "meaningless") : State

object TestAction : Action