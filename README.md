# graphy
Java library for structuring code as a graph of nodes.

## Guarantees:
1. Nodes can update asynchronously
2. A parent node is guaranteed to be notified that a child has been updated (although not guaranteed to be notified once for each update).
3. Nodes must provide exception handling and must propagate exceptions gracefully. 
4. There should be a way to clean up a node, allowing it's memory to be GC'ed, removing it's listeners on it's dependents and preventing any future activity by that node.
5. Because nodes are meant to capture semantic state, a node should be able to mutate it's dependencies.

## Other Goals:
1. An emphasis should be placed on compile-time checks and safety. Nodes should be strongly typed, and should be able to be accessed by name.
2. Where compile-time checks can't be enforced (such as for nodes with mutable dependencies), tools should be provided to ensure runtime correctness and provide debugability for failure cases (For example, cycle detection). An emphasis should be placed on discouraging usage that circumvents compile-time checks, only
3. When runtime tools have a high performance/memory cost, they should be toggleable.
4. Nodes should be easily observable, and tools should be provided to allow developers to visualize their graphs and determine bottlenecks.
