/**
 * Finite state machine implementation in Kotlin.
 */
class StateMachine<STATE, EVENT, SIDE_EFFECT>(
    initialState: STATE,
    build: StateMachineBuilder<STATE, EVENT, SIDE_EFFECT>.() -> Unit,
    private val endingStates: Set<STATE>,
) {

    /**
     * Side effect function that will be executed after each state transition.
     */
    private var onSideEffect: (sideEffect: SIDE_EFFECT) -> Unit = {}

    private var onStateChange: (from: STATE, to: STATE) -> Unit = { _, _ ->  }

    /**
     * Current state of the state machine
     */
    var currentState = initialState

    /**
     * Map of states, all possible transitions, and side effects.
     */
    private var states: MutableMap<STATE, MutableMap<EVENT, Pair<STATE, List<SIDE_EFFECT>>>> = mutableMapOf()

    init {
        build(StateMachineBuilder(this))
    }

    /**
     * Calls an event and the corresponding side effects of the state machine instance.
     * Side effects will be executed in a separate thread.
     * @param event The event that should be called.
     * @param afterSideEffect The callback that will be executed after all side effects.
     */
    fun callEvent(event: EVENT, afterSideEffect: () -> Unit = {}) {
        val path = states[currentState]
        if (path != null) { // Check if the current state has any following states
            val newState = path[event] // Set the new state to the following state based on the current event.
            if (currentState !in endingStates && newState != null) { // Check if the current state is not an ending state and if a new state exists.
                onStateChange(currentState, newState.first)
                currentState = newState.first

                // Start side effects if any exist in a new thread.
                val sideEffects = newState.second
                if (sideEffects.isNotEmpty()) {
                    val thread = Thread {
                        sideEffects.forEach { sideEffect ->
                            onSideEffect(sideEffect)
                        }
                        afterSideEffect()
                    }
                    thread.start()
                }
            }
        }
    }

    /**
     * Sets the side effects of the state machine instance.
     * @param onSideEffect The callback that will be executed with the current side effect as a parameter.
     */
    fun doOnSideEffect(onSideEffect: (sideEffect: SIDE_EFFECT) -> Unit) {
        this.onSideEffect = onSideEffect
    }

    /**
     * Sets behavior on state changes.
     * @param onStateChange This callback will be called on a state change.
     */
    fun doOnStateChange(onStateChange: (from: STATE, to: STATE) -> Unit) {
        this.onStateChange = onStateChange
    }

    /**
     * Builder class for initializing a state machine instance.
     */
    class StateMachineBuilder<STATE, EVENT, SIDE_EFFECT>(val stateMachine: StateMachine<STATE, EVENT, SIDE_EFFECT>) {

        /**
         * Defines a transition for the state machine.
         * @param from The state in which the state machine is.
         * @param to The state in which the state machine will be after the transition.
         * @param on The event that performs the transition.
         * @param sideEffects List of side effects that will be executed when the transition occurs.
         */
        fun transition(from: STATE, to: STATE, on: EVENT, vararg sideEffects: SIDE_EFFECT) {
            if (from in stateMachine.states.keys) {
                stateMachine.states[from]?.set(on, Pair(to, sideEffects.toList()))
            } else {
                stateMachine.states[from] = mutableMapOf(on to Pair(to, sideEffects.toList()))
            }
        }

        /**
         * Defines a transition for the state machine.
         * @param from The state in which the state machine is.
         * @param to The state in which the state machine will be after the transition.
         * @param on The event that performs the transition.
         */
        fun transition(from: STATE, to: STATE, on: EVENT) {
            if (from in stateMachine.states.keys) {
                stateMachine.states[from]?.set(on, Pair(to, emptyList()))
            } else {
                stateMachine.states[from] = mutableMapOf(on to Pair(to, emptyList()))
            }
        }
    }
}