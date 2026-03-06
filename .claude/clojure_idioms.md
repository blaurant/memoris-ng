# Clojure Coding Standards & Design Principles
*Based on "Elements of Clojure" by Zachary Tellman*

## 1. Naming Conventions

### The "Sense" of a Name
* **Narrowness:** Choose names that are narrow enough to exclude things they cannot represent, but not so specific that they leak implementation details[cite: 691, 693].
* **Consistency:** A name's "sense" (what it implies) must align with the reader's expectations based on the surrounding code and the problem domain[cite: 708, 709].
* **Avoid General Terms:** Avoid overly general names like `student` if the meaning varies across contexts (e.g., applicant vs. attendee). Use more specific terms like `applicant` or `registrant`[cite: 732, 735].

### Default Parameter Names
When a specific domain name isn't required, use these community-standard defaults:
* `x`: A single value of any type[cite: 805].
* `xs`: A sequence of values[cite: 808].
* `m`: A map of any key/value type[cite: 809].
* `f`: An arbitrary function[cite: 810].
* `ms` / `fs`: Sequences of maps or functions[cite: 811].
* `key->value`: For maps with specific types (e.g., `id->student`)[cite: 821, 822].
* `a+b`: For tuples/vectors of different types (e.g., `tutor+student`)[cite: 824].

### Function Naming
* **Verbs for Effects:** Use verbs (`get-`, `push-`, `delete-`) for functions that cross scope boundaries or perform effects[cite: 158, 159].
* **Nouns for Transformations:** For pure functions that transform data, avoid verbs. Prefer nouns or conversion arrows (e.g., `md5` instead of `calculate-md5`, `->base64` instead of `convert-to-base64`)[cite: 166, 168].
* **Namespaces:** Let the namespace provide context. In a `payload` namespace, `payload/get` is better than `payload/get-payload`[cite: 161, 183].
* **`!` suffix — mutation only:** The `!` suffix is reserved for functions that **mutate state** (atoms, refs, I/O, database writes) — e.g., `swap!`, `reset!`, `save!`. Do NOT use `!` for pure functions that throw exceptions. For guard functions that validate-or-throw, prefer `assert-` prefix (e.g., `assert-lifecycle`, `assert-ownership`).


## 2. Idiomatic Syntax

### Core Idioms
* **Inequalities:** Prefer `<` and `<=` to order terms from least to greatest for better readability[cite: 241].
* **Arity Support:** If a function accumulates values, it should support 0, 1, and 2-arity to behave correctly with `reduce`[cite: 244, 246].
* **Option Maps:** Prefer a single "option map" over multiple named/keyword parameters for flexibility.
* **Narrow Accessors:** Use the most specific accessor for the data structure to signal intent (e.g., use `keys` rather than `map key` to affirm that the input is definitely a map)[cite: 303, 307].
* **Mutual Recursion:** Use `letfn` only for mutual recursion; use `let` and `fn` for all other local function bindings[cite: 320].

### State and Side Effects
* **Atoms first:** Use an `atom` as the default for mutable state. Only move to `ref` (STM) or specialized primitives if you have complex, multi-object consistency requirements[cite: 282, 286].
* **Avoid Agents:** Avoid `agents` due to their unbounded queues[cite: 283].
* **Signal Effects:** Use an explicit `do` block or assign side-effect results to `_` in a `let` binding to warn the reader that an effect is occurring[cite: 293, 298].
* **Java Interop:** Keep Java interop obvious. Avoid macros like `..` that hide the fact that you are calling Java methods[cite: 327].

## 3. Composition and Architecture

### Separation of Phases
* **Pull / Transform / Push:** Distinguish between functions that fetch data (pull), process data (transform), and output data (push)[cite: 460, 465].
* **Operational vs. Functional:** Keep "operational" concerns (error handling, retries, I/O) in the push/pull phases. The "transform" phase should be functional and context-free[cite: 466, 497].

### Indirection and Abstraction
* **Validation at Periphery:** Perform input validation and enforce invariants at the edges of your system (API entries, database boundaries) rather than re-checking at every internal layer[cite: 393, 769].
* **Indirection Layers:** Use names and functions as layers of indirection to separate *what* is being done from *how* it is implemented[cite: 644, 650].
* **Lazy Effects:** Be cautious with lazy sequences that perform side effects, as they conflate the "pull" and "transform" phases and make error handling difficult[cite: 491, 498].
