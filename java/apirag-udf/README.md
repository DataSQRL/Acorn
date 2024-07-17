# User Defined Functions

This module contains utilities for tools implemented as user defined functions that execute locally. It contains some [built-in function](src/main/java/com/datasqrl/ai/function/builtin) that are generally useful (e.g. for retrieving date and time).

To implement your own user defined function, take a look at the [example](src/test/java/com/datasqrl/ai/function/TestAddFunction.java). All it takes is implementing a class with the function arguments as fields and a single `execute` method. Make sure the fields and functions are annotated with a description, so the LLM knows how and when to invoke the function.