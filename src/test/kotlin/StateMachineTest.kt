package de.botc.server.statemachine

import StateMachine
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StateMachineTest {

    enum class State {
        S1, S2, S3, S4
    }

    enum class Event {
        E1, E2, E3, E4
    }

    enum class SideEffect {
        SE1, SE2
    }

    @Test
    fun `can change state`() {
        val stateMachine = StateMachine<State, Event, Unit>(
            initialState = State.S1,
            {
                transition(State.S1, State.S2, Event.E1)
            },
            endingStates = setOf(State.S2)
        )
        assertEquals(State.S1, stateMachine.currentState)
        stateMachine.callEvent(Event.E1)
        assertEquals(State.S2, stateMachine.currentState)
    }

    @Test
    fun `can't change state if event incorrect`() {
        val stateMachine = StateMachine<State, Event, Unit>(
            initialState = State.S1,
            {
                transition(State.S1, State.S2, Event.E1)
                transition(State.S2, State.S3, Event.E2)
            },
            endingStates = setOf(State.S3)
        )
        stateMachine.callEvent(Event.E2)
        assertEquals(State.S1, stateMachine.currentState)
    }
    @Test
    fun `can't change state on ending state`() {
        val stateMachine = StateMachine<State, Event, Unit>(
            initialState = State.S1,
            {
                transition(State.S1, State.S2, Event.E1)
            },
            endingStates = setOf(State.S1)
        )
        stateMachine.callEvent(Event.E1)
        assertEquals(State.S1, stateMachine.currentState)
    }
    @Test
    fun `can fork and join`() {
        fun getStateMachine() = StateMachine<State, Event, Unit>(
            initialState = State.S1,
            {
                transition(State.S1, State.S2, Event.E1)
                transition(State.S1, State.S3, Event.E2)
                transition(State.S2, State.S4, Event.E3)
                transition(State.S3, State.S4, Event.E4)
            },
            endingStates = setOf(State.S4)
        )
        var stateMachine = getStateMachine()
        stateMachine.callEvent(Event.E1)
        assertEquals(State.S2, stateMachine.currentState)
        stateMachine.callEvent(Event.E3)
        assertEquals(State.S4, stateMachine.currentState)

        stateMachine = getStateMachine()
        stateMachine.callEvent(Event.E2)
        assertEquals(State.S3, stateMachine.currentState)
        stateMachine.callEvent(Event.E4)
        assertEquals(State.S4, stateMachine.currentState)
    }

    @Test
    fun `can have side effect`() {
        var se1 = 0
        var se2 = 0
        val stateMachine = StateMachine(
            initialState = State.S1,
            {
                transition(State.S1, State.S2, Event.E1, SideEffect.SE1)
                transition(State.S2, State.S3, Event.E2, SideEffect.SE1, SideEffect.SE2)
            },
            endingStates = setOf(State.S3)
        )

        stateMachine.doOnSideEffect {
            when(it) {
                SideEffect.SE1 -> se1++
                SideEffect.SE2 -> se2++
            }
        }

        assertEquals(0, se1)
        assertEquals(0, se2)
        stateMachine.callEvent(Event.E1) {
            assertEquals(1, se1)
            assertEquals(0, se2)

            stateMachine.callEvent(Event.E2) {
                assertEquals(2, se1)
                assertEquals(1, se2)
            }
        }
    }
}