package com.mercari.rxredux

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.gherkin.Feature

object CompositeReducerTest : Spek({

  Feature("CompositeReducer") {

    val compositeReducer: CompositeReducer<TestState, TestAction> = CompositeReducer(listOf(FooReducer(), BarReducer()))

    Scenario("Reduce concat text action") {

      lateinit var initialState: TestState
      lateinit var nextState: TestState

      Given("An initial state") {
        initialState = TestState("A", 10, false)
      }

      When("Dispatch ConcatTextAction") {
        nextState = compositeReducer.reduce(initialState, ConcatTextAction)
      }

      Then("Next state is different from initial state") {
        nextState shouldNotBeEqualTo initialState
      }

      And("Next state text should have 2 'A's") {
        nextState.text shouldBeEqualTo "AA"
      }
    }

    Scenario("Reduce subtract number action") {

      lateinit var initialState: TestState
      lateinit var nextState: TestState

      Given("An initial state") {
        initialState = TestState("A", 10, false)
      }

      When("Dispatch SubtractNumberAction") {
        nextState = compositeReducer.reduce(initialState, SubtractNumberAction)
      }

      Then("Next state is different from initial state") {
        nextState shouldNotBeEqualTo initialState
      }

      And("Next state number should decrease") {
        nextState.number shouldBeEqualTo 9
      }
    }

    Scenario("Reduce toggle flag action") {

      lateinit var initialState: TestState
      lateinit var nextState: TestState

      Given("An initial state") {
        initialState = TestState("A", 10, false)
      }

      When("Dispatch ToggleFlagAction") {
        nextState = compositeReducer.reduce(initialState, ToggleFlagAction)
      }

      Then("Next state is the same as initial state") {
        nextState shouldBeEqualTo initialState
      }
    }
  }
})

private data class TestState(
  val text: String,
  val number: Int,
  val flag: Boolean
) : State

sealed class TestAction : Action

object ConcatTextAction : TestAction()

object SubtractNumberAction : TestAction()

object ToggleFlagAction : TestAction()

private class FooReducer : Reducer<TestState, TestAction> {
  override fun reduce(currentState: TestState, action: TestAction): TestState = when (action) {
    is ConcatTextAction -> {
      currentState.copy(
        text = currentState.text.plus("A")
      )
    }
    is ToggleFlagAction -> {
      currentState.copy(
        flag = currentState.flag.not()
      )
    }
    else -> currentState
  }
}

private class BarReducer : Reducer<TestState, TestAction> {
  override fun reduce(currentState: TestState, action: TestAction): TestState = when (action) {
    is SubtractNumberAction -> {
      currentState.copy(
        number = currentState.number - 1
      )
    }
    is ToggleFlagAction -> {
      currentState.copy(
        flag = currentState.flag.not()
      )
    }
    else -> currentState
  }
}
